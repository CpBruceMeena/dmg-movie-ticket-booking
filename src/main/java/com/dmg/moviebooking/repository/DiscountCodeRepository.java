package com.dmg.moviebooking.repository;

import com.dmg.moviebooking.entity.DiscountCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Long> {

    Optional<DiscountCode> findByCode(String code);

    boolean existsByCode(String code);
}
