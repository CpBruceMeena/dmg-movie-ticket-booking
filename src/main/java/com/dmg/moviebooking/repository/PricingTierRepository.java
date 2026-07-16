package com.dmg.moviebooking.repository;

import com.dmg.moviebooking.entity.PricingTier;
import com.dmg.moviebooking.enums.SeatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PricingTierRepository extends JpaRepository<PricingTier, Long> {
    Optional<PricingTier> findBySeatType(SeatType seatType);
    boolean existsByName(String name);
}
