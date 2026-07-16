package com.dmg.moviebooking.repository;

import com.dmg.moviebooking.entity.Seat;
import com.dmg.moviebooking.enums.SeatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByScreenId(Long screenId);
    List<Seat> findByScreenIdAndSeatType(Long screenId, SeatType seatType);
    long countByScreenId(Long screenId);
}
