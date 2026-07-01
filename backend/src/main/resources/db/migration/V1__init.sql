-- ============================================================
-- V1: Core schema for Courier Booking System
-- ============================================================

-- ---------- Permissions ----------
CREATE TABLE permissions (
    id          BIGSERIAL PRIMARY KEY,
    module      VARCHAR(30)  NOT NULL,
    action      VARCHAR(30)  NOT NULL,
    code        VARCHAR(80)  NOT NULL,
    description VARCHAR(255),
    CONSTRAINT uk_permission_code UNIQUE (code),
    CONSTRAINT uk_permission_module_action UNIQUE (module, action)
);

-- ---------- Roles ----------
CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(60)  NOT NULL,
    description VARCHAR(255),
    system_role BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP,
    created_by  VARCHAR(100),
    updated_at  TIMESTAMP,
    updated_by  VARCHAR(100),
    CONSTRAINT uk_role_name UNIQUE (name)
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_perm FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

-- ---------- Users ----------
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(60)  NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    phone         VARCHAR(30),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP,
    created_by    VARCHAR(100),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR(100),
    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT uk_user_email UNIQUE (email)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE TABLE user_permissions (
    user_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, permission_id),
    CONSTRAINT fk_up_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_up_perm FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

-- ---------- Approval routing ----------
CREATE TABLE approval_routing (
    id         BIGSERIAL PRIMARY KEY,
    role_id    BIGINT,
    user_id    BIGINT,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    CONSTRAINT fk_ar_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_ar_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- ---------- Parties (master address book) ----------
CREATE TABLE parties (
    id            BIGSERIAL PRIMARY KEY,
    party_code    VARCHAR(30)  NOT NULL,
    party_name    VARCHAR(150) NOT NULL,
    address_line1 VARCHAR(200) NOT NULL,
    address_line2 VARCHAR(200),
    city          VARCHAR(100) NOT NULL,
    state         VARCHAR(100) NOT NULL,
    pincode       VARCHAR(20)  NOT NULL,
    country       VARCHAR(100) NOT NULL,
    phone         VARCHAR(30),
    email         VARCHAR(150),
    gstin         VARCHAR(20),
    party_type    VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP,
    created_by    VARCHAR(100),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR(100),
    CONSTRAINT uk_party_code UNIQUE (party_code)
);

CREATE INDEX idx_party_name ON parties (party_name);
CREATE INDEX idx_party_city ON parties (city);
CREATE INDEX idx_party_pincode ON parties (pincode);

-- ---------- Booking number sequence ----------
CREATE TABLE booking_sequence (
    seq_date   VARCHAR(8) PRIMARY KEY,
    last_value BIGINT NOT NULL
);

-- ---------- Bookings ----------
CREATE TABLE bookings (
    id                   BIGSERIAL PRIMARY KEY,
    booking_number       VARCHAR(40)  NOT NULL,
    booking_date         DATE         NOT NULL,
    sender_id            BIGINT       NOT NULL,
    receiver_id          BIGINT       NOT NULL,
    item_description     VARCHAR(500) NOT NULL,
    weight_kg            NUMERIC(10,3) NOT NULL,
    no_of_packages       INTEGER      NOT NULL,
    courier_mode         VARCHAR(20)  NOT NULL,
    declared_value       NUMERIC(14,2),
    freight_charges      NUMERIC(14,2) NOT NULL,
    total_charges        NUMERIC(14,2) NOT NULL,
    payment_mode         VARCHAR(20)  NOT NULL,
    special_instructions VARCHAR(1000),
    status               VARCHAR(30)  NOT NULL,
    approver_username    VARCHAR(60),
    approval_timestamp   TIMESTAMP,
    approval_remarks     VARCHAR(500),
    created_at           TIMESTAMP,
    created_by           VARCHAR(100),
    updated_at           TIMESTAMP,
    updated_by           VARCHAR(100),
    CONSTRAINT uk_booking_number UNIQUE (booking_number),
    CONSTRAINT fk_booking_sender FOREIGN KEY (sender_id) REFERENCES parties (id),
    CONSTRAINT fk_booking_receiver FOREIGN KEY (receiver_id) REFERENCES parties (id)
);

CREATE INDEX idx_booking_date ON bookings (booking_date);
CREATE INDEX idx_booking_status ON bookings (status);
CREATE INDEX idx_booking_mode ON bookings (courier_mode);
CREATE INDEX idx_booking_sender ON bookings (sender_id);
CREATE INDEX idx_booking_receiver ON bookings (receiver_id);
