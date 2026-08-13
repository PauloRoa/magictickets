package com.magictickets.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.LocalDate;

import com.magictickets.domain.entity.Event;
import com.magictickets.domain.entity.ShowCategory;
import com.magictickets.domain.valueobject.EventDate;
import com.magictickets.domain.exception.InvalidQuantityException;
import com.magictickets.domain.exception.MaxTicketsExceededException;
import com.magictickets.domain.exception.OutOfStockException;

@DisplayName("Validador de Compras")
class PurchaseValidatorTest {

    private final PurchaseValidator validator = new PurchaseValidator();

    private Event buildEvent(int stock) {
        return new Event("Concierto", stock, new EventDate(LocalDate.now().plusDays(1)), ShowCategory.MUSIC);
    }

    @Test
    @DisplayName("Debería lanzar InvalidQuantityException cuando la cantidad es 0")
    void testValidateQuantityWithZero() {
        // Arrange & Act & Assert
        assertThrows(InvalidQuantityException.class, () -> validator.validateQuantity(0));
    }

    @Test
    @DisplayName("Debería aceptar una cantidad positiva")
    void testValidateQuantityWithPositiveValue() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> validator.validateQuantity(3));
    }

    @Test
    @DisplayName("Debería lanzar MaxTicketsExceededException cuando la cantidad supera 5")
    void testValidateMaxTicketsExceeded() {
        // Arrange & Act & Assert
        assertThrows(MaxTicketsExceededException.class, () -> validator.validateMaxTickets(6));
    }

    @Test
    @DisplayName("Debería aceptar exactamente 5 tickets")
    void testValidateMaxTicketsAtLimit() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> validator.validateMaxTickets(5));
    }

    @Test
    @DisplayName("Debería lanzar OutOfStockException cuando la cantidad supera el stock")
    void testValidateStockInsufficient() {
        // Arrange
        Event event = buildEvent(3);

        // Act & Assert
        assertThrows(OutOfStockException.class, () -> validator.validateStock(event, 5));
    }

    @Test
    @DisplayName("Debería aceptar una cantidad igual al stock disponible")
    void testValidateStockExactMatch() {
        // Arrange
        Event event = buildEvent(5);

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateStock(event, 5));
    }
}