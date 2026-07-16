package com.dmg.moviebooking.repository;

import com.dmg.moviebooking.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    @Query("SELECT bs.seatId FROM BookingSeat bs WHERE bs.bookingId IN (SELECT b.id FROM Booking b WHERE b.showId = :showId AND b.status = 'CONFIRMED')")
    List<Long> findBookedSeatIdsByShowId(@Param("showId") Long showId);

    @Query("SELECT bs.seatId FROM BookingSeat bs WHERE bs.bookingId = :bookingId")
    List<Long> findSeatIdsByBookingId(@Param("bookingId") Long bookingId);

    List<BookingSeat> findByBookingId(Long bookingId);
}
