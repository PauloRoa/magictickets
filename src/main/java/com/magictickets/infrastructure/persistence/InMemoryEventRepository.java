package com.magictickets.infrastructure.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.magictickets.domain.entity.Event;
import com.magictickets.domain.repository.EventRepository;

public class InMemoryEventRepository implements EventRepository {

    private final Map<String, Event> events = new HashMap<>();

    @Override
    public void save(Event event) {
        events.put(event.getId(), event);
    }

    @Override
    public Optional<Event> findById(String id) {
        return Optional.ofNullable(events.get(id));
    }
}