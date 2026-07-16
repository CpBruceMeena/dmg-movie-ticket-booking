package com.dmg.moviebooking.repository;

import com.dmg.moviebooking.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TheaterRepository extends JpaRepository<Theater, Long> {
    List<Theater> findByCityId(Long cityId);
    boolean existsByNameAndCityId(String name, Long cityId);
}
