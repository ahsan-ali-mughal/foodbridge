package com.foodbridge.listing.document;

import com.foodbridge.common.enums.ListingStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * A food donation listing. Modeled as a MongoDB document rather than a
 * relational row because attribute shape varies significantly by donor type
 * (a caterer's "500 servings, buffet trays" listing looks very different
 * from a bakery's "40 unsold loaves") and because geo queries against a
 * 2dsphere index are a first-class MongoDB capability.
 */
@Document(collection = "listings")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Listing {

    @Id
    private String id;

    private Long donorId;

    private String foodType;

    private int quantityServings;

    private List<String> imageUrls;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    private String pickupAddress;

    private ListingStatus status;

    private Instant preparedAt;

    private Instant expiryAt;

    private String claimedByNgoId;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public static Listing newListing(Long donorId, String foodType, int quantityServings,
                                      List<String> imageUrls, double latitude, double longitude,
                                      String pickupAddress, Instant preparedAt, Instant expiryAt) {
        Listing listing = new Listing();
        listing.donorId = donorId;
        listing.foodType = foodType;
        listing.quantityServings = quantityServings;
        listing.imageUrls = imageUrls;
        // GeoJsonPoint stores (longitude, latitude) order per the GeoJSON spec.
        listing.location = new GeoJsonPoint(longitude, latitude);
        listing.pickupAddress = pickupAddress;
        listing.preparedAt = preparedAt;
        listing.expiryAt = expiryAt;
        listing.status = ListingStatus.ACTIVE;
        return listing;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiryAt);
    }

    public boolean isClaimable() {
        return status == ListingStatus.ACTIVE || status == ListingStatus.MATCHED;
    }
}
