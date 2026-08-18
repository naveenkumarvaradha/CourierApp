package com.courierapp.service.tracking;

import com.courierapp.dto.tracking.TrackingEventResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PLACEHOLDER — Maruti Courier does not have a publicly documented tracking
 * API. Real integration is blocked on getting API/EDI access details from
 * Maruti's business/account team (they typically only grant this to
 * registered corporate shippers, not self-serve).
 *
 * Until then, this returns a single clearly-labeled placeholder event so the
 * UI has something consistent to render rather than an error. Replace the
 * body of {@link #track} with a real HTTP call once credentials exist —
 * everything else (entity, repository, controller, frontend) is already
 * wired to whatever this returns.
 */
@Slf4j
@Component
public class MarutiTrackingProvider implements TrackingProvider {

    @Override
    public String courierWayName() {
        return "MARUTI";
    }

    @Override
    public List<TrackingEventResponse> track(String awbNumber) {
        log.warn("Maruti tracking requested for AWB {} — no real API is wired up yet, returning placeholder", awbNumber);
        // No real event exists, so no real timestamp either — eventTime stays null. This also
        // keeps the event stable across repeat calls so TrackingService's dedupe (keyed on
        // status + eventTime) doesn't insert a fresh junk row every time someone re-syncs.
        return List.of(new TrackingEventResponse(
                "MARUTI",
                "NOT_AVAILABLE",
                "Maruti Courier tracking integration is not connected yet — this is placeholder data, not a real status.",
                null,
                null
        ));
    }
}
