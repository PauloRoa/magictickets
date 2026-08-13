package com.magictickets.domain.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;

import com.magictickets.domain.valueobject.EventDate;

@DisplayName("Entidad Event")
class EventTest {

    @Test
    @DisplayName("Debería lanzar IllegalArgumentException cuando la categoría es nula")
    void testCreateWithNullCategory() {
        // Arrange
        EventDate date = new EventDate(LocalDate.now().plusDays(1));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new Event("Concierto", 100, date, null));
    }

    @Test
    @DisplayName("Debería crear el evento cuando todos los datos son válidos")
    void testCreateWithValidData() {
        // Arrange
        EventDate date = new EventDate(LocalDate.now().plusDays(1));

        // Act & Assert
        assertDoesNotThrow(() -> new Event("Concierto", 100, date, ShowCategory.MUSIC));
    }

    @Test
    @DisplayName("Debería nacer con status SCHEDULED por defecto")
    void testNewEventStartsAsScheduled() {
        // Arrange
        EventDate date = new EventDate(LocalDate.now().plusDays(1));

        // Act
        Event event = new Event("Concierto", 100, date, ShowCategory.MUSIC);

        // Assert
        assertEquals(ShowStatus.SCHEDULED, event.getStatus());
    }

    @Test
    @DisplayName("Debería generar un id automáticamente al crear el evento")
    void testEventGeneratesId() {
        // Arrange
        EventDate date = new EventDate(LocalDate.now().plusDays(1));

        // Act
        Event event = new Event("Concierto", 100, date, ShowCategory.MUSIC);

        // Assert
        assertNotNull(event.getId());
    }

    @Test
    @DisplayName("Debería exponer la fecha y categoría asignadas en el constructor")
    void testGettersReturnConstructorValues() {
        // Arrange
        EventDate date = new EventDate(LocalDate.now().plusDays(1));

        // Act
        Event event = new Event("Concierto", 100, date, ShowCategory.SPORTS);

        // Assert
        assertEquals(date, event.getDate());
        assertEquals(ShowCategory.SPORTS, event.getCategory());
    }
}