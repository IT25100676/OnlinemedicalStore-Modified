package com.medstore.order.repository;

import com.medstore.order.exception.OrderNotFoundException;
import com.medstore.order.model.Order;
import com.medstore.order.model.OrderItem;
import com.medstore.order.model.OrderStatus;
import com.medstore.order.model.PaymentMethod;
import com.medstore.order.model.payment.CardPayment;
import com.medstore.order.model.payment.CashOnDeliveryPayment;
import com.medstore.order.model.payment.OnlineTransferPayment;
import com.medstore.order.model.payment.Payment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * MySQL persistence layer — replaces the flat-file OrderRepository.
 *
 * Maps to the `orders` and `order_items` tables defined in medstore_schema.sql.
 * Uses Spring JdbcTemplate (spring-boot-starter-jdbc + HikariCP pool).
 *
 * payment_extra1 / payment_extra2 mapping (matches schema comments exactly):
 *   CASH_ON_DELIVERY  → extra1 = deliveryAddress,   extra2 = null
 *   CREDIT_CARD       → extra1 = cardHolderName,    extra2 = maskedCardNumber
 *   ONLINE_TRANSFER   → extra1 = bankName,          extra2 = referenceNumber
 */
@Repository
public class OrderRepository {

    private final JdbcTemplate jdbc;

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    /**
     * Inserts the order header row and all its line items atomically.
     * If any item insert fails the entire transaction rolls back.
     */
    @Transactional
    public Order save(Order order) {

        jdbc.update("""
                INSERT INTO orders
                    (order_id, customer_id, customer_name, delivery_address,
                     payment_method, payment_extra1, payment_extra2,
                     status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                order.getOrderId(),
                order.getCustomerId(),
                order.getCustomerName(),
                order.getDeliveryAddress(),
                order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null,
                resolveExtra1(order),
                resolveExtra2(order),
                order.getStatus().name(),
                Timestamp.valueOf(order.getCreatedAt()),
                Timestamp.valueOf(order.getUpdatedAt())
        );

        insertItems(order);
        return order;
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    public List<Order> findAll() {
        List<Order> orders = jdbc.query(
                "SELECT * FROM orders ORDER BY created_at DESC",
                new OrderRowMapper()
        );
        orders.forEach(o -> o.setItems(fetchItems(o.getOrderId())));
        return orders;
    }

    public Optional<Order> findById(String orderId) {
        List<Order> results = jdbc.query(
                "SELECT * FROM orders WHERE order_id = ?",
                new OrderRowMapper(),
                orderId
        );
        if (results.isEmpty()) return Optional.empty();
        Order order = results.get(0);
        order.setItems(fetchItems(orderId));
        return Optional.of(order);
    }

    public List<Order> findByCustomerId(String customerId) {
        List<Order> orders = jdbc.query(
                "SELECT * FROM orders WHERE customer_id = ? ORDER BY created_at DESC",
                new OrderRowMapper(),
                customerId
        );
        orders.forEach(o -> o.setItems(fetchItems(o.getOrderId())));
        return orders;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /**
     * Updates the order header and replaces all line items.
     * Throws OrderNotFoundException if the order_id does not exist.
     */
    @Transactional
    public Order update(Order updated) {

        int affected = jdbc.update("""
                UPDATE orders
                SET    customer_name    = ?,
                       delivery_address = ?,
                       payment_method   = ?,
                       payment_extra1   = ?,
                       payment_extra2   = ?,
                       status           = ?,
                       updated_at       = ?
                WHERE  order_id = ?
                """,
                updated.getCustomerName(),
                updated.getDeliveryAddress(),
                updated.getPaymentMethod() != null ? updated.getPaymentMethod().name() : null,
                resolveExtra1(updated),
                resolveExtra2(updated),
                updated.getStatus().name(),
                Timestamp.valueOf(updated.getUpdatedAt()),
                updated.getOrderId()
        );

        if (affected == 0) {
            throw new OrderNotFoundException(updated.getOrderId());
        }

        // Replace line items atomically within the same transaction
        jdbc.update("DELETE FROM order_items WHERE order_id = ?", updated.getOrderId());
        insertItems(updated);

        return updated;
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /**
     * Hard-deletes an order by ID.
     * The ON DELETE CASCADE constraint in the schema removes order_items automatically.
     */
    @Transactional
    public boolean delete(String orderId) {
        int affected = jdbc.update("DELETE FROM orders WHERE order_id = ?", orderId);
        return affected > 0;
    }

    // ── ORDER ID GENERATION ───────────────────────────────────────────────────

    /**
     * Atomically reads and increments the order_id_sequence counter.
     * FOR UPDATE locks the row so two simultaneous requests never get the same ID.
     * Produces IDs in the format: ORD-00000001, ORD-00000002, …
     * Matches the seed data format in medstore_schema.sql.
     */
    @Transactional
    public String generateOrderId() {
        Long nextVal = jdbc.queryForObject(
                "SELECT next_val FROM order_id_sequence WHERE id = 1 FOR UPDATE",
                Long.class
        );
        if (nextVal == null) nextVal = 1L;

        jdbc.update(
                "UPDATE order_id_sequence SET next_val = ? WHERE id = 1",
                nextVal + 1
        );

        return String.format("ORD-%08d", nextVal);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Extracts payment_extra1 by casting to the correct Payment subclass.
     *
     *   CashOnDeliveryPayment → getDeliveryAddress()
     *   CardPayment           → getCardHolderName()
     *   OnlineTransferPayment → getBankName()
     */
    private String resolveExtra1(Order order) {
        Payment payment = order.getPayment();
        if (payment == null || order.getPaymentMethod() == null) return null;

        return switch (order.getPaymentMethod()) {
            case CASH_ON_DELIVERY ->
                    ((CashOnDeliveryPayment) payment).getDeliveryAddress();
            case CREDIT_CARD ->
                    ((CardPayment) payment).getCardHolderName();
            case ONLINE_TRANSFER ->
                    ((OnlineTransferPayment) payment).getBankName();
        };
    }

    /**
     * Extracts payment_extra2 by casting to the correct Payment subclass.
     *
     *   CashOnDeliveryPayment → null
     *   CardPayment           → getMaskedCardNumber()  e.g. "**** **** **** 4242"
     *   OnlineTransferPayment → getReferenceNumber()
     */
    private String resolveExtra2(Order order) {
        Payment payment = order.getPayment();
        if (payment == null || order.getPaymentMethod() == null) return null;

        return switch (order.getPaymentMethod()) {
            case CASH_ON_DELIVERY -> null;
            case CREDIT_CARD      -> ((CardPayment) payment).getMaskedCardNumber();
            case ONLINE_TRANSFER  -> ((OnlineTransferPayment) payment).getReferenceNumber();
        };
    }

    /**
     * Inserts all line items for an order into the order_items table.
     * line_total is a generated column in MySQL — we never insert it directly.
     */
    private void insertItems(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) return;

        for (OrderItem item : order.getItems()) {
            jdbc.update("""
                    INSERT INTO order_items
                        (order_id, medicine_id, medicine_name, quantity, unit_price)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    order.getOrderId(),
                    item.getMedicineId(),
                    item.getMedicineName(),
                    item.getQuantity(),
                    item.getUnitPrice()
            );
        }
    }

    /**
     * Fetches all line items for a given order_id, ordered by insertion sequence.
     * Called after every findAll / findById to hydrate the Order.items list.
     */
    private List<OrderItem> fetchItems(String orderId) {
        return jdbc.query(
                "SELECT * FROM order_items WHERE order_id = ? ORDER BY id",
                (rs, rowNum) -> {
                    OrderItem item = new OrderItem();
                    item.setMedicineId(rs.getString("medicine_id"));
                    item.setMedicineName(rs.getString("medicine_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getDouble("unit_price"));
                    return item;
                },
                orderId
        );
    }

    // ── RowMapper ─────────────────────────────────────────────────────────────

    /**
     * Maps one row from the `orders` table to an Order domain object.
     *
     * Note: items are NOT loaded here. They're fetched in a separate query
     * after the list is retrieved to keep the row count predictable.
     */
    private static class OrderRowMapper implements RowMapper<Order> {

        @Override
        public Order mapRow(ResultSet rs, int rowNum) throws SQLException {
            Order order = new Order();

            order.setOrderId(rs.getString("order_id"));
            order.setCustomerId(rs.getString("customer_id"));
            order.setCustomerName(rs.getString("customer_name"));
            order.setDeliveryAddress(rs.getString("delivery_address"));

            // payment_method is nullable until applyPayment() is called
            String pm = rs.getString("payment_method");
            if (pm != null) {
                order.setPaymentMethod(PaymentMethod.valueOf(pm));
            }

            order.setStatus(OrderStatus.valueOf(rs.getString("status")));

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) order.setCreatedAt(createdAt.toLocalDateTime());

            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) order.setUpdatedAt(updatedAt.toLocalDateTime());

            return order;
        }
    }
}