package com.movemate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * One row in the service catalogue (Home Shifting, Office Relocation, ...).
 *
 * <p>Why a database table instead of hardcoding six cards in React? Because the content is
 * business data, not layout. The admin can retitle a service or change a starting price
 * without a frontend redeploy, and the "starting from" prices stay consistent between the
 * homepage, the services page and marketing copy.
 *
 * <p>Named {@code MovingService} rather than {@code Service} on purpose: {@code Service}
 * would collide with Spring's {@code @Service} annotation in imports and confuse every
 * reader of the code.
 */
@Entity
@Table(name = "moving_services")
public class MovingService extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** URL-safe identifier, e.g. "home-shifting" -> /services/home-shifting. */
    @Column(nullable = false, length = 80, unique = true)
    private String slug;

    @Column(nullable = false, length = 120)
    private String title;

    /** One-liner for cards and meta descriptions. */
    @Column(name = "short_description", nullable = false, length = 255)
    private String shortDescription;

    /** Full copy for the detail page. */
    @Column(nullable = false, length = 1500)
    private String description;

    /** Icon key the React side maps to a Lucide icon component. The API stays framework-agnostic. */
    @Column(nullable = false, length = 40)
    private String icon;

    /** "Starting from" price, in rupees. DECIMAL(10,2) in the schema. */
    @Column(name = "starting_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal startingPrice;

    /** Manual sort position, so marketing can promote a service without renaming it. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** Soft delete. Hiding a service must not orphan the bookings that reference it. */
    @Column(nullable = false)
    private boolean active = true;

    /**
     * No-arg constructor.
     *
     * <p>JPA requires one (it instantiates entities reflectively). It is public here, unlike on
     * {@link User}, because the admin create-service flow legitimately builds a blank catalogue
     * entry and then fills it in from a validated DTO. There are no invariants to protect - the
     * NOT NULL columns are the backstop.
     */
    public MovingService() {
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
