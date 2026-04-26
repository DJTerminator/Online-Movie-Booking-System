<img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/> <img src="https://img.shields.io/badge/Spring_Boot-4.0.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/> <img src="https://img.shields.io/badge/Spring_Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"/> <img src="https://img.shields.io/badge/H2-In--Memory-003545?style=for-the-badge&logo=h2&logoColor=white"/> <img src="https://img.shields.io/badge/PostgreSQL-Ready-4169E1?style=for-the-badge&logo=postgresql&logoColor=white"/> <img src="https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white"/>

---

<h1 align="center">🎬 BookMyShow — Online Movie Booking System</h1>
<p align="center">
  A production-grade, full-stack REST API for movie ticket booking built with Spring Boot, Spring Security (JWT), JPA/Hibernate, and H2/PostgreSQL.
</p>
<p align="center">
  <a href="#-high-level-design-hld">HLD</a> · <a href="#-low-level-design-lld">LLD</a> · <a href="#-api-reference">API Reference</a> · <a href="#-booking-flow">Booking Flow</a> · <a href="#-quick-start">Quick Start</a>
</p>

---
## 📋 Table of Contents

- [Overview](#-overview)
- [Technology Stack](#-technology-stack)
- [Architecture & Design Patterns](#-architecture--design-patterns)
- [SOLID Principles](#-solid-principles)
- [High-Level Design (HLD)](#-high-level-design-hld)
- [Low-Level Design (LLD)](#-low-level-design-lld)
- [Database Design](#-database-design)
- [Concurrency & Locking Strategy](#-concurrency--locking-strategy)
- [API Reference](#-api-reference)
- [Booking Flow](#-booking-flow)
- [Discount Engine](#-discount-engine)
- [Exception Handling](#-exception-handling)
- [API Documentation](#-api-documentation)
- [Quick Start](#-quick-start)
- [Assumptions & Improvements](#-assumptions--future-improvements)

---

## 🔭 Overview

BookMyShow is a backend REST API that replicates the core functionality of an online movie ticket booking platform. It supports:

- **Browse Scenario** — Filter movies by city, date; view show timings and seat availability
- **Booking Scenario** — Multi-seat selection with real-time concurrency protection, discount calculation, and payment processing
- **Auth** — JWT-based registration, login, and role-based access (USER / ADMIN)
- **Concurrency** — Double-booking prevention via dual-layer pessimistic + optimistic locking
- **Payments** — Strategy pattern supporting CARD, UPI, NETBANKING, WALLET with refund support

---

## 🛠 Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.5 |
| Security | Spring Security + JWT (JJWT 0.12.x) |
| ORM | Spring Data JPA / Hibernate |
| Database | H2 (in-memory, dev) / PostgreSQL (prod) |
| Build Tool | Maven |
| Boilerplate Reduction | Lombok |
| Validation | Jakarta Validation (`@Valid`, `@NotNull`, etc.) |

### Why SQL / Relational Database?

| Reason | Detail |
|---|---|
| **ACID Compliance** | Booking and payment require strict transactional guarantees — atomicity prevents partial bookings |
| **Complex Relationships** | Movie → Show → Seat → Booking → Payment form a deep relational graph that SQL handles natively |
| **Query Complexity** | "Shows for movie X in city Y on date Z with seats available" maps perfectly to SQL JOINs + filters |
| **Structured Schema** | Booking domain has a well-defined, stable schema — relational model is the right fit |
| **Concurrency** | `SELECT FOR UPDATE` (pessimistic lock) and `@Version` (optimistic lock) require transactional row-level isolation |

---

## 🏛 Architecture & Design Patterns

### 1. Layered Architecture

```
Client (HTTP)
    │
    ▼
Controller Layer       ← Input validation, auth extraction, HTTP response mapping
    │
    ▼
Service Layer          ← Business logic, transaction management, discount, locking
    │
    ▼
Repository Layer       ← Data access, JPQL queries, pessimistic locks
    │
    ▼
Database (H2 / PostgreSQL)
```

### 2. Strategy Pattern — Payments

Each payment method is an independent `@Component` implementing `PaymentStrategy`. `PaymentServiceImpl` resolves the correct strategy at runtime from a `Map<PaymentMethod, PaymentStrategy>` built via `@PostConstruct`.

```java
public interface PaymentStrategy {
    PaymentMethod getType();
    PaymentResponse process(double amount, PaymentRequest request);
    PaymentResponse refund(String transactionId, double amount);
}

@Component public class CardPaymentStrategy    implements PaymentStrategy { ... }
@Component public class UpiPaymentStrategy     implements PaymentStrategy { ... }
@Component public class WalletPaymentStrategy  implements PaymentStrategy { ... }
@Component public class NetBankingPaymentStrategy implements PaymentStrategy { ... }
```

**Benefits:** Open/Closed Principle — add a new payment method by adding one class; zero existing code changes.

### 3. Strategy Pattern — Discounts

```java
public interface DiscountService {
    double calculateDiscount(double totalAmount, int numberOfSeats, boolean isAfternoonShow);
}
```

Discount rules are encapsulated and can be extended (seasonal discounts, loyalty tiers) without touching `BookingServiceImpl`.

### 4. Repository Pattern

All data access goes th[BookingService.java](src/main/java/com/digvijay/bookMyShow/service/BookingService.java)rough Spring Data JPA interfaces:

```java
@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {
    @Query("SELECT s FROM Show s JOIN FETCH s.movie JOIN FETCH s.theatre " +
           "WHERE s.movie.id = :movieId AND LOWER(s.theatre.city) = LOWER(:city) " +
           "AND s.showDateTime BETWEEN :start AND :end AND s.availableSeats > 0")
    List<Show> findAvailableShowsByMovieAndCityAndDateRange(...);
}
```

### 5. DTO Pattern

Entities are never exposed directly. Every controller input/output uses a DTO:

```
Entity  ──(mapper)──▶  DTO  ──▶  Controller Response
HTTP Request  ──▶  DTO  ──(mapper)──▶  Entity
```

### 6. Filter Chain Pattern — JWT

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    protected void doFilterInternal(request, response, chain) {
        // 1. Parse Bearer token
        // 2. Validate JWT signature + expiry
        // 3. Load UserDetails from DB
        // 4. Set SecurityContextHolder
        // 5. Continue chain
    }
}
```

---

## 🧱 SOLID Principles

### Single Responsibility Principle
Each class has one reason to change:
- `BookingServiceImpl` — booking lifecycle only
- `PaymentServiceImpl` — payment orchestration only
- `JwtUtils` — JWT token operations only
- `DiscountServiceImpl` — discount calculation only
- `BookingExpiryScheduler` — expiry cleanup only

### Open/Closed Principle
- Add `SeasonalDiscountStrategy` — implement `DiscountService`, no existing code changes
- Add `CryptoPaymentStrategy` — annotate `@Component`, implement `PaymentStrategy`, auto-injected

### Liskov Substitution Principle
- Any `PaymentStrategy` implementation can replace another in the strategy map
- All repository interfaces extend `JpaRepository` — polymorphic usage throughout services

### Interface Segregation Principle
```java
public interface BookingService  { lockSeats(...); confirmBooking(...); cancelBooking(...); }
public interface ShowService     { browseShows(...); getSeatsByShow(...); }
public interface PaymentService  { processPayment(...); refundPayment(...); }
```
Controllers depend only on the service interface they need — no "god interface".

### Dependency Inversion Principle
```java
@Service
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;   // interface
    private final DiscountService discountService;        // interface
    private final PaymentService paymentService;          // interface
}
```
Spring injects concrete implementations — high-level modules depend on abstractions.

---

## 🗺 High-Level Design (HLD)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        CLIENT (Postman / Web / Mobile)                      │
└─────────────────────────┬───────────────────────────────────────────────────┘
                          │ HTTPS
                          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     SPRING BOOT APPLICATION (Port 8080)                     │
│                                                                             │
│  ┌──────────────────┐   JWT Filter    ┌──────────────────────────────────┐  │
│  │  Security Layer  │ ◄────────────── │  JwtAuthenticationFilter         │  │
│  │  Spring Security │                 │  (OncePerRequestFilter)           │  │
│  └────────┬─────────┘                 └──────────────────────────────────┘  │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      CONTROLLER LAYER                               │   │
│  │  AuthController  MovieController  ShowController                    │   │
│  │  TheatreController  BookingController  PaymentController            │   │
│  └─────────────────────────────┬───────────────────────────────────────┘   │
│                                │ @Valid DTOs                                │
│                                ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                       SERVICE LAYER                                 │   │
│  │                                                                     │   │
│  │  MovieService    ShowService    TheatreService                      │   │
│  │  BookingService  PaymentService DiscountService                     │   │
│  │                                                                     │   │
│  │  ┌─────────────────────┐   ┌───────────────────────────────────┐   │   │
│  │  │  Payment Strategies │   │  BookingExpiryScheduler           │   │   │
│  │  │  CARD | UPI | NB    │   │  @Scheduled every 2 mins          │   │   │
│  │  │  WALLET             │   │  Releases expired PENDING bookings │   │   │
│  │  └─────────────────────┘   └───────────────────────────────────┘   │   │
│  └─────────────────────────────┬───────────────────────────────────────┘   │
│                                │ @Transactional                             │
│                                ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     REPOSITORY LAYER                                │   │
│  │  UserRepo  MovieRepo  TheatreRepo  ShowRepo                         │   │
│  │  SeatRepo  BookingRepo  PaymentRepo                                 │   │
│  │                                                                     │   │
│  │  ← JPA + Hibernate ORM + PESSIMISTIC / OPTIMISTIC LOCKING →        │   │
│  └─────────────────────────────┬───────────────────────────────────────┘   │
│                                │                                           │
└────────────────────────────────┼───────────────────────────────────────────┘
                                 │
                                 ▼
             ┌────────────────────────────────────┐
             │     DATABASE (H2 / PostgreSQL)      │
             │  users  movies  theatres  shows     │
             │  seats  bookings  payments          │
             └────────────────────────────────────┘
```
## 🔬 Low-Level Design (LLD)

### Entity Relationship Diagram

```
┌──────────┐       ┌──────────┐       ┌──────────────┐
│  User    │       │  Movie   │       │   Theatre    │
├──────────┤       ├──────────┤       ├──────────────┤
│ id (PK)  │       │ id (PK)  │       │ id (PK)      │
│ username │       │ title    │       │ name         │
│ email    │       │ language │       │ city         │
│ password │       │ genre    │       │ address      │
│ roles    │       │ rating   │       │ totalSeats   │
│ enabled  │       │ duration │       └──────┬───────┘
└────┬─────┘       │ active   │              │
     │             └────┬─────┘              │
     │                  │ 1:N                │ 1:N
     │                  ▼                   ▼
     │             ┌─────────────────────────────────────────┐
     │             │                 Show                    │
     │             ├─────────────────────────────────────────┤
     │             │ id (PK)                                 │
     │             │ movie_id (FK → Movie)                   │
     │             │ theatre_id (FK → Theatre)               │
     │             │ showDateTime                            │
     │             │ showType (MORNING/AFTERNOON/EVENING/NIGHT)│
     │             │ basePrice                               │
     │             │ availableSeats                          │
     │             └────┬────────────────────────────────────┘
     │                  │ 1:N
     │                  ▼
     │             ┌──────────────────────────────────────┐
     │             │               Seat                   │
     │             ├──────────────────────────────────────┤
     │             │ id (PK)                              │
     │             │ show_id (FK → Show)                  │
     │             │ seatNumber                           │
     │             │ seatType (REGULAR/PREMIUM/VIP)       │
     │             │ status (AVAILABLE/LOCKED/BOOKED)     │
     │             │ price                                │
     │             │ booking_id (FK → Booking, nullable)  │
     │             │ lockedBy                             │
     │             │ lockExpiry                           │
     │             │ version  ← @Version optimistic lock  │
     │             └──────────────────┬───────────────────┘
     │                                │ N:1
     │                                ▼
     │      ┌──────────────────────────────────────────────┐
     └─────▶│                Booking                       │
            ├──────────────────────────────────────────────┤
            │ id (PK)                                      │
            │ user_id (FK → User)                          │
            │ show_id (FK → Show)                          │
            │ bookingReference (UNIQUE)                    │
            │ status (PENDING/CONFIRMED/CANCELLED/EXPIRED) │
            │ totalAmount  ← GROSS before discount         │
            │ discountApplied                              │
            │ lockExpiry   ← 10-min TTL                    │
            │ paymentId    ← FK to Payment.id              │
            │ confirmedAt / cancelledAt                    │
            └─────────────────┬────────────────────────────┘
                              │ 1:1
                              ▼
            ┌──────────────────────────────────────────────┐
            │               Payment                        │
            ├──────────────────────────────────────────────┤
            │ id (UUID, PK)                                │
            │ booking_id (FK → Booking)                    │
            │ amount                                       │
            │ method (CARD/UPI/NETBANKING/WALLET)          │
            │ status (PENDING/SUCCESS/FAILED/REFUNDED)     │
            │ transactionId (UNIQUE)                       │
            │ createdAt / updatedAt                        │
            └──────────────────────────────────────────────┘
```

### Booking State Machine

```
                        ┌─────────────────┐
                        │    [Start]      │
                        │  User selects   │
                        │  seats          │
                        └────────┬────────┘
                                 │
                                 ▼
                        ┌─────────────────┐
                        │    PENDING      │◄─────────────────────────┐
                        │  lockExpiry set │                          │
                        │  seats=LOCKED   │                          │
                        └────────┬────────┘                          │
                                 │                                   │
              ┌──────────────────┼──────────────────┐               │
              │                  │                  │               │
              ▼ Payment OK       ▼ Expired          ▼ Manual        │
    ┌─────────────────┐ ┌──────────────────┐ ┌─────────────────┐   │
    │   CONFIRMED     │ │    EXPIRED        │ │   CANCELLED     │   │
    │  seats=BOOKED   │ │  seats=AVAILABLE │ │  seats=AVAILABLE│   │
    │  availableSeats │ │  (by scheduler)  │ │  availableSeats │   │
    │  decremented    │ └──────────────────┘ │  restored       │   │
    └─────────────────┘                      └─────────────────┘   │
```

### Seat State Machine

```
   AVAILABLE ──[lockSeats()]──▶ LOCKED ──[confirmBooking()]──▶ BOOKED
      ▲                           │                               │
      │                           │ lockExpiry elapsed            │
      └───────────────────────────┘ (scheduler)                  │
      ▲                                                           │
      └───────────────[cancelBooking()]──────────────────────────┘
```

### Payment Strategy Resolution

```
PaymentRequest.paymentMethod = "UPI"
        │
        ▼
PaymentMethod.valueOf("UPI")  →  PaymentMethod.UPI
        │
        ▼
strategyMap.get(PaymentMethod.UPI)  →  UpiPaymentStrategy
        │
        ▼
UpiPaymentStrategy.process(amount, request)
        │
        ├── Validate upiId format (contains "@")
        ├── Generate UUID transaction ID
        └── Return PaymentResponse(status="SUCCESS", transactionId=...)
```

### Class Diagram (Service Layer)

```
«interface»                    «interface»
BookingService                 PaymentService
─────────────                  ──────────────
+ lockSeats()                  + processPayment()
+ confirmBooking()             + getPaymentByBookingId()
+ bookTickets()                + refundPayment()
+ cancelBooking()
+ getMyBookings()              «interface»
+ getBookingById()             PaymentStrategy
+ getBookingByReference()      ───────────────
        ▲                      + getType()
        │                      + process()
BookingServiceImpl             + refund()
────────────────               ▲    ▲    ▲    ▲
- bookingRepository            │    │    │    │
- showRepository         Card  UPI  NB   Wallet
- seatRepository
- discountService
- paymentService
```

---
## 🎟 Booking Flow

The system supports two booking modes:

### Mode A — Two-Phase Flow (Recommended)

```
Step 1 ─ Discover Content
  GET /api/movies                              → browse movies
  GET /api/shows/browse?movieId=1&city=Bengaluru&date=2025-04-26
                                               → available shows with seat count

Step 2 ─ Select Seats
  GET /api/shows/{showId}/seats                → seat layout (AVAILABLE / LOCKED / BOOKED)

Step 3 ─ Lock Seats                        [ @Transactional + PESSIMISTIC_WRITE ]
  POST /api/bookings/lock
  { "showId": 2, "seatIds": [11, 12, 13] }
  ─────────────────────────────────────────────────────────────
  1. Validate user, show, seat count
  2. SELECT FOR UPDATE on seat rows          ← DB-level lock
  3. Check each seat: AVAILABLE or expired LOCKED
  4. Set status=LOCKED, lockedBy, lockExpiry(+10 min)
  5. Calculate gross total + discount
  6. Create Booking(PENDING, lockExpiry)
  7. saveAll(seats) + save(booking)
  ─────────────────────────────────────────────────────────────
  → Returns BookingResponse(status=PENDING, lockExpiry, finalAmount)

Step 4 ─ Pay & Confirm                     [ @Transactional ]
  POST /api/bookings/{bookingId}/confirm
  { "paymentMethod": "UPI", "amount": 280.0, "upiId": "john@okaxis" }
  ─────────────────────────────────────────────────────────────
  1. Fetch booking, verify owner
  2. Assert status=PENDING, not expired
  3. Validate amount == booking.getFinalAmount()
  4. Resolve PaymentStrategy from strategyMap
  5. Save Payment(PENDING) → execute strategy
  6. Payment SUCCESS → update Payment(SUCCESS, transactionId)
  7. Update Booking(CONFIRMED, paymentId, confirmedAt)
  8. Mark seats BOOKED, clear lock fields
  9. Decrement show.availableSeats
  ─────────────────────────────────────────────────────────────
  → Returns BookingResponse(status=CONFIRMED, paymentId)
```

### Mode B — Direct Single-Step

```
  POST /api/bookings
  { "showId": 3, "seatIds": [50, 51] }
  → Locks + confirms atomically in one @Transactional
  → No separate payment step; status → CONFIRMED directly
```

### Auto-Expiry

```
  @Scheduled (every 2 min)
  → SELECT bookings WHERE status=PENDING AND lockExpiry < NOW()
  → For each: release seats (LOCKED → AVAILABLE), set status=EXPIRED
```

### Cancellation

```
  DELETE /api/bookings/{id}
  → Release seats (BOOKED → AVAILABLE)
  → Restore show.availableSeats
  → Set status=CANCELLED, cancelledAt
  → Refund via POST /api/payments/{paymentId}/refund (separate step)
```

---

## 🚀 Quick Start


### Prerequisites

*   Java 17 or higher    
*   Maven 3.6+
    

### Installation & Running

```bash
# Clone
git clone https://github.com/digvijay/bookmyshow.git
cd bookmyshow

# Build
mvn clean install

# Run
mvn spring-boot:run
```

Application starts on `http://localhost:8080`

### H2 Console (Dev)

```
URL:       http://localhost:8080/h2-console
JDBC URL:  jdbc:h2:mem:bookmyshow
Username:  sa
Password:  (leave empty)
```
---

## 🧪 Sample Data (Auto-Seeded)

Loaded on startup via `StartUpDataLoader`. Seeding is **idempotent** — safe to restart.

**Users:**

| Username | Password | Roles |
|---|---|---|
| `john` | `password123` | USER |
| `digvijay` | `admin123` | USER, ADMIN |

**Movies:** Shutter Island · The War Machine · F1

**Theatres:**

| Name | City |
|---|---|
| Cinepolis | Bengaluru |
| INOX | Bengaluru |
| PVR | Delhi |

**Shows:** 4 timeslots per movie-theatre pair (MORNING / AFTERNOON / EVENING / NIGHT)

**Seats per show:** 60% REGULAR · 30% PREMIUM · 10% VIP (pricing: base × 1.0 / 1.5 / 2.0)

---

## 🗄 Database Design

### Schema Overview

```sql
-- Users & Roles
users        (id BIGINT PK, username UNIQUE, email UNIQUE, password, enabled BOOLEAN)
user_roles   (user_id FK, role VARCHAR)

-- Content Catalogue
movies       (id BIGINT PK, title, description, language, genre,
              duration_minutes INT, rating, active BOOLEAN)

theatres     (id BIGINT PK, name, city, address, total_seats INT)

-- Showtime
shows        (id BIGINT PK,
              movie_id FK → movies,
              theatre_id FK → theatres,
              show_date_time TIMESTAMP,       ← INDEX
              show_type ENUM,
              base_price DOUBLE,
              available_seats INT)            ← decremented on confirm

-- Inventory
seats        (id BIGINT PK,
              show_id FK → shows,            ← INDEX
              seat_number VARCHAR,
              seat_type ENUM,
              status ENUM,                   ← INDEX (AVAILABLE/LOCKED/BOOKED)
              price DOUBLE,
              booking_id FK → bookings NULL,
              locked_by VARCHAR,
              locked_at TIMESTAMP,
              lock_expiry TIMESTAMP,
              version BIGINT)               ← @Version — optimistic lock counter

-- Transactions
bookings     (id BIGINT PK,
              user_id FK → users,            ← INDEX
              show_id FK → shows,
              booking_date_time TIMESTAMP,
              total_amount DOUBLE,           ← GROSS (before discount)
              discount_applied DOUBLE,
              status ENUM,
              booking_reference VARCHAR UNIQUE, ← INDEX
              lock_expiry TIMESTAMP,
              payment_id VARCHAR,
              confirmed_at TIMESTAMP,
              cancelled_at TIMESTAMP)

payments     (id VARCHAR PK (UUID),
              booking_id FK → bookings,
              amount DOUBLE,
              method ENUM,
              status ENUM,
              transaction_id VARCHAR UNIQUE,
              created_at TIMESTAMP NOT NULL,  ← @PrePersist sets this
              updated_at TIMESTAMP)           ← @PreUpdate sets this
```

### Indexing Strategy

| Table | Index | Reason |
|---|---|---|
| `shows` | `(movie_id, show_date_time)` | Browse query filters on both |
| `shows` | `(theatre_id)` | Theatre show listing |
| `seats` | `(show_id, status)` | Seat availability lookup |
| `bookings` | `(user_id)` | Booking history fetch |
| `bookings` | `(booking_reference)` UNIQUE | Ticket lookup |
| `bookings` | `(status, lock_expiry)` | Expiry scheduler query |

---

## ⚡ Concurrency & Locking Strategy

Seat booking is the most race-condition-prone operation in the system. Two independent layers protect against double-booking:

### Layer 1 — Pessimistic Locking (DB Row-Level Lock)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)   // → SELECT ... FOR UPDATE
@Query("SELECT s FROM Seat s WHERE s.show.id = :showId AND s.id IN :seatIds")
List<Seat> findByShowIdAndSeatIdsWithLock(Long showId, List<Long> seatIds);
```

When Transaction A acquires this lock:
- Database issues `SELECT ... FOR UPDATE` on the targeted seat rows
- Transaction B attempting the same seats **blocks at the DB level**
- B is unblocked only after A commits or rolls back
- If A locked the seats: B reads `status = LOCKED` → throws `SeatAlreadyBookedException` (409)
- This works correctly even across multiple application instances

### Layer 2 — Optimistic Locking (Version Check)

```java
@Version
private Long version;  // Hibernate increments this on every UPDATE
```

Every `UPDATE seats SET ... WHERE id=? AND version=?` — if the version has changed (another transaction beat us), Hibernate throws `ObjectOptimisticLockingFailureException` → caught by `GlobalExceptionHandler` → returns `409 CONFLICT`.

### Race Condition Scenario

```
User A: wants R1, R2, R3       User B: wants R2, R3, R4
─────────────────────────────────────────────────────────
T=0ms   A: BEGIN TRANSACTION
T=0ms   A: SELECT FOR UPDATE (R1, R2, R3)  → LOCKS rows
T=1ms   B: BEGIN TRANSACTION
T=1ms   B: SELECT FOR UPDATE (R2, R3, R4)  → BLOCKS (R2, R3 locked)
T=50ms  A: status=LOCKED, save, COMMIT  →  rows RELEASED
T=50ms  B: UNBLOCKED → reads R2.status=LOCKED, R3.status=LOCKED
T=50ms  B: isAvailableForLocking() = false for R2, R3
T=50ms  B: throws SeatAlreadyBookedException
T=50ms  B: ROLLBACK → 409 CONFLICT returned to user B
```

### Lock Expiry Cleanup

A scheduled job runs every 2 minutes to release stale locks from abandoned bookings:

```java
@Scheduled(fixedDelay = 120_000)
public void expireStaleBookings() {
    List<Booking> expired = bookingRepository
        .findByStatusAndLockExpiryBefore(BookingStatus.PENDING, LocalDateTime.now());
    expired.forEach(bookingService::expireBooking);
}
```

---

## 📡 API Reference

### Auth APIs (Public)

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | `/api/auth/signup` | No | Register user |
| POST | `/api/auth/login` | No | Login + get JWT |

### Movie APIs

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| GET | `/api/movies` | No | List all active movies |
| GET | `/api/movies/{id}` | No | Get movie by ID |
| GET | `/api/movies/search?title=&genre=&language=` | No | Search movies |
| POST | `/api/movies` | ADMIN | Create movie |
| PUT | `/api/movies/{id}` | ADMIN | Update movie |
| DELETE | `/api/movies/{id}` | ADMIN | Soft-delete movie |

### Theatre APIs

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| GET | `/api/theatres` | No | List all theatres |
| GET | `/api/theatres/{id}` | No | Get theatre by ID |
| GET | `/api/theatres/city/{city}` | No | Theatres in city |
| POST | `/api/theatres` | ADMIN | Create theatre |

### Show APIs

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| GET | `/api/shows/browse?movieId=&city=&date=` | No | Browse shows |
| GET | `/api/shows/{id}` | No | Show details |
| GET | `/api/shows/{id}/seats` | No | Seat layout for show |
| POST | `/api/shows` | ADMIN | Create show |

### Booking APIs

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | `/api/bookings/lock` | USER | Phase 1: Lock seats (PENDING, 10min TTL) |
| POST | `/api/bookings/{id}/confirm` | USER | Phase 2: Pay and confirm |
| POST | `/api/bookings` | USER | Direct book (single step) |
| GET | `/api/bookings/my` | USER | My booking history |
| GET | `/api/bookings/{id}` | USER | Get booking by ID |
| GET | `/api/bookings/reference/{ref}` | USER | Get by reference (e.g. BMS-A1B2C3D4) |
| DELETE | `/api/bookings/{id}` | USER | Cancel booking |

### Payment APIs

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | `/api/payments` | USER | Process payment |
| GET | `/api/payments/booking/{bookingId}` | USER | Payment by booking |
| POST | `/api/payments/{paymentId}/refund` | USER | Refund payment |


---
## 📦 API Documentation

### Authentication APIs

#### 1\. Register User

```  json
POST /api/auth/signup  Content-Type: application/json
{
"username": "john",
"email": "john@example.com",
"password": "password123"
}
```

**Response:**

`   "User registered successfully!"   `

#### 2\. Login

```   json
POST /api/auth/login    Content-Type: application/json
{
"username": "john",
 "password": "password123"
}
```

**Response:**

``` json
{
"token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
"type": "Bearer",    "id": 1,
"username": "john",
 "email": "john@example.com",
"roles": ["USER"]
}
```

### Show Browsing API (READ Scenario)

#### Browse Shows by Movie, City, and Date


```
GET /api/shows/browse?movieId=1&city=Mumbai&date=2026-04-21
Authorization: Bearer
```

**Response:**


```json
[    
{      
"id": 1,      
"movieId": 1,      
"movieTitle": "Shutter Island",     
"theatreId": 1,      
"theatreName": "Cinepolis",      
"theatreCity": "Bengaluru",      
"theatreAddress": "Nexus Shantiniketan, Whitefield",      
"showDateTime": "2026-04-21T10:00:00",      
"basePrice": 200.0,     
"showType": "MORNING",      
"availableSeats": 100    
},    
{      
"id": 2,      
"movieId": 1,      
"movieTitle": "Shutter Island",      
"theatreId": 1,      
"theatreName": "Cinepolis",      
"theatreCity": "Bengaluru",      
"theatreAddress": "Nexus Shantiniketan, Whitefield",      
"showDateTime": "2026-04-21T14:00:00",      
"basePrice": 150.0,      
"showType": "AFTERNOON",     
"availableSeats": 100    
}  
] 
```

### Booking API (WRITE Scenario)

### GET /api/shows/1/seats (sample)
**Response (200):**
```json
[
  { "id": 1,  "seatNumber": "R1",  "seatType": "REGULAR", "status": "AVAILABLE", "price": 150.0 },
  { "id": 67, "seatNumber": "P67", "seatType": "PREMIUM", "status": "AVAILABLE", "price": 225.0 },
  { "id": 100,"seatNumber": "V100","seatType": "VIP",     "status": "BOOKED",    "price": 300.0 }
]
```

### POST /api/bookings/lock
**Request:**
```json
{
  "showId": 2,
  "seatIds": [1, 2, 3]
}
```
**Response (201):**
```json
{
  "bookingId": 1,
  "bookingReference": "BMS-A1B2C3D4",
  "showId": 2,
  "movieTitle": "Shutter Island",
  "theatreName": "Cinepolis",
  "showDateTime": "2025-04-26T14:00:00",
  "seatNumbers": ["R1", "R2", "R3"],
  "totalAmount": 450.0,
  "discountApplied": 170.0,
  "finalAmount": 280.0,
  "status": "PENDING",
  "bookingDateTime": "2025-04-26T10:32:00",
  "lockExpiry": "2025-04-26T10:42:00",
  "message": "Seats locked for 10 minutes. Complete payment to confirm."
}
```

### POST /api/bookings/1/confirm
**Request:**
```json
{
  "bookingId": 1,
  "paymentMethod": "UPI",
  "amount": 280.0,
  "upiId": "john@okaxis"
}
```
**Response (200):**
```json
{
  "bookingId": 1,
  "bookingReference": "BMS-A1B2C3D4",
  "movieTitle": "Shutter Island",
  "theatreName": "Cinepolis",
  "showDateTime": "2025-04-26T14:00:00",
  "seatNumbers": ["R1", "R2", "R3"],
  "totalAmount": 450.0,
  "discountApplied": 170.0,
  "finalAmount": 280.0,
  "status": "CONFIRMED",
  "paymentId": "f4c2e9a1-...",
  "message": "Booking confirmed successfully!"
}
```

### POST /api/bookings (direct single-step)
**Request:**
```json
{
  "showId": 3,
  "seatIds": [50, 51]
}
```
**Response (201):**
```json
{
  "bookingReference": "BMS-XY12WZ34",
  "status": "CONFIRMED",
  "totalAmount": 500.0,
  "discountApplied": 0.0,
  "finalAmount": 500.0,
  "seatNumbers": ["P50", "P51"],
  "message": "Booking successful!"
}
```

### Validation Error (400)
```json
{
  "timestamp": "2025-04-26T10:30:00",
  "message": "Validation failed",
  "status": 400,
  "errors": [
    { "field": "seatIds", "message": "At least one seat must be selected" },
    { "field": "showId",  "message": "Show ID is required" }
  ]
}
```

### Seat Conflict Error (409)
```json
{
  "timestamp": "2025-04-26T10:30:00",
  "message": "Seats not available: [R1 (LOCKED), R2 (BOOKED)]",
  "status": 409
}
```

---

## 💸 Discount Engine

Discounts are computed in `DiscountServiceImpl` before creating the booking. The gross total is stored in `totalAmount`; discount is stored in `discountApplied`. `getFinalAmount() = totalAmount - discountApplied`.

| Rule | Condition | Discount |
|---|---|---|
| 3rd Ticket | Booking 3 or more seats | 50% off the average per-seat price |
| Afternoon Show | `showType = AFTERNOON` | 20% off gross total |
| Cap | Both rules apply | Total discount capped at 50% of gross |

**Example** — 3 AFTERNOON seats @ ₹150 each:

```
Gross total       = 3 × 150        = ₹450
3rd ticket disc   = (450/3) × 0.5  = ₹75
Afternoon disc    = 450 × 0.20     = ₹90
Combined          = 75 + 90        = ₹165
Cap (50% of 450)  = ₹225           → ₹165 < cap, no adjustment
Final amount      = 450 − 165      = ₹285
```

---

## 🔐Security

### JWT Authentication

1.  **Register or Login** to get a JWT token  
2.  Authorization: Bearer
    

### Protected Endpoints

*   `/api/shows/browse/\*\*` - Requires authentication   
*   `/api/bookings/\*\*` - Requires authentication
    

### Public Endpoints

*   `/api/auth/signup` - User registration    
*   `/api/auth/login` - User login
    

### Password Security

*   Passwords are encrypted using BCrypt    
*   Never stored in plain text    

---

## 🚨 Exception Handling

All exceptions funnel through `GlobalExceptionHandler` (`@RestControllerAdvice`) and return a consistent `ErrorResponse`:

```json
{
  "timestamp": "2025-04-26T10:30:00",
  "message": "Human-readable description",
  "details": "uri=/api/bookings/lock",
  "status": 409,
  "errors": [
    { "field": "seatIds", "message": "At least one seat must be selected" }
  ]
}
```

| Exception | HTTP Status |
|---|---|
| `ResourceNotFoundException` | `404 Not Found` |
| `BookingException` | `400 Bad Request` |
| `SeatAlreadyBookedException` | `409 Conflict` |
| `MethodArgumentNotValidException` | `400` with field-level errors |
| `BadCredentialsException` / `UsernameNotFoundException` | `401 Unauthorized` |
| `AccessDeniedException` | `403 Forbidden` |
| `ObjectOptimisticLockingFailureException` | `409 Conflict` |
| `DataIntegrityViolationException` | `409 Conflict` |
| `MissingServletRequestParameterException` | `400 Bad Request` |
| `MethodArgumentTypeMismatchException` | `400 Bad Request` |
| `Exception` (catch-all) | `500` (internal detail sanitized from response) |

---

## 💡 ASSUMPTIONS & FUTURE IMPROVEMENTS

### Assumptions Made
- Payment is simulated — all strategies return SUCCESS by default
- No real gateway (Razorpay/Stripe) integration
- Screen entity de-scoped — Show maps directly to Theatre (sufficient for MVP)
- Discount rules are hardcoded; no DB-driven rule engine
- Refund does not automatically trigger `cancelBooking` — handled separately

### Recommended Production Improvements
- Switch H2 → PostgreSQL (`spring.datasource.url=jdbc:postgresql://...`)
- Add Redis cache for seat status reads (reduce DB load)
- Implement gateway webhook for async payment confirmation
- Add `@Cacheable` on `getAllMovies()`, `browseShows()`
- Add rate limiting (Bucket4j) on `/api/bookings`
- Replace `synchronized` in Seat entity with DB-only locking
- Add email notification on booking confirmation
- Implement coupon/promo code system in `DiscountService`
- Add admin dashboard endpoints for booking analytics

---

<p align="center">Built with ❤️ by <b>Digvijay</b> · Spring Boot · Java 21</p>
