package com.dmg.moviebooking.repository;

import com.dmg.moviebooking.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, Long> {
    List<Screen> findByTheaterId(Long theaterId);
    boolean existsByNameAndTheaterId(String name, Long theaterId);
}
