# Order Service

## Назначение

`order-service` управляет заказами покупателей (`Order`), одноразовыми кодами получения (`PickupToken`) и жизненным циклом
заказа от создания до получения, отмены или `NO_SHOW`.

Сервис оркестрирует резервирование наборов в `offer-service`, локальную обработку платёжных операций и подтверждение выдачи
с проверкой доступа сотрудника к магазину через `partner-service`.

Данные Offer и Store не принадлежат сервису. При создании заказа `order-service` получает снимок Offer через REST, а
изменение резерва выполняет асинхронно через Kafka. Для проверки STAFF/MANAGER при выдаче и отмене используется внутренний
Partner Store Access API.

## Технологии

- Kotlin 2.3.21, Java 21 и Gradle Kotlin DSL.
- Spring Boot 4.1.0: Spring MVC, Validation, Data JPA, Security, OAuth2 Resource Server и OAuth2 Client, RestClient,
  Actuator и Scheduling.
- PostgreSQL и Liquibase; Hibernate работает с `ddl-auto=validate`.
- Apache Kafka и Spring Kafka для команд резервирования и обработки событий Offer Service.
- Jackson 3.1.4 для JSON-сериализации.
- Spotless и ktfmt для форматирования кода.

## Доменная модель

### Основные сущности

#### `Order`

Заказ одного Offer конкретным покупателем. Хранит `customerId`, `offerId`, `storeId`, количество, снимок цены и окна
получения. Денежные значения представлены в minor units через `Long`: `unitPrice` и `totalAmount`.

Доменная сущность хранит состояние, а правила переходов находятся в application use cases. Поле `version` используется для
optimistic locking.

Статусы заказа:

- `PENDING` — заказ создан, резервирование и/или авторизация ещё не завершены;
- `RESERVED` — резерв Offer подтверждён и платёж успешно авторизован;
- `PICKED_UP` — выдача подтверждена, capture ещё может обрабатываться;
- `COMPLETED` — выдача подтверждена и capture успешно завершён;
- `FAILED` — резервирование или авторизация завершились неуспешно;
- `CANCELLED` — заказ отменён;
- `NO_SHOW` — покупатель не пришёл до конца pickup window.

Основной успешный путь:

```text
PENDING → RESERVED → PICKED_UP → COMPLETED
```

Дополнительные переходы выполняются application-логикой:

- `PENDING → FAILED` при отказе резервирования или платёжной авторизации;
- `PENDING → CANCELLED` и `RESERVED → CANCELLED` при допустимой отмене;
- `RESERVED → NO_SHOW` после окончания pickup window.

#### `PickupToken`

Одноразовый код для подтверждения выдачи заказа. В БД хранится только SHA-256 hash токена, а raw token возвращается
покупателю только при его выпуске и не логируется.

Токен доступен только владельцу `RESERVED` Order. Повторный запрос до использования выпускает новый raw token и заменяет
сохранённый hash, поэтому предыдущий код становится недействительным. После успешной выдачи заполняется `usedAt`.

### Value objects и модели интеграции

- `OrderId`, `OfferId`, `StoreId` — типизированные идентификаторы.
- `OfferSnapshot` — снимок Offer, получаемый из `offer-service`: Store, статус, цена, доступное количество и pickup window.
- `PartnerStoreAccessSnapshot` — статусы Partner и Store и признаки назначения пользователя как MANAGER или STAFF.
- `OrderPage` — страница истории заказов текущего покупателя.
- `PaymentResult` — результат локальной или будущей внешней платёжной операции.

## Основные бизнес-сценарии

- Создание Order через клиентский `orderId`; повторный одинаковый PUT идемпотентен и не создаёт второй reservation command.
- Получение одного Order владельцем или `ADMIN`.
- Получение постраничной истории только текущего `CUSTOMER`.
- Запрос snapshot Offer через REST перед созданием Order.
- Асинхронное резервирование количества в Offer Service через `order.commands.v1`.
- Идемпотентная обработка результатов резервирования через Inbox.
- Локальная авторизация платежа после успешного hold и commit резерва после успешной авторизации.
- Выпуск одноразового pickup token с хранением только hash.
- Подтверждение выдачи STAFF/MANAGER/ADMIN с проверкой конкретного Store через Partner Service.
- Capture после подтверждения выдачи; при текущем локальном `PaymentAdapter` успешный результат обрабатывается синхронно.
- Отмена CUSTOMER, STAFF/MANAGER или ADMIN с actor-specific правилами, освобождением резерва и `VOID`, когда это требуется.
- Автоматический перевод просроченных `RESERVED` заказов в `NO_SHOW` с конфигурируемым платёжным действием.
- Публикация исходящих Kafka-команд через transactional Outbox и дедупликация входящих Offer events через Inbox.

