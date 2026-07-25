package com.magictickets.domain;

import com.magictickets.domain.exception.MaxTicketsExceededException;
import com.magictickets.domain.exception.OutOfStockException;
import com.magictickets.domain.exception.InvalidQuantityException;

public class TicketPurchaseService {

    private final PurchaseNotifier notifier;

    public TicketPurchaseService(PurchaseNotifier notifier) {
        this.notifier = notifier;
    }

    public void purchase(Event event, int quantity) {
        validateQuantity(quantity);
        validateMaxTickets(quantity);
        validateStock(event, quantity);
        event.reduceStock(quantity);
        notifier.notifyPurchase(event.getName(), quantity);
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("Quantity must be greater than 0, " + quantity + " is invalid");
        }
    }

    private void validateMaxTickets(int quantity) {
        if (quantity > 5) {
            throw new MaxTicketsExceededException("5 tickets is the maximum allowed per user, " + quantity + " tickets exceed the limit");
        }
    }

    private void validateStock(Event event, int quantity) {
        if (quantity > event.getStock()) {
            throw new OutOfStockException("Not enough tickets in stock, " + quantity + " tickets requested but only " + event.getStock() + " available");
        }
    }
}