package com.dmg.moviebooking.repository;

import com.dmg.moviebooking.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {
    List<Show> findByScreenId(Long screenId);

    @Query("SELECT s FROM Show s JOIN s.screen sc JOIN sc.theater t WHERE t.id = :theaterId")
    List<Show> findByTheaterId(@Param("theaterId") Long theaterId);

    @Query("SELECT s FROM Show s WHERE s.screen.id = :screenId AND s.startTime >= :from AND s.endTime <= :to")
    List<Show> findByScreenAndTimeRange(@Param("screenId") Long screenId,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    List<Show> findByMovieTitleContainingIgnoreCase(String movieTitle);
}
