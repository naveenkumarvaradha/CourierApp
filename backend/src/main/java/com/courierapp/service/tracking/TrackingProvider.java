package com.courierapp.service.tracking;

import com.courierapp.dto.tracking.TrackingEventResponse;

import java.util.List;

/**
 * One implementation per carrier (DHL, Maruti, ...). {@link #courierWayName()}
 * must match the {@code courier_ways.name} value it handles.
 */
public interface TrackingProvider {

    String courierWayName();

    /**
     * Fetches the current tracking history for an AWB/tracking number from
     * the carrier. Newest event first.
     */
    List<TrackingEventResponse> track(String awbNumber);
}
