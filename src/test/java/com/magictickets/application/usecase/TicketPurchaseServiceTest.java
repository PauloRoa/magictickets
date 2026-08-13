package com.magictickets.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import com.magictickets.application.port.PurchaseNotifier;
import com.magictickets.domain.entity.Event;
import com.magictickets.domain.entity.ShowCategory;
import com.magictickets.domain.repository.EventRepository;
import com.magictickets.domain.service.PurchaseValidator;
import com.magictickets.domain.valueobject.EventDate;
import com.magictickets.domain.exception.EventNotFoundException;
import com.magictickets.domain.exception.InvalidQuantityException;
import com.magictickets.domain.exception.MaxTicketsExceededException;
import com.magictickets.domain.exception.OutOfStockException;

@DisplayName("Servicio de Compras")
class TicketPurchaseServiceTest {

    private Event buildEvent(String name, int stock) {
        return new Event(name, stock, new EventDate(LocalDate.now().plusDays(1)), ShowCategory.MUSIC);
    }

    private TicketPurchaseService buildService(EventRepository repository, PurchaseNotifier notifier) {
        return new TicketPurchaseService(repository, notifier, new PurchaseValidator());
    }

    @Test
    @DisplayName("Debería lanzar InvalidQuantityException cuando la cantidad es 0")
    void testPurchaseWithZeroQuantity() {
        // Arrange
        EventRepository repository = mock(EventRepository.class);
        PurchaseNotifier notifier = mock(PurchaseNotifier.class);
        TicketPurchaseService service = buildService(repository, notifier);
        Event event = buildEvent("Concierto", 100);
        when(repository.findById(event.getId())).thenReturn(Optional.of(event));

        // Act & Assert
        assertThrows(InvalidQuantityException.class, () -> service.purchase(event.getId(), 0));
    }

    @Test
    @DisplayName("Debería lanzar InvalidQuantityException cuando la cantidad es negativa")
    void testPurchaseWithNegativeQuantity() {
        // Arrange
        EventRepository repository = mock(EventRepository.class);
        PurchaseNotifier notifier = mock(PurchaseNotifier.class);
        TicketPurchaseService service = buildService(repository, notifier);
        Event event = buildEvent("Concierto", 100);
        when(repository.findById(event.getId())).thenReturn(Optional.of(event));

        // Act & Assert
        assertThrows(InvalidQuantityException.class, () -> service.purchase(event.getId(), -1));
    }

    @Test
    @DisplayName("Debería lanzar MaxTicketsExceededException cuando la cantidad supera 5")
    void testPurchaseExceedingMaxTickets() {
        // Arrange
        EventRepository repository = mock(EventRepository.class);
        PurchaseNotifier notifier = mock(PurchaseNotifier.class);
        TicketPurchaseService service = buildService(repository, notifier);
        Event event = buildEvent("Concierto", 100);
        when(repository.findById(event.getId())).thenReturn(Optional.of(event));

        // Act & Assert
        assertThrows(MaxTicketsExceededException.class, () -> service.purchase(event.getId(), 7));
    }

    @Test
    @DisplayName("Debería permitir la compra cuando la cantidad es exactamente 5")
    void testPurchaseWithExactlyMaxAllowedTickets() {
        // Arrange
        EventRepository repository = mock(EventRepository.class);
        PurchaseNotifier notifier = mock(PurchaseNotifier.class);
        TicketPurchaseService service = buildService(repository, notifier);
        Event event = buildEvent("Concierto", 100);
        when(repository.findById(event.getId())).thenReturn(Optional.of(event));

        // Act & Assert
        assertDoesNotThrow(() -> service.purchase(event.getId(), 5));
    }

    @Test
    @DisplayName("Debería lanzar OutOfStockException cuando la cantidad supera el stock disponible")
    void testPurchaseWithInsufficientStock() {
        // Arrange
        EventRepository repository = mock(EventRepository.class);
        PurchaseNotifier notifier = mock(PurchaseNotifier.class);
        TicketPurchaseService service = buildService(repository, notifier);
        Event event = buildEvent("Concierto", 3);
        when(repository.findById(event.getId())).thenReturn(Optional.of(event));

        // Act & Assert
        assertThrows(OutOfStockException.class, () -> service.purchase(event.getId(), 5));
    }

    @Test
    @DisplayName("Debería reducir el stock correctamente cuando la compra es válida")
    void testPurchaseReducesStockCorrectly() {
        // Arrange
        EventRepository repository = mock(EventRepository.class);
        PurchaseNotifier notifier = mock(PurchaseNotifier.class);
        TicketPurchaseService service = buildService(repository, notifier);
        Event event = buildEvent("Concierto", 10);
        when(repository.findById(event.getId())).thenReturn(Optional.of(event));

        // Act
        service.purchase(event.getId(), 4);

        // Assert
        assertEquals(6, event.getStock());
    }

    @Test
    @DisplayName("Debería notificar la compra cuando esta se realiza correctamente")
    void testPurchaseNotifiesOnSuccess() {
        // Arrange
        EventRepository repository = mock(EventRepository.class);
        PurchaseNotifier notifier = mock(PurchaseNotifier.class);
        TicketPurchaseService service = buildService(repository, notifier);
        Event event = buildEvent("Concierto", 10);
        when(repository.findById(event.getId())).thenReturn(Optional.of(event));

        // Act
        service.purchase(event.getId(), 3);

        // Assert
        verify(notifier).notifyPurchase("Concierto", 3);
    }

    @Test
    @DisplayName("Debería guardar el evento en el repositorio tras una compra exitosa")
    void testPurchaseSavesEventOnSuccess() {
        // Arrange
        EventRepository repository = mock(EventRepository.class);
        PurchaseNotifier notifier = mock(PurchaseNotifier.class);
        TicketPurchaseService service = buildService(repository, notifier);
        Event event = buildEvent("Concierto", 10);
        when(repository.findById(event.getId())).thenReturn(Optional.of(event));

        // Act
        service.purchase(event.getId(), 2);

        // Assert
        verify(repository).save(event);
    }

    @Test
    @DisplayName("Debería lanzar EventNotFoundException cuando el evento no existe")
    void testPurchaseWithNonExistentEvent() {
        // Arrange
        EventRepository repository = mock(EventRepository.class);
        PurchaseNotifier notifier = mock(PurchaseNotifier.class);
        TicketPurchaseService service = buildService(repository, notifier);
        when(repository.findById("non-existent-id")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EventNotFoundException.class, () -> service.purchase("non-existent-id", 3));
    }
}