
package com.healthdiary.backend.medical.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class MedicalTimeConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Europe/Rome"));
    }
}
