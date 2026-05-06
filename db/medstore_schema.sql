-- ============================================================
--  ███╗   ███╗███████╗██████╗ ███████╗████████╗ ██████╗ ██████╗ ███████╗
--  ████╗ ████║██╔════╝██╔══██╗██╔════╝╚══██╔══╝██╔═══██╗██╔══██╗██╔════╝
--  ██╔████╔██║█████╗  ██║  ██║███████╗   ██║   ██║   ██║██████╔╝█████╗
--  ██║╚██╔╝██║██╔══╝  ██║  ██║╚════██║   ██║   ██║   ██║██╔══██╗██╔══╝
--  ██║ ╚═╝ ██║███████╗██████╔╝███████║   ██║   ╚██████╔╝██║  ██║███████╗
--  ╚═╝     ╚═╝╚══════╝╚═════╝ ╚══════╝   ╚═╝    ╚═════╝ ╚═╝  ╚═╝╚══════╝
--
--  MedStore — Order Module
--  Database : online_medical_store
--  Engine   : MySQL 8.0+
--  Charset  : utf8mb4 / utf8mb4_unicode_ci
--  Author   : MedStore Dev Team
--  Version  : 1.0.0
-- ============================================================


-- ── 0. SAFETY GUARDS ─────────────────────────────────────────────────────────
--  Drop existing tables in reverse dependency order so FK constraints
--  never block the operation. Safe to re-run on a fresh or existing DB.

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS order_id_sequence;

SET FOREIGN_KEY_CHECKS = 1;


-- ── 1. DATABASE ───────────────────────────────────────────────────────────────

CREATE DATABASE IF NOT EXISTS online_medical_store
    CHARACTER SET  utf8mb4
    COLLATE        utf8mb4_unicode_ci;

USE online_medical_store;


-- ── 2. ORDER ID SEQUENCE ──────────────────────────────────────────────────────
--  Mirrors the OrderRepository.generateOrderId() logic.
--  A single-row counter table lets Java atomically pull the next ID
--  without a race condition.

