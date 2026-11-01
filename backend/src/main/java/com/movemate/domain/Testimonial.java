package com.movemate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A customer review.
 *
 * <p>{@code approved} defaults to false, so a submitted review is invisible until an admin
 * approves it. Any public write endpoint that renders content back to other visitors needs a
 * moderation gate - without it the homepage is one POST away from spam or abuse.
 */
@Entity
@Table(name = "testimonials")
public class Testimonial extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Who submitted it, when it came from a logged-in customer. Null for seeded reviews. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Snapshot of the display name, so the review keeps its byline if the profile changes. */
    @Column(name = "author_name", nullable = false, length = 120)
    private String authorName;

    @Column(nullable = false, length = 80)
    private String city;

    /** 1-5. Validated in the DTO and again by a CHECK constraint in the schema. */
    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 600)
    private String message;

    @Column(nullable = false)
    private boolean approved = false;

    protected Testimonial() {
    }

    public Testimonial(User user, String authorName, String city, int rating, String message) {
        this.user = user;
        this.authorName = authorName;
        this.city = city;
        this.rating = rating;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }
}
