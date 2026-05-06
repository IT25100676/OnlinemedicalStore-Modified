package com.medstore.order.model.payment;

import com.medstore.order.model.PaymentMethod;

/**
 * Factory that instantiates the correct Payment subclass based on PaymentMethod.
 * Keeps Controller clean and uses POLYMORPHISM transparently.
 */
public class PaymentFactory {

    private PaymentFactory() {} // utility class — not instantiable

    public static Payment create(PaymentMethod method,
                                 double amount,
                                 String extra1,   // deliveryAddress | cardHolder | bankName
                                 String extra2) { // null | cardNumber | referenceNumber
        return switch (method) {
            case CASH_ON_DELIVERY ->
                    new CashOnDeliveryPayment(amount, extra1);
            case CREDIT_CARD ->
                    new CardPayment(amount, extra1, extra2 != null ? extra2 : "0000");
            case ONLINE_TRANSFER ->
                    new OnlineTransferPayment(amount, extra1, extra2 != null ? extra2 : "N/A");
        };
    }
}
