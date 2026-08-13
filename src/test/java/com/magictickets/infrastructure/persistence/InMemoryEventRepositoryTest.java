package com.magictickets.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;
import java.util.Optional;

import com.magictickets.domain.entity.Event;
import com.magictickets.domain.entity.ShowCategory;
import com.magictickets.domain.valueobject.EventDate;

@DisplayName("Repositorio en Memoria de Event")
class InMemoryEventRepositoryTest {

    private final InMemoryEventRepository repository = new InMemoryEventRepository();

    private Event buildEvent() {
        return new Event("Concierto", 100, new EventDate(LocalDate.now().plusDays(1)), ShowCategory.MUSIC);
    }

    @Test
    @DisplayName("Debería guardar y luego encontrar un evento por su id")
    void testSaveAndFindById() {
        // Arrange
        Event event = buildEvent();

        // Act
        repository.save(event);
        Optional<Event> found = repository.findById(event.getId());

        // Assert
        assertTrue(found.isPresent());
        assertEquals(event.getId(), found.get().getId());
    }

    @Test
    @DisplayName("Debería devolver Optional vacío cuando el id no existe")
    void testFindByIdNotFound() {
        // Arrange & Act
        Optional<Event> found = repository.findById("non-existent-id");

        // Assert
        assertFalse(found.isPresent());
    }
}