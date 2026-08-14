package com.foodbridge.listing.repository;

import com.foodbridge.common.enums.ListingStatus;
import com.foodbridge.listing.document.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;

public interface ListingRepository extends MongoRepository<Listing, String> {

    Page<Listing> findByDonorId(Long donorId, Pageable pageable);

    GeoResults<Listing> findByLocationNearAndStatus(Point location, Distance distance, ListingStatus status);

    List<Listing> findByStatusAndExpiryAtBefore(ListingStatus status, Instant instant);

    @Query("{ 'status': ?0, 'expiryAt': { $gte: ?1, $lte: ?2 } }")
    List<Listing> findByStatusAndExpiryAtBetween(ListingStatus status, Instant from, Instant to);
}
