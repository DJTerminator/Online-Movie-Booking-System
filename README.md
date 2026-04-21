# *READ Scenario*

Browse Theatres: Browse theatres currently running a selected movie in a town, including show timings by date 
 - Filter by movie, city, and date
 - View available seats for each show
 - See pricing information

# *WRITE Scenario*

Book Tickets: Book movie tickets by selecting theatre, timing, and preferred seats
 - Select multiple seats
 - Automatic discount calculation:
 - 50% discount on the 3rd ticket, 20% discount for afternoon shows
 - Generate unique booking reference
 - Real-time seat availability checking

# *Additional Features*

- JWT-based authentication and authorization
- User registration and login
- Secure API endpoints
- Global exception handling
- Transaction management
- Sample data initialization

Technology Stack
-------------------

*   **Language**: Java 21
*   **Framework**: Spring Boot 4.0.5    
*   **Security**: Spring Security with JWT    
*   **Database**: H2 (in-memory) / PostgreSQL   
*   **ORM**: Spring Data JPA / Hibernate    
*   **Build Tool**: Maven    
*   **Additional Libraries**: 
    *   Lombok (reduce boilerplate)   
    *   JWT (io.jsonwebtoken)   
    *   Jakarta Validation
        

Database Choice: SQL
--------------------------------

### Why SQL (Relational Database)?

I chose **SQL database (H2/PostgreSQL)** for this project based on the following reasons:

#### 1. **ACID Compliance**

*   Ticket booking requires strong transactional guarantees    
*   Need to ensure seat availability is consistent across concurrent bookings   
*   Financial transactions (payments) require atomicity
    
#### 2. **Complex Relationships**

*   Multiple entities with well-defined relationships:
    
    *   Movie → Show (One-to-Many)   
    *   Theatre → Show (One-to-Many)    
    *   Show → Seat (One-to-Many)    
    *   User → Booking (One-to-Many)   
    *   Booking → Seat (One-to-Many)
        
*   JOINs are efficient for querying related data
 
#### 3. **Query Complexity**

*   Complex queries like "Find all shows for a movie in a city on a specific date"    
*   SQL excels at filtering, sorting, and aggregating structured data   
*   Need for transaction isolation to handle race conditions
    

#### 5. **Structured Schema**

*   Movie booking domain has a well-defined, stable schema   
*   Entities have clear attributes that don't change frequently   
*   Schema migrations are manageable

Architecture & Design Patterns
------------------------------

### 1. **Layered Architecture**

`   Controller Layer → Service Layer → Repository Layer → Database   `

*   Clear separation of concerns   
*   Easy to test and maintain
    

### 2. **Strategy Pattern**

**Location**: DiscountStrategy interface and implementations
```
   public interface DiscountStrategy {      
      double calculateDiscount(double totalAmount, int numberOfSeats, boolean isAfternoonShow); 
      }  
      @Component  
      public class CompositeDiscountStrategy implements DiscountStrategy {      
      // Implementation that combines multiple discount rules  
      }
```   

**Benefits**:

*   Open/Closed Principle: Add new discount strategies without modifying existing code    
*   Flexible discount calculation       

### 3. **Repository Pattern**

**Location**: All repository interfaces (UserRepository, ShowRepository, etc.)
```
  @Repository  public interface ShowRepository extends JpaRepository {
      List findShowsByMovieAndCityAndDate(...);  
    }
```

**Benefits**:

*   Abstraction over data access    
*   Dependency Inversion Principle    

### 4. **DTO Pattern**

**Location**: dto package (ShowDTO, BookingRequest, BookingResponse)

**Benefits**:

*   Separation between domain entities and API contracts    
*   Control over what data is exposed    
*   Version API responses independently
    

### 5. **Builder Pattern**

**Location**: JPA entities using Lombok's @Data, @Builder

**Benefits**:

*   Clean object construction    
*   Immutability where needed    
*   Readable code
    

### 6. **Filter Chain Pattern**

**Location**: JwtAuthenticationFilter
```
    public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override      
    protected void doFilterInternal(...) {  
    // JWT validation and authentication      
    } }
```

**Benefits**:

*   Cross-cutting concerns (authentication)    
*   Clean separation from business logic
    

SOLID Principles
-------------------

### 1. **Single Responsibility Principle (SRP)**

Each class has one reason to change:

