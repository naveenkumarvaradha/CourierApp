package com.courierapp.actuator;

import com.courierapp.repository.BookingRepository;
import com.courierapp.repository.UserRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("courierApp")
public class CourierAppHealthIndicator implements HealthIndicator {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public CourierAppHealthIndicator(BookingRepository bookingRepository,
                                     UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Health health() {
        try {
            long totalBookings = bookingRepository.count();
            long totalUsers = userRepository.count();
            return Health.up()
                    .withDetail("totalBookings", totalBookings)
                    .withDetail("totalUsers", totalUsers)
                    .withDetail("status", "All systems operational")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
