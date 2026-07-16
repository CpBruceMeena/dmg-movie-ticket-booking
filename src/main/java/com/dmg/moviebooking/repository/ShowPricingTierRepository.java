package com.dmg.moviebooking.repository;

import com.dmg.moviebooking.entity.ShowPricingTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShowPricingTierRepository extends JpaRepository<ShowPricingTier, Long> {
    List<ShowPricingTier> findByShowId(Long showId);
    List<ShowPricingTier> findByPricingTierId(Long pricingTierId);
    void deleteByShowId(Long showId);
}
