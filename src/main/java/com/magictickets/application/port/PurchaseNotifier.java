package com.magictickets.application.port;

public interface PurchaseNotifier {
    void notifyPurchase(String eventName, int quantity);
}