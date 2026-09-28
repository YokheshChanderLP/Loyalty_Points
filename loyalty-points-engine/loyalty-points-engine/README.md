# LoyaltyPoints - Tier-Based Retail Loyalty Points Engine

Spring Boot 3 (Java 17) + Spring Data JPA + MySQL + Maven, with an HTML/CSS/JavaScript frontend.

## Run
1. Install JDK 17+, Maven 3.9+ and MySQL 8.
2. Edit `src/main/resources/application.properties` -> set `spring.datasource.username` / `password`.
   (The database `loyalty_db` and all tables are created automatically.)
3. `mvn spring-boot:run`  (or run `LoyaltyPointsApplication` from IntelliJ)
4. Open http://localhost:8080

Default tiers (seeded on first start): Silver 0 pts / 2%, Gold 1000 pts / 5%, Platinum 5000 pts / 10%.

## Configurable rules (application.properties)
- `app.loyalty.points-per-rupee=0.1`  -> points earned per rupee paid
- `app.loyalty.redemption-value-per-point=1` -> rupee value of one point

## Business rules (enforced in the service layer, before saving)
- Tier discount is applied automatically from the customer's current tier.
- Points are earned on the amount actually paid (after discount and redemption).
- Redeemed points can never exceed the available balance; redemption value can never exceed the payable amount.
- Tier upgrades happen automatically when lifetime points cross a threshold; tiers never downgrade.

## REST API
| Method | Endpoint | Purpose |
|---|---|---|
| GET/POST | `/api/customers` | list / create customer |
| GET/PUT/DELETE | `/api/customers/{id}` | view (tier + balance) / update / delete |
| GET | `/api/customers/{id}/points-history` | points ledger |
| GET/POST | `/api/tiers` | list / create tier |
| GET/PUT/DELETE | `/api/tiers/{id}` | view / update / delete tier |
| GET/POST | `/api/purchases` | list (`?customerId=`) / record purchase |
| POST | `/api/purchases/preview` | calculate without saving |
| GET/PUT/DELETE | `/api/purchases/{id}` | view / edit description / cancel (reverses points) |
| GET | `/api/settings` | current earn/redeem rates |

Error responses are JSON: `{timestamp, status, error, message, details}`.
