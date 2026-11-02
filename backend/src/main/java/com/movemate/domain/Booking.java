package com.movemate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A move request. This is the heart of the application.
 *
 * <p>Points to defend in an interview:
 * <ul>
 *   <li><b>{@code FetchType.LAZY} on the user association.</b> The JPA default for
 *       {@code @ManyToOne} is EAGER, which means every single booking query silently joins
 *       (or issues a second SELECT on) the users table even when nobody reads the user.
 *       On an admin list of 20 bookings that is 21 queries - the classic N+1 problem.
 *       Always mark {@code @ManyToOne} LAZY and fetch deliberately with a JOIN FETCH query
 *       when you need it.</li>
 *   <li><b>Customer details are copied onto the booking</b> rather than read through the user
 *       link. The phone number used for one move must stay attached to THAT move after the
 *       customer later edits their profile, and the details must survive the account being
 *       deleted entirely. This is a snapshot, not a normalisation mistake.</li>
 *   <li><b>{@code reference} is the public identifier.</b> Exposing {@code /bookings/1} lets
 *       anyone enumerate your customer base by incrementing the number.</li>
 *   <li><b>{@code estimatedAmount} is stored,</b> not recomputed on read. If the price table
 *       changes next month, the customer must still see the number they agreed to.</li>
 * </ul>
 */
