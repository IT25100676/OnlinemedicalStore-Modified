package com.medstore.order.controller;

import com.medstore.order.model.*;
import com.medstore.order.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;

/**
 * MVC Controller — the C in MVC.
 * Handles HTTP requests, coordinates with OrderService (Model),
 * and returns Thymeleaf template names (View).
 */
@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ── LIST ──────────────────────────────────────────────────────────────────

    @GetMapping
    public String listOrders(Model model) throws IOException {
        model.addAttribute("orders", orderService.getAllOrders());
        return "order/list";
    }

    // ── DASHBOARD ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) throws IOException {
        model.addAttribute("orders", orderService.getAllOrders());
        model.addAttribute("totalRevenue", orderService.totalRevenue());
        model.addAttribute("pendingCount", orderService.countByStatus(OrderStatus.PENDING));
        model.addAttribute("confirmedCount", orderService.countByStatus(OrderStatus.CONFIRMED));
        model.addAttribute("shippedCount", orderService.countByStatus(OrderStatus.SHIPPED));
        model.addAttribute("deliveredCount", orderService.countByStatus(OrderStatus.DELIVERED));
        model.addAttribute("cancelledCount", orderService.countByStatus(OrderStatus.CANCELLED));
        return "order/dashboard";
    }

    // ── PLACE ORDER ───────────────────────────────────────────────────────────

    @GetMapping("/place")
    public String placeOrderForm(Model model) {
        model.addAttribute("form", new PlaceOrderForm());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "order/place";
    }

    @PostMapping("/place")
    public String submitOrder(@ModelAttribute("form") PlaceOrderForm form,
                              Model model,
                              RedirectAttributes ra) throws IOException {

        List<String> errors = form.validate();

        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("paymentMethods", PaymentMethod.values());
            return "order/place";
        }

        // Parse line items from the hidden field
        List<OrderItem> items = parseItems(form.getItemsRaw());

        if (items.isEmpty()) {
            model.addAttribute("itemError", "Please add at least one medicine to your order.");
            model.addAttribute("paymentMethods", PaymentMethod.values());
            return "order/place";
        }

        // Determine extra payment parameters
        String extra1 = null;
        String extra2 = null;

        if (form.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY) {
            extra1 = form.getDeliveryAddress();
            extra2 = null;
        } else if (form.getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
            extra1 = form.getCardHolder();
            extra2 = form.getCardNumber();
        } else if (form.getPaymentMethod() == PaymentMethod.ONLINE_TRANSFER) {
            extra1 = form.getBankName();
            extra2 = form.getReferenceNumber();
        }

        Order order = orderService.placeOrder(
                form.getCustomerId(),
                form.getCustomerName(),
                form.getDeliveryAddress(),
                items,
                form.getPaymentMethod(),
                extra1,
                extra2
        );

        ra.addFlashAttribute("successMsg", "Order " + order.getOrderId() + " placed successfully!");

        return "redirect:/orders/" + order.getOrderId();
    }

    // ── DETAIL / TRACKING ─────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable String id, Model model) throws IOException {
        model.addAttribute("order", orderService.getOrderById(id));
        model.addAttribute("allStatuses", OrderStatus.values());
        return "order/detail";
    }

    // ── UPDATE STATUS ─────────────────────────────────────────────────────────

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable String id,
                               @RequestParam OrderStatus newStatus,
                               RedirectAttributes ra) throws IOException {

        orderService.updateOrderStatus(id, newStatus);
        ra.addFlashAttribute("successMsg", "Order status updated to " + newStatus.getLabel());

        return "redirect:/orders/" + id;
    }

    // ── CANCEL ────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable String id,
                              RedirectAttributes ra) throws IOException {

        try {
            orderService.cancelOrder(id);
            ra.addFlashAttribute("successMsg", "Order " + id + " has been cancelled.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/orders/" + id;
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/delete")
    public String deleteOrder(@PathVariable String id,
                              RedirectAttributes ra) throws IOException {

        orderService.deleteOrder(id);
        ra.addFlashAttribute("successMsg", "Order " + id + " deleted.");

        return "redirect:/orders";
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private List<OrderItem> parseItems(String raw) {
        List<OrderItem> items = new ArrayList<>();

        if (raw == null || raw.trim().isEmpty()) {
            return items;
        }

        for (String entry : raw.split(",")) {
            String[] p = entry.split("\\|");

            if (p.length == 4) {
                try {
                    String medicineId = p[0];
                    String medicineName = p[1];
                    int quantity = Integer.parseInt(p[2]);
                    double price = Double.parseDouble(p[3]);

                    items.add(new OrderItem(medicineId, medicineName, quantity, price));

                } catch (NumberFormatException ignored) {
                    // Skip invalid item
                }
            }
        }

        return items;
    }
}