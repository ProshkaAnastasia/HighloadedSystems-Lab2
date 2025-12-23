package ru.itmo.productservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.get
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import ru.itmo.productservice.model.dto.request.CreateShopRequest
import ru.itmo.productservice.repository.ShopRepository

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Shop Controller Integration Tests")
class ShopControllerIntegrationTest {
    
    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15").apply {
            withDatabaseName("itmo_market_test")
            withUsername("itmo_user")
            withPassword("itmo_password")
        }
    }
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @Autowired
    private lateinit var shopRepository: ShopRepository
    
    @BeforeEach
    fun setUp() {
        shopRepository.deleteAll()
    }
    
    @Test
    @DisplayName("Should create shop successfully")
    fun testCreateShop() {
        val request = CreateShopRequest(
            name = "My Shop",
            description = "My description"
        )
        
        mockMvc.post("/api/shops?sellerId=1") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
        }
    }
    
    @Test
    @DisplayName("Should get all shops")
    fun testGetAllShops() {
        mockMvc.get("/api/shops?page=1&pageSize=20")
            .andExpect {
                status { isOk() }
            }
    }
}
