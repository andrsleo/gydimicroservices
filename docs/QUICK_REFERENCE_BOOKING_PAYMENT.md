# Quick Reference: Booking & Payment System

**For Developers** | **Last Updated:** 2025-11-24

---

## Table Structure Cheat Sheet

### referrals.booking
```
booking_id (PK) → BIGSERIAL
referral_link_id (FK) → referrals.referral_link.id
property_id (FK) → properties.properties.id
start_date, end_date → DATE (end > start, start >= today)
client_email, client_first_name, client_last_name, client_phone
total_amount, currency → DECIMAL, VARCHAR(3)
status → booking_status ENUM (REQUEST | RESERVED | FINISHED | CANCELED)
cancellation_reason, canceled_by, canceled_at
created_at, updated_at → TIMESTAMP WITH TIME ZONE
```

### payment.booking
```
payment_booking_id (PK) → BIGSERIAL
booking_id (FK, UNIQUE) → referrals.booking.booking_id
user_id (FK) → users.users.id (referring user)
property_id (FK) → properties.properties.id
percentage_commission, commission_amount → DECIMAL
currency → VARCHAR(3)
payment_status → payment_status ENUM (PENDING | PROCESSING | SUCCESS | FAILED | CANCELED)
payment_method, transaction_id, gateway_name, gateway_response
paid_at → TIMESTAMP WITH TIME ZONE (auto-set on SUCCESS)
created_at, updated_at → TIMESTAMP WITH TIME ZONE
```

---

## Status Flows

### Booking Status
```
REQUEST
  ├─→ RESERVED (owner confirms)
  │     ├─→ FINISHED (client checks out) → CREATE PAYMENT
  │     └─→ CANCELED (canceled after confirmation)
  └─→ CANCELED (canceled before confirmation)
```

### Payment Status
```
PENDING (payment record created)
  └─→ PROCESSING (payment gateway invoked)
        ├─→ SUCCESS (payment completed) → paid_at auto-set
        └─→ FAILED (payment error)

Any status → CANCELED (if booking canceled before payment)
```

---

## Common JPA Entity Patterns

### Booking Entity (Domain Layer)

```java
package com.affiliate.rentals.gydi.referrals.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(name = "booking", schema = "referrals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long id;

    @Column(name = "referral_link_id", nullable = false)
    private Long referralLinkId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "client_email", nullable = false, length = 255)
    private String clientEmail;

    @Column(name = "client_first_name", nullable = false, length = 100)
    private String clientFirstName;

    @Column(name = "client_last_name", nullable = false, length = 100)
    private String clientLastName;

    @Column(name = "client_phone", length = 20)
    private String clientPhone;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status = BookingStatus.REQUEST;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "canceled_by", length = 50)
    private String canceledBy;

    @Column(name = "canceled_at")
    private ZonedDateTime canceledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = ZonedDateTime.now();
        updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }

    // Business Methods
    public void confirm() {
        if (this.status != BookingStatus.REQUEST) {
            throw new IllegalStateException("Can only confirm REQUEST bookings");
        }
        this.status = BookingStatus.RESERVED;
    }

    public void finish() {
        if (this.status != BookingStatus.RESERVED) {
            throw new IllegalStateException("Can only finish RESERVED bookings");
        }
        this.status = BookingStatus.FINISHED;
    }

    public void cancel(String reason, String canceledBy) {
        if (this.status == BookingStatus.FINISHED) {
            throw new IllegalStateException("Cannot cancel FINISHED bookings");
        }
        this.status = BookingStatus.CANCELED;
        this.cancellationReason = reason;
        this.canceledBy = canceledBy;
        this.canceledAt = ZonedDateTime.now();
    }
}
```

### BookingStatus Enum

```java
package com.affiliate.rentals.gydi.referrals.domain.model;

public enum BookingStatus {
    REQUEST,
    RESERVED,
    FINISHED,
    CANCELED
}
```

### PaymentBooking Entity (Domain Layer)

