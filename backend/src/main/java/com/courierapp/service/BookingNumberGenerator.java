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

    private static final DateTimeFormatter KEY_FMT = DateTimeFormatter.ofPattern("yyyy");

    private final BookingSequenceRepository sequenceRepository;
    private final String basePrefix;

    public BookingNumberGenerator(BookingSequenceRepository sequenceRepository,
                                  @Value("${app.booking.number-prefix:CB}") String basePrefix) {
        this.sequenceRepository = sequenceRepository;
        this.basePrefix = basePrefix;
    }

    /**
     * Generates the next booking number: C{companyCode}-CB-YYYYMMDD-NNNNN
     * e.g. C1-CB-20260701-00001
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String next(LocalDate date, String companyCode) {
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
        String companyPart = (companyCode != null && !companyCode.isBlank()) ? "C" + companyCode + "-" : "";
        return String.format("%s%s-%s-%05d", companyPart, basePrefix, key, value);
    }
}