@Entity
@Table(name = "bookings")
public class Booking extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public, human-readable id such as MM-2026-4F7K2Q. Unique-constrained in the schema. */
    @Column(nullable = false, length = 24, unique = true)
    private String reference;

    /**
     * Owning account. Every new booking has one, because the create endpoint is authenticated.
     * The column is nullable only so that deleting a customer blanks this link rather than
     * destroying their move history.
     * {@code @JoinColumn(name = "user_id")} names the FK column so Hibernate does not invent
     * one; the FK itself is declared in the migration with ON DELETE SET NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 180)
    private String customerEmail;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "move_type", nullable = false, length = 40)
    private MoveType moveType;

    /** Null for office, vehicle and storage-only jobs. */
    @Enumerated(EnumType.STRING)
    @Column(name = "home_size", length = 20)
    private HomeSize homeSize;

    @Column(name = "pickup_address", nullable = false, length = 255)
    private String pickupAddress;

    @Column(name = "pickup_city", nullable = false, length = 80)
    private String pickupCity;

    @Column(name = "pickup_pincode", length = 10)
    private String pickupPincode;

    @Column(name = "drop_address", nullable = false, length = 255)
    private String dropAddress;

    @Column(name = "drop_city", nullable = false, length = 80)
    private String dropCity;

    @Column(name = "drop_pincode", length = 10)
    private String dropPincode;

    /** Road distance in km, supplied by the customer (a real product would call a maps API). */
    @Column(name = "distance_km", nullable = false)
    private int distanceKm;

    /** {@link LocalDate}, not Instant: a move date is a calendar day, with no time or zone. */
    @Column(name = "move_date", nullable = false)
    private LocalDate moveDate;

    @Column(name = "pickup_floor", nullable = false)
    private int pickupFloor;

    @Column(name = "drop_floor", nullable = false)
    private int dropFloor;

    @Column(name = "lift_available", nullable = false)
    private boolean liftAvailable;

    @Column(name = "packing_required", nullable = false)
    private boolean packingRequired = true;

    @Column(name = "insurance_required", nullable = false)
    private boolean insuranceRequired;

    /** Customer's declared value of goods; the insurance premium is a percentage of it. */
    @Column(name = "declared_value", precision = 12, scale = 2)
    private BigDecimal declaredValue;

    @Column(name = "storage_days", nullable = false)
    private int storageDays;

    /** Quoted total including GST, frozen at booking time. */
    @Column(name = "estimated_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "customer_notes", length = 500)
    private String customerNotes;

    /** Internal remarks - crew assigned, vehicle number, reschedule reason. Admin-only. */
    @Column(name = "admin_notes", length = 500)
    private String adminNotes;

    /**
     * No-arg constructor.
     *
     * <p>JPA requires one. It is public here, unlike on {@link User}, because
     * {@code BookingService} (a different package) builds a booking field by field from a
     * validated DTO. A nineteen-argument constructor would be unreadable at the call site and
     * impossible to get right without an IDE, and `protected` is only visible within this
     * package or to subclasses - so it would not compile from the service layer.
     *
     * <p>The invariant that actually matters here is the status workflow, and that is protected
     * by {@link #changeStatus} rather than by the constructor.
     */
    public Booking() {
    }

    /**
     * Applies a status change, enforcing the state machine in {@link BookingStatus}.
     *
     * <p>The rule is enforced here, on the entity, rather than in the service layer. That is
     * the difference between a "rich domain model" and an anaemic one: the object protects its
     * own invariants, so no future controller, scheduled job or test can put it in an illegal
     * state. The caller gets an exception, which the global handler turns into HTTP 400.
     *
     * @throws IllegalStateException when the transition is not allowed
     */
    public void changeStatus(BookingStatus next) {
        if (this.status == next) {
            return; // Idempotent: re-sending the same status is not an error.
        }
        if (!this.status.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "Cannot change booking " + reference + " from " + status + " to " + next);
        }
        this.status = next;
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public MoveType getMoveType() {
        return moveType;
    }

    public void setMoveType(MoveType moveType) {
        this.moveType = moveType;
    }

    public HomeSize getHomeSize() {
        return homeSize;
    }

    public void setHomeSize(HomeSize homeSize) {
        this.homeSize = homeSize;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getPickupCity() {
        return pickupCity;
    }

    public void setPickupCity(String pickupCity) {
        this.pickupCity = pickupCity;
    }

    public String getPickupPincode() {
        return pickupPincode;
    }

    public void setPickupPincode(String pickupPincode) {
        this.pickupPincode = pickupPincode;
    }

    public String getDropAddress() {
        return dropAddress;
    }

    public void setDropAddress(String dropAddress) {
        this.dropAddress = dropAddress;
    }

    public String getDropCity() {
        return dropCity;
    }

    public void setDropCity(String dropCity) {
        this.dropCity = dropCity;
    }

    public String getDropPincode() {
        return dropPincode;
    }

    public void setDropPincode(String dropPincode) {
        this.dropPincode = dropPincode;
    }

    public int getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(int distanceKm) {
        this.distanceKm = distanceKm;
    }

    public LocalDate getMoveDate() {
        return moveDate;
    }

    public void setMoveDate(LocalDate moveDate) {
        this.moveDate = moveDate;
    }

    public int getPickupFloor() {
        return pickupFloor;
    }

    public void setPickupFloor(int pickupFloor) {
        this.pickupFloor = pickupFloor;
    }

    public int getDropFloor() {
        return dropFloor;
    }

    public void setDropFloor(int dropFloor) {
        this.dropFloor = dropFloor;
    }

    public boolean isLiftAvailable() {
        return liftAvailable;
    }

    public void setLiftAvailable(boolean liftAvailable) {
        this.liftAvailable = liftAvailable;
    }

    public boolean isPackingRequired() {
        return packingRequired;
    }

    public void setPackingRequired(boolean packingRequired) {
        this.packingRequired = packingRequired;
    }

    public boolean isInsuranceRequired() {
        return insuranceRequired;
    }

    public void setInsuranceRequired(boolean insuranceRequired) {
        this.insuranceRequired = insuranceRequired;
    }

    public BigDecimal getDeclaredValue() {
        return declaredValue;
    }

    public void setDeclaredValue(BigDecimal declaredValue) {
        this.declaredValue = declaredValue;
    }

    public int getStorageDays() {
        return storageDays;
    }

    public void setStorageDays(int storageDays) {
        this.storageDays = storageDays;
    }

    public BigDecimal getEstimatedAmount() {
        return estimatedAmount;
    }

    public void setEstimatedAmount(BigDecimal estimatedAmount) {
        this.estimatedAmount = estimatedAmount;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
}
