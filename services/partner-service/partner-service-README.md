# Partner Service

## Назначение

`partner-service` управляет партнёрами (`Partner`) и принадлежащими им магазинами (`Store`), а также хранит данные,
необходимые для проверки управленческого доступа к магазину.

Сервис предоставляет management API для чтения, создания и обновления Partner и Store, внутренний endpoint для проверки
связи Partner/Store и назначения пользователя, а также публикует изменения Partner и Store в Kafka через Transactional
Outbox.

## Технологии

- Kotlin 2.3.21, Java 21 и Gradle Kotlin DSL.
- Spring Boot 4.1.0: Spring MVC, Validation, Data JPA, Security, OAuth2 Resource Server, Actuator и Spring Kafka.
- PostgreSQL, Hibernate ORM 7.4.5 и Liquibase.
- Apache Kafka и Spring Kafka для публикации событий Partner и Store.
- Jackson для JSON-сериализации.
- JUnit 5, Spring Boot Test и Testcontainers PostgreSQL для тестирования.
- Spotless и ktfmt для форматирования кода; Zipkin и Micrometer Tracing для распределённой трассировки.

## Доменная модель

### Основные сущности

#### `Partner`

Партнёр хранит идентификатор, `managerId`, название, статус, даты создания и обновления и `version` для optimistic locking.

Статусы Partner:

- `ACTIVE` — партнёр активен;
- `SUSPENDED` — партнёр приостановлен.

При обновлении можно изменить название и статус. `managerId` существующего Partner изменить нельзя.

#### `Store`

Магазин принадлежит одному Partner и хранит название, статус, адрес, расписание работы, даты создания и обновления и
`version` для optimistic locking.

Статусы Store:

- `ACTIVE` — магазин активен;
- `SUSPENDED` — магазин приостановлен.

При обновлении нельзя изменить `partnerId`.

### Value objects и связанные модели

- `PartnerId`, `StoreId` — типизированные UUID-идентификаторы.
- `Address` — город, улица, здание и необязательный почтовый индекс.
- `WorkingHours` — день недели, время открытия и время закрытия.
- `PartnerStoreAccessSnapshot` — результат внутренней проверки Partner/Store: статусы сущностей и признаки того, является
  ли пользователь менеджером Partner или сотрудником Store.

## Основные бизнес-сценарии

- Получение Partner с проверкой прав текущего пользователя.
- Создание Partner после проверки прав.
- Обновление Partner с проверкой прав, запретом смены `managerId` и optimistic locking.
- Получение Store с проверкой, что Store действительно принадлежит Partner из URL, и последующей проверкой прав.
- Создание Store только для существующего Partner и с проверкой прав.
- Обновление Store с запретом смены Partner, проверкой optimistic locking и прав пользователя.
- Проверка связи Partner/Store и назначения пользователя через внутренний `PartnerStoreAccess` endpoint.
- Транзакционная запись событий Partner и Store в Outbox после успешного изменения данных.
- Асинхронная публикация Outbox-событий в Kafka topic `partner.events.v1`.

## REST API

Все endpoint, кроме health check, требуют JWT.

### `PartnerController`

- `GET /api/v1/partners/{partnerId}` — `getPartner`: возвращает Partner после проверки доступа.
- `PUT /api/v1/partners/{partnerId}` — `createOrUpdatePartner`: создаёт или обновляет Partner.

Для чтения Partner доступ разрешён:

- `ADMIN`;
- `MANAGER`, если пользователь является менеджером этого Partner;
- `STAFF`, если пользователь назначен хотя бы на один Store этого Partner.

Создавать и изменять Partner могут `ADMIN` или соответствующий `MANAGER`.

### `StoreController`

- `GET /api/v1/partners/{partnerId}/stores/{storeId}` — `getStore`: возвращает Store, принадлежащий указанному Partner.
- `PUT /api/v1/partners/{partnerId}/stores/{storeId}` — `createOrUpdateStore`: создаёт или обновляет Store.

