package com.courierapp.config;

import org.apache.coyote.http11.Http11Nio2Protocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatNio2Customizer() {
        return factory -> factory.addProtocolHandlerCustomizers(handler -> {
            if (handler instanceof Http11Nio2Protocol) {
                // Already NIO2 — nothing to do
            }
        });
    }

    @Bean
    public TomcatServletWebServerFactory tomcatFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.setProtocol(Http11Nio2Protocol.class.getName());
        return factory;
    }
}
