package ru.itmo.productservice.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(basePackages = ["ru.itmo.productservice.repository"])
class JpaConfig
