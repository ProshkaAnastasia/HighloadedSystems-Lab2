package ru.itmo.gateway.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import org.springframework.cloud.gateway.route.Route
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils

@RestController
class FallbackController {

    @RequestMapping("/fallback")
    fun fallback(exchange: ServerWebExchange): ResponseEntity<Map<String, Any>> {
        val route = exchange.getAttribute<Route>(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)
        val routeId = route?.id ?: "unknown-service"
        val serviceName = formatServiceName(routeId)
        
        // Получаем реальный путь из атрибутов (работает с forward)
        val realPath = exchange.attributes["X-Real-Path"]?.toString() 
            ?: exchange.request.path.value()
        
        return ResponseEntity(
            mapOf(
                "status" to "SERVICE_UNAVAILABLE",
                "error" to "ServiceUnavailable",
                "message" to "The $serviceName is currently unavailable. Please try again later.",
                "service" to routeId,
                "serviceName" to serviceName,
                "timestamp" to System.currentTimeMillis(),
                "path" to realPath
            ),
            HttpStatus.SERVICE_UNAVAILABLE
        )
    }

    private fun formatServiceName(routeId: String): String {
        return routeId
            .split("-")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            .replaceFirstChar { it.uppercase() }
    }
}