## REST API

Все endpoint, кроме health check, требуют JWT.

### `OrderController`

- `GET /api/v1/orders` — `CUSTOMER`: возвращает страницу собственных заказов; параметры `page` и `size` ограничены
  приложением.
- `GET /api/v1/orders/{orderId}` — `CUSTOMER` или `ADMIN`: возвращает Order; CUSTOMER может читать только свой заказ.
- `PUT /api/v1/orders/{orderId}` — `CUSTOMER`: создаёт Order и возвращает `202 Accepted`. `orderId` генерируется клиентом
  один раз и используется как natural idempotency key при повторе запроса.
- `POST /api/v1/orders/{orderId}/cancel` — `CUSTOMER`, `STAFF`, `MANAGER` или `ADMIN`: отменяет Order по правилам роли и
  текущего состояния.
- `GET /api/v1/orders/{orderId}/pickup-token` — `CUSTOMER`: выпускает одноразовый pickup token для собственного
  `RESERVED` Order.

При повторном `PUT` с тем же `orderId`, тем же CUSTOMER, Offer и quantity возвращается существующий Order без новых side
effects. Тот же `orderId` с другим Offer или quantity даёт conflict; для другого CUSTOMER существующий Order скрывается как
not found.

### `PickupController`

- `POST /api/v1/pickups/confirm` — `STAFF`, `MANAGER` или `ADMIN`: подтверждает выдачу по `storeId` и pickup token.

При подтверждении проверяются hash токена, соответствие Store, доступ сотрудника через Partner Service, состояние Order и
pickup window. Токен отмечается использованным, Order переводится в `PICKED_UP`, после чего инициируется capture. При
текущем локальном `PaymentAdapter` успешный capture сразу переводит Order в `COMPLETED`.

Повторное подтверждение уже использованного токена идемпотентно для Order в `PICKED_UP` или `COMPLETED` и не инициирует
второй capture.

### Служебный endpoint

- `GET /actuator/health` — возвращает состояние сервиса и доступен без JWT.

## Создание заказа и интеграция с Offer Service

Перед созданием нового Order сервис синхронно получает актуальный Offer snapshot:

```http
GET /api/v1/offers/{offerId}
```

Вызов выполняется через `OfferQueryPort`/`OfferHttpAdapter` с OAuth2 Client Credentials от имени `order-service`.
Проверяются статус `ACTIVE`, доступное количество и то, что pickup window ещё не завершился. После этого Order сохраняется
в `PENDING`, а в той же локальной транзакции в Outbox записывается команда резервирования.

Order Service публикует в `order.commands.v1` три типа команд:

- `order.reservation-requested` — создать hold для нового Order;
- `order.reservation-commit-requested` — подтвердить резерв после успешной авторизации платежа;
- `order.reservation-release-requested` — освободить резерв при компенсации или отмене.

`reservationId` в Offer Service совпадает с `orderId`, что даёт естественную идемпотентность reservation flow.

Order Service потребляет `offer.events.v1` и обрабатывает:

- `offer.reserved` — резерв успешно создан; запускается payment authorization;
- `offer.reservation-rejected` — Order переводится из `PENDING` в `FAILED`.

Входящие события дедуплицируются по `eventId` через Inbox. Если Order успел перейти в `CANCELLED`, а позднее приходит
`offer.reserved`, Order не возвращается в активное состояние: вместо этого публикуется release command.

## Платежи

Отдельного Payment Service сейчас нет. Application layer зависит от `PaymentCommandPort`:

```text
requestAuthorization(...)
requestCapture(...)
requestVoid(...)
requestRefund(...)
```

Текущий `PaymentAdapter` — временная локальная реализация. Он имитирует успешный результат и сразу вызывает
`ProcessPaymentResultUseCase`, поэтому текущий flow остаётся синхронным внутри процесса, но application contract не требует
синхронного ответа от будущей внешней реализации.

Реально используемые операции:

