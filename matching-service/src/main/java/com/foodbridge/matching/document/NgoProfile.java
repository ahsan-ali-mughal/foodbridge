package com.foodbridge.matching.document;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * matching-service's own lightweight projection of an NGO: just enough
 * (location + default search radius) to compute matches, populated from
 * {@code user.registered} events and updated directly by the NGO via the API.
 * Deliberately not the source of truth for identity/verification — auth-service owns that.
 */
@Document(collection = "ngo_profiles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NgoProfile {

    @Id
    private String id; // same value as the NGO's auth-service user id, as a string

    private Long ngoId;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    private double serviceRadiusKm = 15.0;

    private boolean locationSet;

    public static NgoProfile stubFor(Long ngoId) {
        NgoProfile profile = new NgoProfile();
        profile.id = String.valueOf(ngoId);
        profile.ngoId = ngoId;
        profile.locationSet = false;
        return profile;
    }

    public void updateLocation(double latitude, double longitude, double serviceRadiusKm) {
        this.location = new GeoJsonPoint(longitude, latitude);
        this.serviceRadiusKm = serviceRadiusKm;
        this.locationSet = true;
    }
}
