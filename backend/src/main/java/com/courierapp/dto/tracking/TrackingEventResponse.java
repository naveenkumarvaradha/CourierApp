package com.courierapp.dto.tracking;

import java.time.OffsetDateTime;

public record TrackingEventResponse(
        String provider,
        String status,
        String description,
        String location,
        OffsetDateTime eventTime
) {
}
