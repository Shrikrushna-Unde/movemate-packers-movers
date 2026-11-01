-- ===================================================================
--  V1__init_schema.sql
--
--  Flyway naming contract:  V<version>__<description>.sql  (two underscores)
--  Flyway runs these files in version order exactly once per database and
--  records each one in a table called `flyway_schema_history`. That gives you
--  a reviewable, repeatable schema - the same thing Liquibase does, and the
--  reason we set spring.jpa.hibernate.ddl-auto=none.
--
--  Rule to remember: a migration that has already run is immutable.
--  To change the schema later you add V3__..., you never edit V1.
-- ===================================================================

-- -------------------------------------------------------------------
--  users - customers and admins in one table, separated by `role`
-- -------------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL,
    phone         VARCHAR(20),
    -- Column is named password_hash, not password, so nobody is ever tempted
    -- to store plaintext here. BCrypt output is always 60 chars.
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'ROLE_CUSTOMER',
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6),
    PRIMARY KEY (id),
    -- A UNIQUE constraint is the only reliable way to stop duplicate signups.
    -- An application-level "select then insert" check loses the race under
    -- concurrent requests; the database never does.
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
--  moving_services - the service catalogue shown on the website
-- -------------------------------------------------------------------
CREATE TABLE moving_services (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    -- slug is the URL-safe id used by the frontend: /services/home-shifting
    slug              VARCHAR(80)   NOT NULL,
    title             VARCHAR(120)  NOT NULL,
    short_description VARCHAR(255)  NOT NULL,
    description       VARCHAR(1500) NOT NULL,
    icon              VARCHAR(40)   NOT NULL,
    starting_price    DECIMAL(10, 2) NOT NULL,
    display_order     INT           NOT NULL DEFAULT 0,
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_moving_services_slug UNIQUE (slug)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
--  bookings - the core business table
-- -------------------------------------------------------------------
CREATE TABLE bookings (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    -- Human-friendly id we show the customer (MM-2026-4F7K2Q). Never expose the
    -- numeric primary key in a URL a stranger could increment.
    reference          VARCHAR(24)   NOT NULL,
    -- NULL on purpose: guests can book without creating an account.
    user_id            BIGINT,
    customer_name      VARCHAR(120)  NOT NULL,
    customer_email     VARCHAR(180)  NOT NULL,
    customer_phone     VARCHAR(20)   NOT NULL,
    move_type          VARCHAR(40)   NOT NULL,
    home_size          VARCHAR(20),
    pickup_address     VARCHAR(255)  NOT NULL,
    pickup_city        VARCHAR(80)   NOT NULL,
    pickup_pincode     VARCHAR(10),
    drop_address       VARCHAR(255)  NOT NULL,
    drop_city          VARCHAR(80)   NOT NULL,
    drop_pincode       VARCHAR(10),
    distance_km        INT           NOT NULL DEFAULT 0,
    move_date          DATE          NOT NULL,
    pickup_floor       INT           NOT NULL DEFAULT 0,
    drop_floor         INT           NOT NULL DEFAULT 0,
    lift_available     BOOLEAN       NOT NULL DEFAULT FALSE,
    packing_required   BOOLEAN       NOT NULL DEFAULT TRUE,
    insurance_required BOOLEAN       NOT NULL DEFAULT FALSE,
    declared_value     DECIMAL(12, 2),
    storage_days       INT           NOT NULL DEFAULT 0,
    -- DECIMAL, never DOUBLE, for money. Binary floating point cannot represent
    -- 0.1 exactly, so sums drift. This is a standard interview question.
    estimated_amount   DECIMAL(12, 2) NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    customer_notes     VARCHAR(500),
    admin_notes        VARCHAR(500),
    created_at         DATETIME(6)   NOT NULL,
    updated_at         DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_bookings_reference UNIQUE (reference),
    -- ON DELETE SET NULL: deleting a user must not delete the company's
    -- order history. The booking survives, it just loses its owner link.
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Indexes follow the WHERE clauses we actually run. The admin list filters by
-- status and sorts by created_at; tracking looks up by phone + reference.
CREATE INDEX idx_bookings_status ON bookings (status);
CREATE INDEX idx_bookings_created_at ON bookings (created_at);
CREATE INDEX idx_bookings_customer_email ON bookings (customer_email);

-- -------------------------------------------------------------------
--  testimonials - customer reviews, admin-moderated before going live
-- -------------------------------------------------------------------
CREATE TABLE testimonials (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT,
    author_name VARCHAR(120) NOT NULL,
    city        VARCHAR(80)  NOT NULL,
    rating      INT          NOT NULL,
    message     VARCHAR(600) NOT NULL,
    -- Defaults to FALSE so nothing a stranger types appears on the homepage
    -- until a human approves it.
    approved    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_testimonials_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    -- A CHECK constraint keeps bad data out even if a future bug skips validation.
    -- Defence in depth: validate in the DTO *and* in the schema.
    CONSTRAINT ck_testimonials_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_testimonials_approved ON testimonials (approved);

-- -------------------------------------------------------------------
--  contact_messages - "Contact us" form submissions
-- -------------------------------------------------------------------
CREATE TABLE contact_messages (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    name       VARCHAR(120)  NOT NULL,
    email      VARCHAR(180)  NOT NULL,
    phone      VARCHAR(20),
    subject    VARCHAR(150)  NOT NULL,
    message    VARCHAR(2000) NOT NULL,
    handled    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)   NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_contact_messages_handled ON contact_messages (handled);