На уровне Spring Security чтение Store доступно ролям `STAFF`, `MANAGER` и `ADMIN`, а изменение — `MANAGER` и `ADMIN`.
Дополнительно `StoreAccessPolicy` проверяет фактическое назначение пользователя:

- `ADMIN` имеет доступ без назначения;
- `MANAGER` должен быть менеджером Partner, которому принадлежит Store;
- `STAFF` при чтении должен быть назначен на конкретный Store.

### `PartnerStoreAccessController`

- `POST /internal/api/v1/partner-store-access/check` — проверяет Partner, Store и назначение пользователя для
  межсервисных management-сценариев.

Если в запросе передан `partnerId`, use case дополнительно проверяет, что Store принадлежит этому Partner. Если `partnerId`
не передан, Partner определяется через `store.partnerId`.

Ответ содержит:

- `partnerStatus`;
- `storeStatus`;
- `userIsStoreManager`;
- `userIsStoreStaff`.

### Служебный endpoint

- `GET /actuator/health` — возвращает состояние сервиса и доступен без JWT.

## Kafka events

`partner-service` является producer событий Partner и Store и публикует их в:

```text
partner.events.v1
```

Поддерживаемые события:

- `store.created` — Store создан;
- `store.updated` — Store обновлён;
- `store.suspended` — итоговый статус Store равен `SUSPENDED`;
- `partner.updated` — Partner обновлён;
- `partner.suspended` — итоговый статус Partner равен `SUSPENDED`.

Создание Partner не публикует отдельное событие.

Все события используют schema version `1`. `aggregateId` соответствует ID изменённой сущности, а `aggregateVersion` —
её сохранённой optimistic-locking версии.

### Store event payload

Store event содержит:

- `storeId`;
- `partnerId`;
- `partnerStatus`;
- `storeStatus`;
- `name`;
- `address`.

`partnerStatus` берётся из актуального Partner. Адрес сериализуется в строку из города, улицы, здания и, если он указан,
почтового индекса.

### Partner event payload

Partner event содержит:

- `partnerId`;
- `partnerStatus`.

## Transactional Outbox

Kafka не вызывается напрямую из application use case.

После успешного изменения сущности use case создаёт `ApplicationEvent` через `ApplicationEventFactory` и передаёт его в
`DomainEventPublisherPort`.

`OutboxDomainEventPublisherAdapterService` требует уже существующую транзакцию (`Propagation.MANDATORY`), сериализует весь
event envelope в JSON и сохраняет его в `outbox_events` со статусом `NEW`. Поэтому изменение Partner/Store и запись события
коммитятся одной business transaction.

`OutboxRelay` периодически вызывает `OutboxBatchProcessor`. По умолчанию:

- batch size — `100`;
- максимальное количество попыток — `10`;
- задержка между запусками — `1s`;
- initial delay — `10s`.

`OutboxBatchProcessor` выбирает publishable события со статусом `NEW` или `FAILED`, отправляет их в
`partner.events.v1` с Kafka key, равным `aggregateId`, и ждёт результат публикации.

После успешной отправки событие переводится в `PUBLISHED`. При ошибке оно переводится в `FAILED`, количество попыток
увеличивается, и событие может быть выбрано следующим запуском relay, пока не достигнут `maxAttempts`.

Kafka producer использует String key/value serializers, `acks=all` и idempotent producer.

## Интеграция с Offer Service

`offer-service` использует `partner-service` двумя способами:

1. Синхронно вызывает `POST /internal/api/v1/partner-store-access/check` для проверки management-доступа.
2. Асинхронно слушает `partner.events.v1` и поддерживает локальные Store snapshots для публичного чтения и других
   сценариев, которым не нужен синхронный вызов Partner Service.

При создании Store публикуется `store.created`, поэтому после успешной обработки события Offer Service может создать
локальный snapshot нового магазина. Последующие Store и Partner events обновляют соответствующие данные snapshot.
