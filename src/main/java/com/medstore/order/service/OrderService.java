package com.medstore.order.service;

import com.medstore.order.exception.OrderNotFoundException;
import com.medstore.order.model.*;
import com.medstore.order.model.payment.Payment;
import com.medstore.order.model.payment.PaymentFactory;
import com.medstore.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Service layer — pure business logic, no HTTP or persistence concerns.
 * Uses PaymentFactory for polymorphic payment creation.
 */
@Service
public class OrderService {

    private final OrderRepository repo;

    public OrderService(OrderRepository repo) {
        this.repo = repo;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    /**
     * Places a new order and processes payment in a single transaction.
     *
     * @param customerId      customer placing the order
     * @param customerName    display name
     * @param deliveryAddress shipping address
     * @param items           line items in the basket
     * @param paymentMethod   chosen payment method
     * @param extra1          COD: address | Card: cardHolder | Transfer: bankName
     * @param extra2          Card: cardNumber | Transfer: referenceNumber
     * @return the saved Order
     */
    public Order placeOrder(String customerId,
                            String customerName,
                            String deliveryAddress,
                            List<OrderItem> items,
                            PaymentMethod paymentMethod,
                            String extra1,
                            String extra2) throws IOException {

        String orderId = repo.generateOrderId();
        Order order = new Order(orderId, customerId, customerName, deliveryAddress);
        order.setItems(items);

        // Polymorphic payment creation via factory
        Payment payment = PaymentFactory.create(paymentMethod, order.getTotalAmount(), extra1, extra2);
        order.applyPayment(payment); // processes payment and updates status

        return repo.save(order);
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    public List<Order> getAllOrders() throws IOException {
        return repo.findAll();
    }

    public Order getOrderById(String orderId) throws IOException {
        return repo.findById(orderId)
                   .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public List<Order> getOrdersByCustomer(String customerId) throws IOException {
        return repo.findByCustomerId(customerId);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    public Order updateOrderStatus(String orderId, OrderStatus newStatus) throws IOException {
        Order order = getOrderById(orderId);
        order.updateStatus(newStatus);
        return repo.update(order);
    }

    // ── DELETE / CANCEL ───────────────────────────────────────────────────────

    /**
     * Cancels an order. Only PENDING or CONFIRMED orders can be cancelled.
     */
    public Order cancelOrder(String orderId) throws IOException {
        Order order = getOrderById(orderId);
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel an order that is already " + order.getStatus().getLabel());
        }
        order.updateStatus(OrderStatus.CANCELLED);
        return repo.update(order);
    }

    /**
     * Hard-delete an order record (Admin only).
     */
    public boolean deleteOrder(String orderId) throws IOException {
        if (!repo.findById(orderId).isPresent()) {
            throw new OrderNotFoundException(orderId);
        }
        return repo.delete(orderId);
    }

    // ── Statistics ────────────────────────────────────────────────────────────

    public long countByStatus(OrderStatus status) throws IOException {
        return repo.findAll().stream()
                   .filter(o -> o.getStatus() == status)
                   .count();
    }

    public double totalRevenue() throws IOException {
        return repo.findAll().stream()
                   .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                   .mapToDouble(Order::getTotalAmount)
                   .sum();
    }
}
