package com.dmg.moviebooking.repository;

import com.dmg.moviebooking.entity.Booking;
import com.dmg.moviebooking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findByShowIdAndStatus(Long showId, BookingStatus status);

    long countByShowIdAndStatus(Long showId, BookingStatus status);

    List<Booking> findByStatusAndHoldExpiresAtBefore(BookingStatus status, LocalDateTime now);

    List<Booking> findByStatusAndShowIdIn(BookingStatus status, List<Long> showIds);
}
