package com.courierapp.config;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TotpConfig {

    @Bean
    public SecretGenerator secretGenerator() {
        return new DefaultSecretGenerator(32);
    }

    @Bean
    public QrGenerator qrGenerator() {
        return new ZxingPngQrGenerator();
    }

    @Bean
    public DefaultCodeVerifier codeVerifier() {
        return new DefaultCodeVerifier(
                new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6),
                new SystemTimeProvider());
    }
}
