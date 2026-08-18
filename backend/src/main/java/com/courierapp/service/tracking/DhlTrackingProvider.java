package com.courierapp.service.tracking;

import com.courierapp.dto.tracking.TrackingEventResponse;
import com.courierapp.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * DHL Unified Tracking API client — https://developer.dhl.com/api-reference/shipment-tracking
 *
 * Requires a free DHL developer account and API key (DHL_API_KEY env var).
 * Response field names below match DHL's documented schema as of API v1;
 * verify against a live sandbox call before relying on this in production,
 * since this integration has not been exercised against a real DHL account.
 */
@Slf4j
@Component
public class DhlTrackingProvider implements TrackingProvider {

    private final RestClient restClient;
    private final String apiKey;
    private final boolean enabled;

    public DhlTrackingProvider(
            @Value("${app.tracking.dhl.base-url}") String baseUrl,
            @Value("${app.tracking.dhl.api-key:}") String apiKey,
            @Value("${app.tracking.dhl.enabled:false}") boolean enabled) {
        this.apiKey = apiKey;
        this.enabled = enabled;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public String courierWayName() {
        return "DHL";
    }

    @Override
    public List<TrackingEventResponse> track(String awbNumber) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(
                    "DHL tracking is not configured. Set DHL_TRACKING_ENABLED=true and DHL_API_KEY " +
                    "(get one free at developer.dhl.com) to enable live tracking.");
        }

        JsonNode root;
        try {
            root = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/track/shipments")
                            .queryParam("trackingNumber", awbNumber)
                            .build())
                    .header("DHL-API-Key", apiKey)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            log.warn("DHL tracking API returned {} for AWB {}: {}", e.getStatusCode(), awbNumber, e.getResponseBodyAsString());
            throw new BusinessException("DHL tracking lookup failed (" + e.getStatusCode() + "). "
                    + "Check the AWB number and DHL API key.");
        }

        List<TrackingEventResponse> events = new ArrayList<>();
        if (root == null) return events;

        JsonNode shipments = root.path("shipments");
        for (JsonNode shipment : shipments) {
            for (JsonNode event : shipment.path("events")) {
                events.add(new TrackingEventResponse(
                        "DHL",
                        text(event, "status", "statusCode"),
                        text(event, "description"),
                        locationOf(event),
                        parseTime(text(event, "timestamp"))
                ));
            }
        }
        return events;
    }

    private static String text(JsonNode node, String... field) {
        for (String f : field) {
            if (node.hasNonNull(f)) return node.get(f).asText();
        }
        return null;
    }

    private static String locationOf(JsonNode event) {
        JsonNode address = event.path("location").path("address");
        String locality = address.path("addressLocality").asText(null);
        String country = address.path("countryCode").asText(null);
        if (locality == null) return country;
        return country == null ? locality : locality + ", " + country;
    }

    private static OffsetDateTime parseTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
