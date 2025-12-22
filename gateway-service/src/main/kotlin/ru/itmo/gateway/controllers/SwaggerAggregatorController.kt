package ru.itmo.gateway.controllers

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.http.MediaType as HttpMediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import org.slf4j.LoggerFactory

@RestController
class SwaggerAggregatorController {

    @Autowired
    private lateinit var routeLocator: RouteLocator

    @Autowired
    private lateinit var webClient: WebClient

    private val logger = LoggerFactory.getLogger(SwaggerAggregatorController::class.java)

    @GetMapping(value = ["/v1/api-docs"], produces = [HttpMediaType.APPLICATION_JSON_VALUE])
    fun aggregateApiDocs(): Mono<OpenAPI> {
        val baseOpenAPI = OpenAPI()
            .info(
                Info()
                    .title("Gateway API")
                    .description("Unified API Gateway with Microservices")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("API Support")
                    )
            )
            .addServersItem(
                Server()
                    .url("http://localhost:8080")
                    .description("Gateway")
            )

        return Flux.from(routeLocator.routes)
            .filter { route ->
                route.id != null && !route.id.contains("null")
            }
            .flatMap { route ->
                fetchServiceDocs(route.id)
                    .map { docs -> route.id to docs }
            }
            .collectMap({ it.first }, { it.second })
            .map { serviceDocs ->
                mergePathItems(baseOpenAPI, serviceDocs)
            }
            .onErrorResume { error ->
                logger.error("Error aggregating docs: ${error.message}", error)
                Mono.just(baseOpenAPI)
            }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun fetchServiceDocs(serviceId: String?): Mono<Map<String, Any>> {
        if (serviceId == null) return Mono.empty()

        val docsUrl = "http://$serviceId/v1/api-docs"
        logger.info("Fetching docs from: $docsUrl")

        return webClient.get()
            .uri(docsUrl)
            .retrieve()
            .bodyToMono(Map::class.java)
            .cast(Map::class.java)
            .map { it as Map<String, Any> }
            .onErrorResume { error ->
                logger.warn("Failed to fetch docs for $serviceId: ${error.message}")
                Mono.just(emptyMap())
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mergePathItems(
        baseOpenAPI: OpenAPI,
        serviceDocs: Map<String, Map<String, Any>>
    ): OpenAPI {
        val allPaths = mutableMapOf<String, PathItem>()

        serviceDocs.forEach { (serviceId, docs) ->
            val paths = docs["paths"] as? Map<String, Map<String, Any>> ?: emptyMap()

            paths.forEach { (path, pathItem) ->
                // Добавляем префикс сервиса к пути
                val gatewayPath = "/$serviceId$path"
                logger.info("Adding path: $gatewayPath")

                // Преобразуем в PathItem
                val item = PathItem()

                (pathItem["get"] as? Map<String, Any>)?.let {
                    item.get(convertToOperation(it))
                }
                (pathItem["post"] as? Map<String, Any>)?.let {
                    item.post(convertToOperation(it))
                }
                (pathItem["put"] as? Map<String, Any>)?.let {
                    item.put(convertToOperation(it))
                }
                (pathItem["delete"] as? Map<String, Any>)?.let {
                    item.delete(convertToOperation(it))
                }
                (pathItem["patch"] as? Map<String, Any>)?.let {
                    item.patch(convertToOperation(it))
                }

                if (item.readOperations().isNotEmpty()) {
                    allPaths[gatewayPath] = item
                }
            }
        }

        baseOpenAPI.paths(
            io.swagger.v3.oas.models.Paths().apply {
                allPaths.forEach { (path, item) ->
                    addPathItem(path, item)
                }
            }
        )

        return baseOpenAPI
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertToOperation(operationMap: Map<String, Any>): Operation {
        val operation = Operation()

        // Summary
        (operationMap["summary"] as? String)?.let { operation.summary(it) }

        // Description
        (operationMap["description"] as? String)?.let { operation.description(it) }

        // Tags
        (operationMap["tags"] as? List<String>)?.let { operation.tags(it) }

        // Operation ID
        (operationMap["operationId"] as? String)?.let { operation.operationId(it) }

        // Responses
        val responses = ApiResponses()
        (operationMap["responses"] as? Map<String, Map<String, Any>>)?.forEach { (code, response) ->
            val apiResponse = ApiResponse()
            (response["description"] as? String)?.let { apiResponse.description(it) }
            responses.addApiResponse(code, apiResponse)
        }
        if (responses.isNotEmpty()) {
            operation.responses(responses)
        } else {
            operation.responses(
                ApiResponses().addApiResponse(
                    "200",
                    ApiResponse().description("Success")
                )
            )
        }

        return operation
    }
}