*   ```
    @Service
    public class ShowBrowsingServiceImpl implements ShowBrowsingService {
    // Only responsible for browsing shows
    }
    ```    
*   **BookingServiceImpl**: Only handles booking logic   
*   **JwtUtils**: Only handles JWT token operations   
*   **UserDetailsServiceImpl**: Only loads user details
    

### 2. **Open/Closed Principle (OCP)**

Open for extension, closed for modification:

*   `// Can add new strategies like SeasonalDiscountStrategy, StudentDiscountStrategypublic class SeasonalDiscountStrategy implements DiscountStrategy { // New implementation}   `
*   **Service interfaces**: Can create new implementations without changing existing code
    

### 3. **Liskov Substitution Principle (LSP)**

Subtypes can replace base types:

*   **Repository interfaces**: All extend JpaRepository, can be substituted    
*   **DiscountStrategy**: Any implementation can replace another    
*   **UserDetailsService**: Custom implementation can replace default
    

### 4. **Interface Segregation Principle (ISP)**

Clients shouldn't depend on interfaces they don't use:

*   ```
    public interface ShowBrowsingService {
     List browseShowsByMovieCityAndDate(...);
    }
    public interface BookingService {
     BookingResponse bookTickets(...)
    ;}
    ```   
*   Controllers only depend on the specific service they need    
*   No "god interface" with all methods
    

### 5. **Dependency Inversion Principle (DIP)**

Depend on abstractions, not concretions:

*   ```
    @Service
    @RequiredArgsConstructor
    public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
      private final ShowRepository showRepository; // Interface, not class
     private final DiscountStrategy discountStrategy; // Interface, not class
    }
    ```   
*   ```
    @RestController
    @RequiredArgsConstructor
    public class ShowController {
    private final ShowBrowsingService showBrowsingService; // Interface
    }
    ```  
*   Spring's dependency injection manages concrete implementations

Getting Started
----------------

### Prerequisites

*   Java 17 or higher    
*   Maven 3.6+
    

### Installation & Running

1.  **Clone the repository**
    
`   cd Online-Movie-Booking-System  `

2.  **Build the project**
    
`   mvn clean install   `  

3.  **Run the application**   

`   mvn spring-boot:run   `

4.  **Access the application**
    
*   API Base URL: http://localhost:8080    
*   H2 Console: http://localhost:8080/h2-console
    
    *   JDBC URL: jdbc:h2:mem:bookmyshow    
    *   Username: sa    
    *   Password: (leave empty)
        

## Sample Data

The application automatically initializes with sample data:

**Users:**

*   Username: john, Password: password123, Role: USER    
*   Username: digvijay, Password: admin123, Roles: USER, ADMIN
    

**Movies:**

*   Shutter Island (English, Sci-Fi)   
*   The War Machine (English, Action)   
*   F1 (Telugu, Action)
    

**Theatres:**

*   Cinepolis (Bengaluru)    
*   INOX (Bengaluru)   
*   PVR (Delhi)
    

**Shows:** Multiple shows per day for each movie-theatre combination

API Documentation
--------------------

### Authentication APIs

#### 1\. Register User

```  
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

```   
POST /api/auth/login    Content-Type: application/json
{
"username": "john",
 "password": "password123"
}
```

**Response:**

```  
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


```
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

#### Book Tickets

```
POST /api/bookings  
Authorization: Bearer
Content-Type: application/json
{
"showId": 1,
"seatIds": [1, 2, 3]
}
```

**Response:**

```
{    
"bookingId": 1,
"bookingReference": "BMS-A1B2C3D4",
"showId": 1,
"movieTitle": "Shutter Island",
"theatreName": "Cinepolis",
"showDateTime": "2026-01-20T10:00:00",
"seatNumbers": ["R1", "R2", "R3"],
"totalAmount": 500.0,
"discountApplied": 100.0,
"status": "CONFIRMED",
"bookingDateTime": "2026-01-20T09:30:00"
}
```  

**Discount Calculation:**

*   Base amount: 3 seats × 200 = 600    
*   50% discount on 3rd ticket: -100   
*   Final amount: 500
    

### Error Responses

```  
{
"timestamp": "2026-01-20T10:00:00",
"message": "Movie not found with id: 999",
"details": "uri=/api/shows/browse",
"status": 404
}
```

🔒 Security
-----------

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
