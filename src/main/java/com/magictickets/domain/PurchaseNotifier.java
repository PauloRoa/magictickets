package com.magictickets.domain;

public interface PurchaseNotifier {
    void notifyPurchase(String eventName, int quantity);
}