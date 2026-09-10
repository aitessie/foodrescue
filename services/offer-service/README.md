# Offer Service

## Назначение

`offer-service` управляет наборами еды (`FoodBag`), создаваемыми на их основе предложениями (`Offer`) и резервами
покупателей (`OfferReservation`).

Сервис предоставляет management API для сотрудников магазинов, каталог доступных предложений для покупателей и
автоматически закрывает предложения после окончания окна выдачи.

Данные о партнёрах и магазинах не принадлежат сервису. Для проверки управленческого доступа он обращается в
`partner-service`, а для публичной видимости и резервирования использует локальные снимки состояния магазинов.

## Технологии

- Kotlin 2.3.21, Java 21 и Gradle Kotlin DSL.
- Spring Boot 4.1.0: Spring MVC, Validation, Data JPA, Security, OAuth2 Resource Server и OAuth2 Client, RestClient,
  Actuator.
- PostgreSQL, Hibernate ORM 7.4.1, Hibernate Spatial и Liquibase.
- Apache Kafka и Spring Kafka для обмена событиями.
- Jackson 3.1.4 для JSON-сериализации.
- JUnit 5, Mockito, Testcontainers и MockWebServer для тестирования.
- Spotless и ktfmt для форматирования кода; Zipkin для распределённой трассировки.

## Доменная модель

### Основные сущности

#### `FoodBag`

Шаблон набора еды конкретного магазина. Содержит название, описание, категорию, исходную цену, цену продажи и аллергены.
Цена продажи должна быть положительной и меньше исходной. Состояние изменяется отдельно от остальных данных и принимает
значения `ACTIVE` или `INACTIVE`. Поле `version` используется для optimistic locking.

#### `Offer`

Конкретное предложение магазина, созданное на основе `FoodBag`. Хранит снимок его категории, цены и аллергенов, общее и
доступное количество наборов, окно выдачи и статус. Уже зарезервированное количество вычисляется как разница между общим
и доступным.

При обновлении нельзя заменить магазин, `FoodBag` или сохранённые данные набора. Уменьшить общее количество ниже уже
зарезервированного нельзя. Новое предложение создаётся в статусе `SCHEDULED`.

Реализованные переходы статусов:

- `SCHEDULED → ACTIVE`, если `FoodBag` активен, окно выдачи не закончилось и доступное количество больше нуля;
- `SCHEDULED → CANCELLED` и `ACTIVE → CANCELLED`, если активных резервов нет;
- незавершённое просроченное предложение → `CLOSED` автоматически.

Статусы `DRAFT` и `SOLD_OUT` объявлены в модели, но в текущих application-сценариях переходы в них не реализованы.

#### `OfferReservation`

Резерв определённого количества наборов из одного `Offer` за конкретным покупателем. Создаётся в статусе `RESERVED` и
может быть переведён в `RELEASED`. Освобождение (отмена) повторного резерва идемпотентно и возвращает количество обратно
в доступный остаток предложения.

### Value objects и модели чтения

- `FoodBagId`, `OfferId`, `ReservationId`, `PartnerId`, `StoreId` — типизированные идентификаторы.
- `PickupWindow` — начало и конец выдачи; конец должен быть строго позже начала.
- `StoreSnapshot` — локальный снимок Partner, Store, их статусов, адреса, часового пояса и версий.
- `StoreSnapshotUpdate` и `PartnerStatusSnapshotUpdate` — данные для обновления локальных снимков.
- `PartnerStoreAccessSnapshot` — результат синхронной проверки статусов и принадлежности пользователя к магазину.
- `OfferSearchFilter` — фильтры поиска по магазину и категории, а также номер и размер страницы.
- `OfferSearchItem` — объединённая публичная модель предложения, набора и магазина.
- `OfferSearchPage` — страница результатов с общим количеством элементов и страниц.

## Основные бизнес-сценарии

- Создание и обновление `FoodBag` с проверкой магазина, прав пользователя и версии сущности.
- Получение `FoodBag` и отдельное изменение его статуса.
- Создание `Offer` на основе активного `FoodBag`; категория, цена и аллергены копируются в предложение как неизменяемый
  снимок.
- Обновление общего количества и окна выдачи без потери уже созданных резервов.
- Активация и отмена предложения с проверкой допустимости перехода статуса.
- Получение одного видимого предложения и постраничный поиск с фильтрацией по `storeId` и `category`.
- Идемпотентное резервирование по переданному `reservationId` с уменьшением `availableQuantity`.
- Получение собственного резерва и его освобождение (отмена) с восстановлением доступного количества.
- Периодическое закрытие просроченных предложений через `CloseExpiredOffersJob`; предложения обрабатываются отдельными
  транзакциями, и ошибка одного предложения не останавливает batch.