CREATE TABLE order_id_sequence (
    id            TINYINT UNSIGNED NOT NULL DEFAULT 1,   -- always row 1
    next_val      BIGINT  UNSIGNED NOT NULL DEFAULT 1,
    last_updated  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Single-row counter for generating ORD-XXXXXXXX order IDs';

-- Seed the counter
INSERT INTO order_id_sequence (id, next_val) VALUES (1, 1);


-- ── 3. ORDERS ─────────────────────────────────────────────────────────────────
--  Maps directly to com.medstore.order.model.Order
--  All enum values mirror their Java counterpart exactly.

CREATE TABLE orders (

    -- Identity ----------------------------------------------------------------
    order_id         VARCHAR(20)      NOT NULL
                         COMMENT 'Business key, e.g. ORD-00000001',

    customer_id      VARCHAR(50)      NOT NULL
                         COMMENT 'Foreign key to the customer/user table',

    customer_name    VARCHAR(150)     NOT NULL,

    -- Delivery ----------------------------------------------------------------
    delivery_address TEXT             NOT NULL,

    -- Payment (mirrors PaymentMethod enum) ------------------------------------
    payment_method   ENUM(
                         'CASH_ON_DELIVERY',
                         'CREDIT_CARD',
                         'ONLINE_TRANSFER'
                     )                NULL
                         COMMENT 'NULL until payment is attached via Order.applyPayment()',

    -- Extra payment metadata --------------------------------------------------
    --  COD      → extra1 = delivery address copy, extra2 = NULL
    --  Card     → extra1 = cardHolder,            extra2 = masked card number
    --  Transfer → extra1 = bankName,              extra2 = referenceNumber
    payment_extra1   VARCHAR(255)     NULL
                         COMMENT 'COD: address | Card: cardHolder | Transfer: bankName',
    payment_extra2   VARCHAR(100)     NULL
                         COMMENT 'Card: cardNumber (masked) | Transfer: referenceNumber',

    -- Lifecycle (mirrors OrderStatus enum) ------------------------------------
    status           ENUM(
                         'PENDING',
                         'CONFIRMED',
                         'PROCESSING',
                         'SHIPPED',
                         'DELIVERED',
                         'CANCELLED'
                     )                NOT NULL DEFAULT 'PENDING'
                         COMMENT 'Lifecycle state — see OrderStatus.java',

    -- Timestamps --------------------------------------------------------------
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP
                                               ON UPDATE CURRENT_TIMESTAMP,

    -- Constraints -------------------------------------------------------------
    PRIMARY KEY (order_id),

    INDEX idx_orders_customer_id  (customer_id),
    INDEX idx_orders_status       (status),
    INDEX idx_orders_payment      (payment_method),
    INDEX idx_orders_created_at   (created_at DESC)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Core order records — maps to com.medstore.order.model.Order';


-- ── 4. ORDER ITEMS ────────────────────────────────────────────────────────────
--  Maps to com.medstore.order.model.OrderItem
--  line_total is a generated column matching OrderItem.getLineTotal()

CREATE TABLE order_items (

    -- Surrogate PK ------------------------------------------------------------
    id            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,

    -- Parent order ------------------------------------------------------------
    order_id      VARCHAR(20)      NOT NULL,

    -- Medicine line item (mirrors OrderItem fields) ---------------------------
    medicine_id   VARCHAR(50)      NOT NULL
                      COMMENT 'ID of the medicine from the medicine catalogue',

    medicine_name VARCHAR(200)     NOT NULL
                      COMMENT 'Snapshot of the name at time of order',

    quantity      INT              NOT NULL,
    unit_price    DECIMAL(10, 2)   NOT NULL
                      COMMENT 'Price per unit at time of order (snapshot)',

    -- Computed column mirrors OrderItem.getLineTotal() ------------------------
    line_total    DECIMAL(12, 2)   GENERATED ALWAYS AS (quantity * unit_price) STORED
                      COMMENT 'Mirrors OrderItem.getLineTotal() — quantity × unitPrice',

    -- Constraints -------------------------------------------------------------
    PRIMARY KEY (id),

    CONSTRAINT chk_quantity   CHECK (quantity   > 0),
    CONSTRAINT chk_unit_price CHECK (unit_price >= 0),

    CONSTRAINT fk_items_order
        FOREIGN KEY (order_id)
        REFERENCES  orders (order_id)
        ON UPDATE   CASCADE
        ON DELETE   CASCADE,         -- remove items when the parent order is deleted

    INDEX idx_items_order_id    (order_id),
    INDEX idx_items_medicine_id (medicine_id)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Line items — maps to com.medstore.order.model.OrderItem';


-- ── 5. HELPER VIEW  (optional but useful) ────────────────────────────────────
--  Reconstructs the full order summary that OrderService.totalRevenue()
--  and dashboard queries need — handy for ad-hoc reporting.

CREATE OR REPLACE VIEW v_order_summary AS
SELECT
    o.order_id,
    o.customer_id,
    o.customer_name,
    o.delivery_address,
    o.payment_method,
    o.status,
    o.created_at,
    o.updated_at,
    COUNT(i.id)          AS item_count,
    SUM(i.line_total)    AS total_amount
FROM  orders      o
LEFT  JOIN order_items i ON i.order_id = o.order_id
GROUP BY
    o.order_id,
    o.customer_id,
    o.customer_name,
    o.delivery_address,
    o.payment_method,
    o.status,
    o.created_at,
    o.updated_at;


-- ── 6. SEED DATA (development / demo) ────────────────────────────────────────
--  Comment out this entire block before running in production.

INSERT INTO orders
    (order_id, customer_id, customer_name, delivery_address,
     payment_method, payment_extra1, payment_extra2, status,
     created_at, updated_at)
VALUES
    ('ORD-00000001', 'CUST-001', 'Alice Fernando',
     '42 Galle Road, Colombo 03',
     'CREDIT_CARD', 'Alice Fernando', '**** **** **** 4242',
     'CONFIRMED',
     '2025-05-01 09:00:00', '2025-05-01 09:01:00'),

    ('ORD-00000002', 'CUST-002', 'Buddhika Perera',
     '18 Kandy Road, Negombo',
     'CASH_ON_DELIVERY', '18 Kandy Road, Negombo', NULL,
     'SHIPPED',
     '2025-05-02 11:30:00', '2025-05-03 08:00:00'),

    ('ORD-00000003', 'CUST-003', 'Chathuri Silva',
     '7 Temple Street, Kandy',
     'ONLINE_TRANSFER', 'Bank of Ceylon', 'REF-20250503-77821',
     'PENDING',
     '2025-05-03 14:15:00', '2025-05-03 14:15:00');

INSERT INTO order_items
    (order_id, medicine_id, medicine_name, quantity, unit_price)
VALUES
    -- ORD-00000001
    ('ORD-00000001', 'MED-101', 'Paracetamol 500mg',  3,  45.00),
    ('ORD-00000001', 'MED-203', 'Amoxicillin 250mg',  2, 185.00),

    -- ORD-00000002
    ('ORD-00000002', 'MED-310', 'Cetirizine 10mg',    1,  75.50),
    ('ORD-00000002', 'MED-101', 'Paracetamol 500mg',  5,  45.00),

    -- ORD-00000003
    ('ORD-00000003', 'MED-512', 'Metformin 500mg',    2, 220.00),
    ('ORD-00000003', 'MED-618', 'Atorvastatin 10mg',  1, 310.00);

-- Update the sequence counter past the seeded IDs
UPDATE order_id_sequence SET next_val = 4 WHERE id = 1;


-- ── 7. VERIFICATION QUERIES ───────────────────────────────────────────────────
--  Run these after import to confirm everything is wired correctly.

-- SELECT * FROM orders;
-- SELECT * FROM order_items;
-- SELECT * FROM v_order_summary;
-- SELECT * FROM order_id_sequence;

-- ── END OF SCHEMA ─────────────────────────────────────────────────────────────
