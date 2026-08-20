package self.dev.order.service;

import org.springframework.stereotype.Service;

import self.dev.domain.event.*;
import self.dev.domain.enums.*;


@Service
public class OrderManageService {

    // confirm result of the order based on the payment and inventory events, return the reason for failure if any
    public OrderEvent confirm(PaymentEvent paymentEvent, InventoryEvent inventoryEvent) {

        boolean paymentSuccess =
                paymentEvent.getStatus()
                        == PaymentStatus.RESERVATION_SUCCESS;

        boolean inventorySuccess =
                inventoryEvent.getStatus()
                        == InventoryStatus.RESERVATION_SUCCESS;

        OrderStatus finalStatus =
                paymentSuccess && inventorySuccess
                        ? OrderStatus.CONFIRMED
                        : OrderStatus.CANCELLED;

        OrderEvent orderEvent = new OrderEvent(
                paymentEvent.getOrderId(),
                paymentEvent.getCustomerId(),
                finalStatus,
                inventoryEvent.getProductId(),
                inventoryEvent.getQuantity(),
                paymentEvent.getAmount()
        );

        if (finalStatus == OrderStatus.CANCELLED) {
            orderEvent.setReason(getFailureReason(paymentEvent, inventoryEvent));
        }

        return orderEvent;
    }

    // get the reason for failure based on the payment and inventory events (reservation failure for payment OR inventory)
    private String getFailureReason(
            PaymentEvent paymentEvent,
            InventoryEvent inventoryEvent) {

        if (paymentEvent.getStatus()
                != PaymentStatus.RESERVATION_SUCCESS) {
            return paymentEvent.getReason() != null
                    ? paymentEvent.getReason()
                    : "Payment reservation failed";
        }

        return inventoryEvent.getReason() != null
                ? inventoryEvent.getReason()
                : "Inventory reservation failed";
    }
}
