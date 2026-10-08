package br.dev.jady.candidaturas;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
class RelogioConfig {
    @Bean Clock relogio() { return Clock.system(ZoneId.of("America/Sao_Paulo")); }
}
