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

    List<Show> findByMovieId(Long movieId);

    @Query("SELECT DISTINCT s.screenId FROM Show s WHERE s.movieId = :movieId")
    List<Long> findScreenIdsByMovieId(@Param("movieId") Long movieId);

    @Query("SELECT s FROM Show s WHERE s.screenId IN :screenIds")
    List<Show> findByScreenIds(@Param("screenIds") List<Long> screenIds);

    @Query("SELECT s FROM Show s WHERE s.screenId = :screenId AND s.startTime >= :from AND s.endTime <= :to")
    List<Show> findByScreenAndTimeRange(@Param("screenId") Long screenId,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    List<Show> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
}