```java
package com.affiliate.rentals.gydi.payment.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "booking", schema = "payment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_booking_id")
    private Long id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "percentage_commission", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageCommission;

    @Column(name = "commission_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    @Column(name = "gateway_name", length = 100)
    private String gatewayName;

    @Column(name = "gateway_response", columnDefinition = "jsonb")
    private String gatewayResponse;

    @Column(name = "paid_at")
    private ZonedDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = ZonedDateTime.now();
        updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }

    // Business Methods
    public void markAsProcessing() {
        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new IllegalStateException("Can only process PENDING payments");
        }
        this.paymentStatus = PaymentStatus.PROCESSING;
    }

    public void markAsSuccess(String transactionId, String gatewayResponse) {
        if (this.paymentStatus != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Can only succeed PROCESSING payments");
        }
        this.paymentStatus = PaymentStatus.SUCCESS;
        this.transactionId = transactionId;
        this.gatewayResponse = gatewayResponse;
        this.paidAt = ZonedDateTime.now();
    }

    public void markAsFailed(String errorResponse) {
        this.paymentStatus = PaymentStatus.FAILED;
        this.gatewayResponse = errorResponse;
    }

    public void cancel() {
        this.paymentStatus = PaymentStatus.CANCELED;
    }
}
```

### PaymentStatus Enum

```java
package com.affiliate.rentals.gydi.payment.domain.model;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELED
}
```

---

## Key Validation Rules

### Spring Validation Annotations

```java
// In CreateBookingRequest DTO
@NotNull(message = "Referral link ID is required")
private Long referralLinkId;

@NotNull(message = "Property ID is required")
private Long propertyId;

@NotNull(message = "Start date is required")
@Future(message = "Start date must be in the future")
private LocalDate startDate;

@NotNull(message = "End date is required")
@Future(message = "End date must be in the future")
private LocalDate endDate;

@Email(message = "Invalid email format")
@NotBlank(message = "Client email is required")
private String clientEmail;

@NotBlank(message = "Client first name is required")
@Size(max = 100, message = "First name too long")
private String clientFirstName;

@NotBlank(message = "Client last name is required")
@Size(max = 100, message = "Last name too long")
private String clientLastName;

@Pattern(regexp = "^\\+?[0-9]{10,20}$", message = "Invalid phone number")
private String clientPhone;

@NotNull(message = "Total amount is required")
@Positive(message = "Total amount must be positive")
private BigDecimal totalAmount;

@Pattern(regexp = "^(USD|EUR|GBP|MXN|COP)$", message = "Invalid currency")
private String currency = "USD";

// Custom validator for end_date > start_date
@AssertTrue(message = "End date must be after start date")
public boolean isEndDateAfterStartDate() {
    return endDate != null && startDate != null && endDate.isAfter(startDate);
}
```

---

## Application Service Example

### BookingService (Use Case)

