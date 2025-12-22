package ru.itmo.gateway.config

import io.netty.channel.ChannelOption
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration


@Configuration
class ReactiveClientConfig {

    @Bean
    fun exchangeStrategies(): ExchangeStrategies {
        return ExchangeStrategies.builder()
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024) // 1MB
            }
            .build()
    }

    @Bean
    fun webClientBuilder(exchangeStrategies: ExchangeStrategies): WebClient.Builder {
        // Конфигурируем HttpClient с timeout'ами
        val httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(10))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)

        return WebClient.builder()
            .exchangeStrategies(exchangeStrategies)
            .clientConnector(ReactorClientHttpConnector(httpClient))
    }
}
