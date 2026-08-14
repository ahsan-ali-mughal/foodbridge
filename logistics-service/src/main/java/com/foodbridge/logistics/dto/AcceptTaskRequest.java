package com.foodbridge.logistics.dto;

// Marker DTO: volunteer accepting a task carries no body payload beyond the
// path variable and authenticated principal; kept as a record for symmetry
// and to leave room for future fields (e.g. an ETA) without breaking the API shape.
public record AcceptTaskRequest() {
}