```java
package com.affiliate.rentals.gydi.referrals.application.usecase;

import com.affiliate.rentals.gydi.referrals.domain.model.Booking;
import com.affiliate.rentals.gydi.referrals.domain.model.BookingStatus;
import com.affiliate.rentals.gydi.payment.domain.model.PaymentBooking;
import com.affiliate.rentals.gydi.payment.domain.model.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PaymentBookingRepository paymentBookingRepository;
    private final ReferralLinkRepository referralLinkRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    @Transactional
    public Booking createBooking(CreateBookingRequest request) {
        // 1. Validate referral link exists
        var referralLink = referralLinkRepository.findById(request.getReferralLinkId())
            .orElseThrow(() -> new NotFoundException("Referral link not found"));

        // 2. Check property availability (no overlapping bookings)
        boolean isAvailable = bookingRepository.isPropertyAvailable(
            request.getPropertyId(),
            request.getStartDate(),
            request.getEndDate()
        );
        if (!isAvailable) {
            throw new PropertyNotAvailableException("Property not available for selected dates");
        }

        // 3. Create booking
        Booking booking = Booking.builder()
            .referralLinkId(request.getReferralLinkId())
            .propertyId(request.getPropertyId())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .clientEmail(request.getClientEmail())
            .clientFirstName(request.getClientFirstName())
            .clientLastName(request.getClientLastName())
            .clientPhone(request.getClientPhone())
            .totalAmount(request.getTotalAmount())
            .currency(request.getCurrency())
            .status(BookingStatus.REQUEST)
            .build();

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Booking not found"));

        booking.confirm(); // Domain method handles validation
        return bookingRepository.save(booking);
    }

    @Transactional
    public PaymentBooking finishBookingAndCreatePayment(Long bookingId) {
        // 1. Get booking
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Booking not found"));

        // 2. Mark booking as finished
        booking.finish(); // Domain method validates status
        bookingRepository.save(booking);

        // 3. Get referring user's subscription
        var referralLink = referralLinkRepository.findById(booking.getReferralLinkId())
            .orElseThrow(() -> new NotFoundException("Referral link not found"));

        var userSubscription = userSubscriptionRepository.findActiveByUserId(referralLink.getUserId())
            .orElseThrow(() -> new NotFoundException("User has no active subscription"));

        // 4. Calculate commission
        BigDecimal commissionRate = userSubscription.getSubscriptionPlan().getCommissionRate();
        BigDecimal commissionAmount = booking.getTotalAmount()
            .multiply(commissionRate)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // 5. Create payment record
        PaymentBooking payment = PaymentBooking.builder()
            .bookingId(booking.getId())
            .userId(referralLink.getUserId())
            .propertyId(booking.getPropertyId())
            .percentageCommission(commissionRate)
            .commissionAmount(commissionAmount)
            .currency(booking.getCurrency())
            .paymentStatus(PaymentStatus.PENDING)
            .build();

        return paymentBookingRepository.save(payment);
    }

    @Transactional
    public Booking cancelBooking(Long bookingId, String reason, String canceledBy) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Booking not found"));

        booking.cancel(reason, canceledBy); // Domain method handles validation
        return bookingRepository.save(booking);
    }
}
```

---

## Repository Patterns

### BookingRepository (Custom Query Methods)

```java
package com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence;

import com.affiliate.rentals.gydi.referrals.domain.model.Booking;
import com.affiliate.rentals.gydi.referrals.domain.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Find all bookings for a referral link
    List<Booking> findByReferralLinkIdOrderByCreatedAtDesc(Long referralLinkId);

    // Find bookings by status
    List<Booking> findByStatus(BookingStatus status);

    // Check property availability (no overlapping bookings)
    @Query("""
        SELECT CASE WHEN COUNT(b) = 0 THEN true ELSE false END
        FROM Booking b
        WHERE b.propertyId = :propertyId
          AND b.status IN ('RESERVED', 'FINISHED')
          AND b.startDate < :endDate
          AND b.endDate > :startDate
        """)
    boolean isPropertyAvailable(
        @Param("propertyId") Long propertyId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // Find active bookings by referral link
    @Query("""
        SELECT b FROM Booking b
        WHERE b.referralLinkId = :referralLinkId
          AND b.status IN ('REQUEST', 'RESERVED')
        ORDER BY b.createdAt DESC
        """)
    List<Booking> findActiveBookingsByReferralLink(@Param("referralLinkId") Long referralLinkId);

    // Find bookings in date range
    @Query("""
        SELECT b FROM Booking b
        WHERE b.startDate >= :startDate
          AND b.endDate <= :endDate
        ORDER BY b.startDate ASC
        """)
    List<Booking> findBookingsInDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
```

### PaymentBookingRepository