- Публикация доменных событий после успешного изменения `FoodBag`, `Offer` или резерва.

## REST API

Все endpoint, кроме health check, требуют JWT. Management-операции доступны `ADMIN`, назначенному `MANAGER` партнёра или
назначенному `STAFF` магазина. Операции с резервами доступны `CUSTOMER` и `ADMIN`; покупатель может читать и
освобождать (отменять)
только собственные резервы.

### `FoodBagController`

- `GET /api/v1/partners/{partnerId}/stores/{storeId}/food-bags/{foodBagId}` — `getFoodBag`: возвращает набор еды.
- `PUT /api/v1/partners/{partnerId}/stores/{storeId}/food-bags/{foodBagId}` — `createOrUpdateFoodBag`: создаёт или
  обновляет набор.
- `GET /api/v1/partners/{partnerId}/stores/{storeId}/food-bags/{foodBagId}/status` — `getFoodBagStatus`: возвращает
  статус и версию набора.
- `PUT /api/v1/partners/{partnerId}/stores/{storeId}/food-bags/{foodBagId}/status` — `changeFoodBagStatus`: изменяет
  статус набора.

### `OfferController`

- `GET /api/v1/partners/{partnerId}/stores/{storeId}/offers/{offerId}` — `getManagedOffer`: возвращает предложение для
  управления.
- `PUT /api/v1/partners/{partnerId}/stores/{storeId}/offers/{offerId}` — `createOrUpdateOffer`: создаёт или обновляет
  предложение.
- `GET /api/v1/partners/{partnerId}/stores/{storeId}/offers/{offerId}/status` — `getManagedOfferStatus`: возвращает
  статус и версию предложения.
- `PUT /api/v1/partners/{partnerId}/stores/{storeId}/offers/{offerId}/status` — `changeOfferStatus`: активирует или
  отменяет предложение.
- `PUT /api/v1/partners/{partnerId}/stores/{storeId}/offers/{offerId}/quantity` — `changeOfferQuantity`: изменяет общее
  количество наборов с учётом уже созданных резервов.

### `PublicOfferController`

- `GET /api/v1/offers/{offerId}` — `getPublicOffer`: возвращает видимое покупателю предложение вместе с данными набора и
  магазина.
- `GET /api/v1/offers` — `searchOffers`: возвращает страницу видимых предложений; поддерживает `storeId`, `category`,
  `page` и `size` как query parameters.

### `OfferReservationController`

- `PUT /api/v1/offers/{offerId}/reservations/{reservationId}` — `reserveFoodBags`: создаёт идемпотентный резерв и
  уменьшает доступное количество наборов.
- `GET /api/v1/reservations/{reservationId}` — `getOfferReservation`: возвращает резерв владельцу или администратору.
- `DELETE /api/v1/reservations/{reservationId}` — `releaseFoodBagReservation`: переводит резерв в `RELEASED`; для
  пользователя это означает отмену резерва.

### Служебный endpoint

- `GET /actuator/health` — возвращает состояние сервиса и доступен без JWT.

## Интеграция с Partner Service

Перед management-операцией `FoodBagAccessPolicy` или `OfferAccessPolicy` вызывает через `PartnerStoreAccessPort`:

```http
POST /internal/api/v1/partner-store-access/check
```

В запрос передаются `partnerId`, `storeId` и `userId`. Ответ содержит статусы Partner и Store, а также признаки
`userIsStoreManager` и `userIsStoreStaff`. Операция разрешается только для активных Partner и Store и пользователя с
подходящей ролью и назначением; `ADMIN` не требует назначения.

Для HTTP-вызова используется `RestClient` с настраиваемыми base URL и timeout. `offer-service` получает отдельный
service token по OAuth2 Client Credentials и не пересылает пользовательский JWT.

Ответы Partner Service преобразуются следующим образом:

- `400` — нарушение контракта запроса;
- `401/403` — ошибка межсервисной аутентификации;
- `404` — Partner, Store или их связь не найдены;
- `5xx` или недоступность соединения — Partner Service недоступен.

Для публичного чтения и резервирования используется локальный `StoreSnapshot`. Неактивный Partner или Store скрывает
предложение от покупателя и запрещает создание резерва.
