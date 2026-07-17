package com.dmg.moviebooking.repository;

import com.dmg.moviebooking.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByTitleContainingIgnoreCase(String title);

    @Query("SELECT DISTINCT m FROM Movie m JOIN Show s ON s.movieId = m.id WHERE s.screenId IN " +
           "(SELECT sc.id FROM Screen sc WHERE sc.theaterId = :theaterId)")
    List<Movie> findMoviesByTheaterId(@Param("theaterId") Long theaterId);

    boolean existsByTitle(String title);
}
