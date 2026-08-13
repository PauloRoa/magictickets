package com.magictickets.domain.exception;

public class InvalidEventDateException extends RuntimeException {
    public InvalidEventDateException(String message) {
        super(message);
    }
}