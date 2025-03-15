package com.carming.backend.place.repository;

import com.carming.backend.place.domain.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long>, PlaceRepositoryCustom {

    @Query(
            value = "SELECT place.place_id, place.place_name, place.place_tel, place.place_category, place.place.lon, place.place.lat, " +
                    "place.place_region, place.place_address, place.place_rating_count, place.place_rating_sum, place.place_keyword, place.place_image " +
                    "FROM place " +
                    "WHERE region = :region AND category = :category " +
                    "ORDER BY place_rating_sum",
            nativeQuery = true
    )
    List<Place> findAllInfiniteScroll(@Param("region") String region,
                                      @Param("category") String category,
                                      @Param("limit") Long limit);

    @Query(
            value = "SELECT place.place_id, place.place_name, place.place_tel, place.place_category, place.place.lon, place.place.lat, " +
                    "place.place_region, place.place_address, place.place_rating_count, place.place_rating_sum, place.place_keyword, place.place_image " +
                    "FROM place " +
                    "WHERE place_id < :lastPlaceId, region = :region AND category = :category " +
                    "ORDER BY place_rating_sum",
            nativeQuery = true
    )
    List<Place> findAllInfiniteScroll(@Param("region") String region,
                                      @Param("category") String category,
                                      @Param("limit") Long limit,
                                      @Param("lastPlaceId") Long lastPlaceId);
}
