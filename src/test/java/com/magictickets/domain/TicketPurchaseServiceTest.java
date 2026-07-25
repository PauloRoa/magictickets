package com.magictickets.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import com.magictickets.domain.exception.InvalidQuantityException;
import com.magictickets.domain.exception.MaxTicketsExceededException;
import com.magictickets.domain.exception.OutOfStockException;

@DisplayName("Servicio de Compras")
class TicketPurchaseServiceTest {

    @Test
    @DisplayName("Debería lanzar InvalidQuantityException cuando la cantidad es 0")
    void testPurchaseWithZeroQuantity() {
        // Arrange
        TicketPurchaseService service = new TicketPurchaseService();
        Event event = new Event("Concierto", 100);

        // Act & Assert
        assertThrows(InvalidQuantityException.class, () -> service.purchase(event, 0));
    }

    @Test
    @DisplayName("Debería lanzar InvalidQuantityException cuando la cantidad es negativa")
    void testPurchaseWithNegativeQuantity() {
        // Arrange
        TicketPurchaseService service = new TicketPurchaseService();
        Event event = new Event("Concierto", 100);

        // Act & Assert
        assertThrows(InvalidQuantityException.class, () -> service.purchase(event, -1));
    }

    @Test
    @DisplayName("Debería lanzar MaxTicketsExceededException cuando la cantidad supera 5")
    void testPurchaseExceedingMaxTickets() {
        // Arrange
        TicketPurchaseService service = new TicketPurchaseService();
        Event event = new Event("Concierto", 100);

        // Act & Assert
        assertThrows(MaxTicketsExceededException.class, () -> service.purchase(event, 7));
    }

    @Test
    @DisplayName("Debería permitir la compra cuando la cantidad es exactamente 5")
    void testPurchaseWithExactlyMaxAllowedTickets() {
        // Arrange
        TicketPurchaseService service = new TicketPurchaseService();
        Event event = new Event("Concierto", 100);

        // Act & Assert
        assertDoesNotThrow(() -> service.purchase(event, 5));
    }

    @Test
    @DisplayName("Debería lanzar OutOfStockException cuando la cantidad supera el stock disponible")
    void testPurchaseWithInsufficientStock() {
        // Arrange
        TicketPurchaseService service = new TicketPurchaseService();
        Event event = new Event("Concierto", 3);

        // Act & Assert
        assertThrows(OutOfStockException.class, () -> service.purchase(event, 5));
    }

    @Test
    @DisplayName("Debería reducir el stock correctamente cuando la compra es válida")
    void testPurchaseReducesStockCorrectly() {
        // Arrange
        TicketPurchaseService service = new TicketPurchaseService();
        Event event = new Event("Concierto", 10);

        // Act
        service.purchase(event, 4);

        // Assert
        assertEquals(6, event.getStock());
    }
}