package com.magictickets.domain.repository;

import java.util.Optional;
import com.magictickets.domain.entity.Event;

public interface EventRepository {
    void save(Event event);
    Optional<Event> findById(String id);
}