```java
package com.affiliate.rentals.gydi.payment.infrastructure.out.persistence;

import com.affiliate.rentals.gydi.payment.domain.model.PaymentBooking;
import com.affiliate.rentals.gydi.payment.domain.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentBookingRepository extends JpaRepository<PaymentBooking, Long> {

    // Find payment by booking ID
    Optional<PaymentBooking> findByBookingId(Long bookingId);

    // Find all payments for a user
    List<PaymentBooking> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Find successful payments for earnings history (paginated)
    @Query("""
        SELECT p FROM PaymentBooking p
        WHERE p.userId = :userId
          AND p.paymentStatus = 'SUCCESS'
        ORDER BY p.paidAt DESC
        """)
    Page<PaymentBooking> findSuccessfulPaymentsByUser(
        @Param("userId") Long userId,
        Pageable pageable
    );

    // Calculate total earnings for user
    @Query("""
        SELECT COALESCE(SUM(p.commissionAmount), 0)
        FROM PaymentBooking p
        WHERE p.userId = :userId
          AND p.paymentStatus = 'SUCCESS'
        """)
    BigDecimal calculateTotalEarnings(@Param("userId") Long userId);

    // Find pending payments for batch processing
    @Query("""
        SELECT p FROM PaymentBooking p
        WHERE p.paymentStatus = 'PENDING'
          AND p.createdAt <= CURRENT_TIMESTAMP - INTERVAL '5 minutes'
        ORDER BY p.createdAt ASC
        """)
    List<PaymentBooking> findPendingPaymentsForProcessing(Pageable pageable);

    // Find payments by status
    List<PaymentBooking> findByPaymentStatus(PaymentStatus status);
}
```

---

## REST API Endpoints (Recommended)

### BookingController

```java
package com.affiliate.rentals.gydi.referrals.infrastructure.in.rest;

import com.affiliate.rentals.gydi.referrals.application.dto.BookingResponse;
import com.affiliate.rentals.gydi.referrals.application.dto.CreateBookingRequest;
import com.affiliate.rentals.gydi.referrals.application.usecase.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.createBooking(request);
    }

    @GetMapping("/{id}")
    public BookingResponse getBooking(@PathVariable Long id) {
        return bookingService.getBooking(id);
    }

    @GetMapping("/referral-link/{referralLinkId}")
    public List<BookingResponse> getBookingsByReferralLink(@PathVariable Long referralLinkId) {
        return bookingService.getBookingsByReferralLink(referralLinkId);
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('HOST') or hasRole('ADMIN')")
    public BookingResponse confirmBooking(@PathVariable Long id) {
        return bookingService.confirmBooking(id);
    }

    @PutMapping("/{id}/finish")
    @PreAuthorize("hasRole('HOST') or hasRole('ADMIN')")
    public BookingResponse finishBooking(@PathVariable Long id) {
        return bookingService.finishBookingAndCreatePayment(id);
    }

    @PutMapping("/{id}/cancel")
    public BookingResponse cancelBooking(
        @PathVariable Long id,
        @RequestParam String reason,
        @RequestParam String canceledBy
    ) {
        return bookingService.cancelBooking(id, reason, canceledBy);
    }

    @GetMapping("/property/{propertyId}/availability")
    public ResponseEntity<Boolean> checkAvailability(
        @PathVariable Long propertyId,
        @RequestParam String startDate,
        @RequestParam String endDate
    ) {
        boolean available = bookingService.checkPropertyAvailability(
            propertyId,
            LocalDate.parse(startDate),
            LocalDate.parse(endDate)
        );
        return ResponseEntity.ok(available);
    }
}
```

---

## Testing Examples

### Unit Test (JUnit 5 + Mockito)

