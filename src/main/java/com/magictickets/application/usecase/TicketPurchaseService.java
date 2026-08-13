package com.magictickets.application.usecase;

import java.util.Optional;

import com.magictickets.application.port.PurchaseNotifier;
import com.magictickets.domain.entity.Event;
import com.magictickets.domain.repository.EventRepository;
import com.magictickets.domain.service.PurchaseValidator;
import com.magictickets.domain.exception.EventNotFoundException;

public class TicketPurchaseService {

    private final EventRepository eventRepository;
    private final PurchaseNotifier notifier;
    private final PurchaseValidator validator;

    public TicketPurchaseService(EventRepository eventRepository, PurchaseNotifier notifier, PurchaseValidator validator) {
        this.eventRepository = eventRepository;
        this.notifier = notifier;
        this.validator = validator;
    }

    public void purchase(String eventId, int quantity) {
        Event event = findEvent(eventId);
        validator.validateQuantity(quantity);
        validator.validateMaxTickets(quantity);
        validator.validateStock(event, quantity);
        event.reduceStock(quantity);
        eventRepository.save(event);
        notifier.notifyPurchase(event.getName(), quantity);
    }

    private Event findEvent(String eventId) {
        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty()) {
            throw new EventNotFoundException("Event not found for id: " + eventId);
        }
        return event.get();
    }
}