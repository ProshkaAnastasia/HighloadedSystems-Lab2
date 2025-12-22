package ru.itmo.gateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.annotation.Order
import org.springframework.context.annotation.Configuration
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Order(-1)
@Configuration
class PreservePathFilter : GlobalFilter {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val realPath = exchange.request.path.value()
        
        exchange.attributes["X-Real-Path"] = realPath
        
        return chain.filter(exchange)
    }
}