- `AUTHORIZATION`: после `offer.reserved`; success переводит `PENDING → RESERVED` и создаёт commit command;
- `CAPTURE`: после подтверждения выдачи; success переводит `PICKED_UP → COMPLETED`; для `NO_SHOW` статус остаётся
  `NO_SHOW`;
- `VOID`: используется при отмене `RESERVED` Order и может использоваться для `NO_SHOW` согласно конфигурации.

`REFUND` присутствует в `PaymentCommandPort` как будущая точка расширения, но в текущих Order-сценариях не инициируется и
его результат application layer пока не обрабатывает.

## Интеграция с Partner Service

Для подтверждения выдачи и store cancellation сервис проверяет доступ пользователя к Store через
`PartnerStoreAccessPort`:

```http
POST /internal/api/v1/partner-store-access/check
```

Order Service передаёт `storeId` и `userId`. Partner Service сам определяет Partner по Store и возвращает статусы Partner и
Store, а также `userIsStoreManager` и `userIsStoreStaff`.

Операция разрешается только при активных Partner и Store. `MANAGER` должен быть manager соответствующего Store через его
Partner, `STAFF` должен быть назначен на Store, а `ADMIN` не требует назначения.

HTTP-вызов выполняется через `RestClient` с отдельными base URL и timeout. `order-service` получает service token по OAuth2
Client Credentials и не пересылает пользовательский JWT.

## Cancellation

Отменять можно Order в `PENDING` или `RESERVED`. Повторная отмена уже `CANCELLED` Order идемпотентна.

Для `CUSTOMER` дополнительно проверяются ownership и deadline. По умолчанию отмена разрешена только раньше чем за два часа
до `pickupStart`; интервал задаётся через `food-rescue.order-cancellation.customer-deadline-before-pickup-start`.

`STAFF` и `MANAGER` могут отменять Order только для Store, к которому Partner Service подтверждает доступ. `ADMIN` не
требует Store membership и не ограничен customer deadline.

При отмене `RESERVED` Order публикуется `order.reservation-release-requested` и вызывается `requestVoid`. Для `PENDING`
Order немедленный release не публикуется, потому что hold ещё может не существовать. Если позднее приходит `offer.reserved`,
обработчик увидит `CANCELLED` и создаст компенсационный release command.

## Pickup token и подтверждение выдачи

Raw pickup token генерируется криптографически стойким генератором и в базе не хранится. Для поиска используется SHA-256
hash с unique constraint.

Подтверждение допускается только в пределах сохранённого `pickupStart..pickupEnd`. После успешной проверки токен получает
`usedAt`, а Order сначала становится `PICKED_UP`. Затем запускается capture; локальный успешный payment result переводит
его в `COMPLETED`.

## NO_SHOW scheduler

`NoShowOrdersJob` периодически ищет `RESERVED` Order, у которых `pickupEnd <= now`, и передаёт их application use case.
Сам scheduler бизнес-логику не содержит.

Обработка выполняется batch-ами, а каждый Order повторно проверяется и изменяется в отдельной `REQUIRES_NEW` транзакции.
Повторный запуск безопасен: Order, уже перешедший из `RESERVED`, больше не обрабатывается.

Основные параметры:

- `food-rescue.scheduler.no-show-cron` — по умолчанию раз в минуту;
- `food-rescue.scheduler.no-show-zone` — по умолчанию `UTC`;
- `food-rescue.scheduler.no-show-batch-size` — размер batch, по умолчанию `100`;
- `food-rescue.scheduler.no-show-payment-action` — `CAPTURE` или `VOID`, по умолчанию `CAPTURE`.

При `NO_SHOW` резерв Offer не освобождается и `availableQuantity` не увеличивается: pickup window уже завершён, поэтому
единица не возвращается в продаваемый остаток этого Offer.

## Outbox и Inbox

Исходящие reservation commands сначала сохраняются в `outbox_events` в одной транзакции с изменением Order. `OutboxRelay`
периодически передаёт batch в `OutboxBatchProcessor`, который публикует сообщения в Kafka и отмечает успешную публикацию.

Входящие события Offer Service обрабатываются с at-least-once семантикой. `inbox_events` фиксирует `eventId` до изменения
Order в той же транзакции, поэтому повторная доставка того же события не повторяет бизнес-операцию.

Для consumer настроены ограниченные retry и публикация необработанного сообщения в DLT. Payload исходящих событий и
чувствительные значения вроде pickup token, JWT или Authorization header в логах не выводятся.
