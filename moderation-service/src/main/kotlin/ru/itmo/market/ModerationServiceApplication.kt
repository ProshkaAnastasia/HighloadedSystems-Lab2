package ru.itmo.market

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = ["ru.itmo.market.client"])
@ComponentScan(basePackages = ["ru.itmo.market"])
class ModerationServiceApplication

fun main(args: Array<String>) {
    runApplication<ModerationServiceApplication>(*args)
}
