
package com.magictickets.domain;


public class Event {
    private final String name;
    private int stock;
        
    public Event(String name, int stock) {
        this.name = name;
        this.stock = stock;
    }

    public String getName() {
        return name;
    }

    public int getStock() {
        return stock;
    }

    public void reduceStock(int quantity) {
            stock -= quantity;
    }
}

