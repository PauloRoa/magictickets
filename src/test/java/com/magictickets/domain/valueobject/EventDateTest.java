package com.magictickets.domain.valueobject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import com.magictickets.domain.exception.InvalidEventDateException;

@DisplayName("Value Object EventDate")
class EventDateTest {

    @Test
    @DisplayName("Debería lanzar InvalidEventDateException cuando la fecha es nula")
    void testCreateWithNullDate() {
        // Arrange & Act & Assert
        assertThrows(InvalidEventDateException.class, () -> new EventDate(null));
    }

    @Test
    @DisplayName("Debería lanzar InvalidEventDateException cuando la fecha es hoy")
    void testCreateWithTodayDate() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Act & Assert
        assertThrows(InvalidEventDateException.class, () -> new EventDate(today));
    }

    @Test
    @DisplayName("Debería lanzar InvalidEventDateException cuando la fecha es pasada")
    void testCreateWithPastDate() {
        // Arrange
        LocalDate pastDate = LocalDate.now().minusDays(1);

        // Act & Assert
        assertThrows(InvalidEventDateException.class, () -> new EventDate(pastDate));
    }

    @Test
    @DisplayName("Debería crear el Value Object cuando la fecha es futura")
    void testCreateWithFutureDate() {
        // Arrange
        LocalDate futureDate = LocalDate.now().plusDays(1);

        // Act & Assert
        assertDoesNotThrow(() -> new EventDate(futureDate));
    }

    @Test
    @DisplayName("Debería exponer la fecha almacenada a través de value()")
    void testValueReturnsStoredDate() {
        // Arrange
        LocalDate futureDate = LocalDate.now().plusDays(5);

        // Act
        EventDate eventDate = new EventDate(futureDate);

        // Assert
        assertEquals(futureDate, eventDate.value());
    }
}