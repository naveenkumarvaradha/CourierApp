package com.courierapp.auth.service.impl;

import com.courierapp.auth.service.MfaService;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaServiceImpl implements MfaService {

    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;

    @Value("${app.mfa.issuer:CourierApp}")
    private String issuer;

    @Override
    public MfaSetupResult generateSecret(String username) {
        String secret = secretGenerator.generate();
        QrData qrData = new QrData.Builder()
                .label(username).secret(secret).issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1).digits(6).period(30).build();
        try {
            byte[] imageBytes = qrGenerator.generate(qrData);
            return new MfaSetupResult(secret, Utils.getDataUriForImage(imageBytes, qrGenerator.getImageMimeType()));
        } catch (QrGenerationException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    @Override
    public boolean verifyCode(String secret, String code) {
        CodeVerifier verifier = new DefaultCodeVerifier(
                new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6), new SystemTimeProvider());
        return verifier.isValidCode(secret, code);
    }
}
