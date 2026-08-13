package com.magictickets.domain.entity;

import java.util.UUID;
import com.magictickets.domain.valueobject.EventDate;

public class Event {
    private final String id;
    private final String name;
    private int stock;
    private final EventDate date;
    private ShowStatus status;
    private final ShowCategory category;

    public Event(String name, int stock, EventDate date, ShowCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Event category cannot be null");
        }
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.stock = stock;
        this.date = date;
        this.status = ShowStatus.SCHEDULED;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getStock() {
        return stock;
    }

    public EventDate getDate() {
        return date;
    }

    public ShowStatus getStatus() {
        return status;
    }

    public ShowCategory getCategory() {
        return category;
    }

    public void reduceStock(int quantity) {
        stock -= quantity;
    }
}