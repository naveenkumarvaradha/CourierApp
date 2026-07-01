package com.courierapp.service;

import com.courierapp.entity.BookingSequence;
import com.courierapp.repository.BookingSequenceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class BookingNumberGenerator {

    private static final DateTimeFormatter KEY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final BookingSequenceRepository sequenceRepository;
    private final String prefix;

    public BookingNumberGenerator(BookingSequenceRepository sequenceRepository,
                                  @Value("${app.booking.number-prefix:CB}") String prefix) {
        this.sequenceRepository = sequenceRepository;
        this.prefix = prefix;
    }

    /**
     * Generates the next booking number in the form PREFIX-YYYYMMDD-NNNNN.
     * Uses a row-locked per-day counter so concurrent bookings get unique numbers.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String next(LocalDate date) {
        String key = date.format(KEY_FMT);
        BookingSequence seq = sequenceRepository.findBySeqDate(key)
                .orElseGet(() -> {
                    BookingSequence s = new BookingSequence();
                    s.setSeqDate(key);
                    s.setLastValue(0L);
                    return s;
                });
        long value = seq.getLastValue() + 1;
        seq.setLastValue(value);
        sequenceRepository.saveAndFlush(seq);
        return String.format("%s-%s-%05d", prefix, key, value);
    }
}