```java
package com.affiliate.rentals.gydi.referrals.application.usecase;

import com.affiliate.rentals.gydi.referrals.domain.model.Booking;
import com.affiliate.rentals.gydi.referrals.domain.model.BookingStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ReferralLinkRepository referralLinkRepository;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void createBooking_Success() {
        // Given
        CreateBookingRequest request = CreateBookingRequest.builder()
            .referralLinkId(1L)
            .propertyId(5L)
            .startDate(LocalDate.now().plusDays(7))
            .endDate(LocalDate.now().plusDays(14))
            .clientEmail("test@example.com")
            .clientFirstName("John")
            .clientLastName("Doe")
            .totalAmount(new BigDecimal("840.00"))
            .currency("USD")
            .build();

        when(referralLinkRepository.findById(1L))
            .thenReturn(Optional.of(new ReferralLink()));
        when(bookingRepository.isPropertyAvailable(any(), any(), any()))
            .thenReturn(true);
        when(bookingRepository.save(any(Booking.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Booking booking = bookingService.createBooking(request);

        // Then
        assertNotNull(booking);
        assertEquals(BookingStatus.REQUEST, booking.getStatus());
        assertEquals(request.getClientEmail(), booking.getClientEmail());
        assertEquals(request.getTotalAmount(), booking.getTotalAmount());

        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void confirmBooking_Success() {
        // Given
        Booking booking = Booking.builder()
            .id(1L)
            .status(BookingStatus.REQUEST)
            .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Booking confirmed = bookingService.confirmBooking(1L);

        // Then
        assertEquals(BookingStatus.RESERVED, confirmed.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void confirmBooking_ThrowsException_WhenNotInRequestStatus() {
        // Given
        Booking booking = Booking.builder()
            .id(1L)
            .status(BookingStatus.FINISHED)
            .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            bookingService.confirmBooking(1L);
        });

        verify(bookingRepository, never()).save(any());
    }
}
```

### Integration Test (TestContainers)

```java
package com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence;

import com.affiliate.rentals.gydi.referrals.domain.model.Booking;
import com.affiliate.rentals.gydi.referrals.domain.model.BookingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("gydi_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void saveBooking_Success() {
        // Given
        Booking booking = Booking.builder()
            .referralLinkId(1L)
            .propertyId(5L)
            .startDate(LocalDate.now().plusDays(7))
            .endDate(LocalDate.now().plusDays(14))
            .clientEmail("test@example.com")
            .clientFirstName("John")
            .clientLastName("Doe")
            .totalAmount(new BigDecimal("840.00"))
            .currency("USD")
            .status(BookingStatus.REQUEST)
            .build();

        // When
        Booking saved = bookingRepository.save(booking);

        // Then
        assertNotNull(saved.getId());
        assertEquals(BookingStatus.REQUEST, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void isPropertyAvailable_ReturnsFalse_WhenOverlappingBookingExists() {
        // Given
        LocalDate start = LocalDate.now().plusDays(7);
        LocalDate end = LocalDate.now().plusDays(14);

        Booking existingBooking = Booking.builder()
            .referralLinkId(1L)
            .propertyId(5L)
            .startDate(start)
            .endDate(end)
            .clientEmail("existing@example.com")
            .clientFirstName("Jane")
            .clientLastName("Smith")
            .totalAmount(new BigDecimal("500.00"))
            .currency("USD")
            .status(BookingStatus.RESERVED)
            .build();

        bookingRepository.save(existingBooking);

        // When - try to book overlapping dates
        LocalDate newStart = start.plusDays(3); // overlaps with existing
        LocalDate newEnd = end.plusDays(3);
        boolean available = bookingRepository.isPropertyAvailable(5L, newStart, newEnd);

        // Then
        assertFalse(available);
    }
}
```

---

## Performance Tips

1. **Use Projections for List Queries**
   ```java
   @Query("SELECT new BookingSummary(b.id, b.clientEmail, b.totalAmount, b.status) FROM Booking b")
   List<BookingSummary> findAllSummaries();
   ```

2. **Eager Loading for Related Data**
   ```java
   @Query("SELECT b FROM Booking b JOIN FETCH b.property WHERE b.id = :id")
   Optional<Booking> findByIdWithProperty(@Param("id") Long id);
   ```

3. **Batch Processing**
   ```java
   List<PaymentBooking> pending = paymentRepository.findPendingPayments(PageRequest.of(0, 100));
   paymentGateway.processBatch(pending);
   ```

---

## Troubleshooting Checklist

- [ ] Did you run all 4 migrations in order?
- [ ] Are foreign key IDs valid before inserting?
- [ ] Is start_date >= today and end_date > start_date?
- [ ] Does user have active subscription when creating payment?
- [ ] Is booking status FINISHED before creating payment?
- [ ] Did you check database constraints (EXPLAIN output)?

---

**Need Full Documentation?** See `/docs/DATABASE_BOOKING_PAYMENT_SCHEMA.md`
