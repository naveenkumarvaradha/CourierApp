package com.courierapp.service;

import com.courierapp.entity.DcReceiptSequence;
import com.courierapp.repository.DcReceiptSequenceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class DcReceiptNumberGenerator {

    private static final DateTimeFormatter KEY_FMT = DateTimeFormatter.ofPattern("yyyy");

    private final DcReceiptSequenceRepository sequenceRepository;
    private final String basePrefix;

    public DcReceiptNumberGenerator(DcReceiptSequenceRepository sequenceRepository,
                                    @Value("${app.dc-receipt.number-prefix:RC}") String basePrefix) {
        this.sequenceRepository = sequenceRepository;
        this.basePrefix = basePrefix;
    }

    /**
     * Generates the next DC receipt number: C{companyCode}-RC-YYYY-NNNNN
     * e.g. C1-RC-2026-00001
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String next(LocalDate date, String companyCode) {
        String key = date.format(KEY_FMT);
        DcReceiptSequence seq = sequenceRepository.findBySeqDate(key)
                .orElseGet(() -> {
                    DcReceiptSequence s = new DcReceiptSequence();
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
