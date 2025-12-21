plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.itmo.market"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.3")
    }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
    
    // Database
    implementation("org.postgresql:r2dbc-postgresql:1.0.3.RELEASE")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.flywaydb:flyway-core:10.10.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.10.0")
    
    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
    
    // OpenAPI/Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.4.0")
    
    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
    implementation("io.github.resilience4j:resilience4j-timelimiter:2.2.0")
    
    // Logging
    implementation("org.springframework.boot:spring-boot-starter-logging")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("io.projectreactor:reactor-test:2024.04.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.testcontainers:testcontainers:1.19.7")
    testImplementation("org.testcontainers:postgresql:1.19.7")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
