package com.medstore.order.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Catches unhandled exceptions across all controllers and renders
 * a friendly error page instead of a raw stack trace.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public String handleNotFound(OrderNotFoundException ex, Model model) {
        model.addAttribute("errorTitle", "Order Not Found");
        model.addAttribute("errorMessage", ex.getMessage());
        return "order/error";
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalState(IllegalStateException ex, Model model) {
        model.addAttribute("errorTitle", "Action Not Allowed");
        model.addAttribute("errorMessage", ex.getMessage());
        return "order/error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception ex, Model model) {
        model.addAttribute("errorTitle", "Something Went Wrong");
        model.addAttribute("errorMessage", ex.getMessage());
        return "order/error";
    }
}
