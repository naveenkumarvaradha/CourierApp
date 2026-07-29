package com.courierapp.service;

import com.courierapp.entity.DcSequence;
import com.courierapp.repository.DcSequenceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class DcNumberGenerator {

    private static final DateTimeFormatter KEY_FMT = DateTimeFormatter.ofPattern("yyyy");

    private final DcSequenceRepository sequenceRepository;
    private final String basePrefix;

    public DcNumberGenerator(DcSequenceRepository sequenceRepository,
                             @Value("${app.dc.number-prefix:DC}") String basePrefix) {
        this.sequenceRepository = sequenceRepository;
        this.basePrefix = basePrefix;
    }

    /**
     * Generates the next DC number: C{companyCode}-DC-YYYY-NNNNN
     * e.g. C1-DC-2026-00001
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String next(LocalDate date, String companyCode) {
        String key = date.format(KEY_FMT);
        DcSequence seq = sequenceRepository.findBySeqDate(key)
                .orElseGet(() -> {
                    DcSequence s = new DcSequence();
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
