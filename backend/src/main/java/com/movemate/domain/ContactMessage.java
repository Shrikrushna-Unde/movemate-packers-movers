package com.movemate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A "Contact us" form submission.
 *
 * <p>Why store it instead of emailing it? An email can bounce, land in spam or be deleted,
 * and then the enquiry is gone. Persisting first and notifying second means the lead is never
 * lost, the admin has a worklist with a {@code handled} flag, and adding email later is a
 * change in one service method rather than a redesign.
 */
@Entity
@Table(name = "contact_messages")
public class ContactMessage extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 150)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String message;

    /** Flipped by an admin once the enquiry has been answered. */
    @Column(nullable = false)
    private boolean handled = false;

    protected ContactMessage() {
    }

    public ContactMessage(String name, String email, String phone, String subject, String message) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.subject = subject;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isHandled() {
        return handled;
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }
}
