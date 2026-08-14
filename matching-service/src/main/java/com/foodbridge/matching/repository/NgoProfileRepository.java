package com.foodbridge.matching.repository;

import com.foodbridge.matching.document.NgoProfile;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NgoProfileRepository extends MongoRepository<NgoProfile, String> {

    GeoResults<NgoProfile> findByLocationNearAndLocationSetTrue(Point location, Distance distance);
}
