package com.courierapp.actuator;

import com.courierapp.enums.BookingStatus;
import com.courierapp.repository.BookingRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class BookingMetrics {

    private final MeterRegistry registry;
    private final BookingRepository bookingRepository;

    public BookingMetrics(MeterRegistry registry, BookingRepository bookingRepository) {
        this.registry = registry;
        this.bookingRepository = bookingRepository;
    }

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("courier.bookings.pending_approval", bookingRepository,
                        r -> r.countByStatus(BookingStatus.PENDING_APPROVAL))
                .description("Number of bookings pending approval")
                .register(registry);

        Gauge.builder("courier.bookings.approved", bookingRepository,
                        r -> r.countByStatus(BookingStatus.APPROVED))
                .description("Number of approved bookings")
                .register(registry);

        Gauge.builder("courier.bookings.total", bookingRepository,
                        r -> (double) r.count())
                .description("Total number of bookings")
                .register(registry);
    }
}
