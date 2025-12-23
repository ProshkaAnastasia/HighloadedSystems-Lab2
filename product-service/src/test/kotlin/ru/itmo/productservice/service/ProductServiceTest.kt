package ru.itmo.productservice.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import ru.itmo.productservice.exception.BadRequestException
import ru.itmo.productservice.exception.ForbiddenException
import ru.itmo.productservice.exception.ResourceNotFoundException
import ru.itmo.productservice.model.dto.request.CreateProductRequest
import ru.itmo.productservice.model.dto.request.CreateShopRequest
import ru.itmo.productservice.model.enums.ProductStatus
import ru.itmo.productservice.repository.ProductRepository
import ru.itmo.productservice.repository.ShopRepository
import java.math.BigDecimal

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Product Service Tests")
class ProductServiceTest {
    
    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15").apply {
            withDatabaseName("itmo_market_test")
            withUsername("itmo_user")
            withPassword("itmo_password")
        }
    }
    
    @Autowired
    private lateinit var productService: ProductService
    
    @Autowired
    private lateinit var shopService: ShopService
    
    @Autowired
    private lateinit var productRepository: ProductRepository
    
    @Autowired
    private lateinit var shopRepository: ShopRepository
    
    @BeforeEach
    fun setUp() {
        productRepository.deleteAll()
        shopRepository.deleteAll()
    }
    
    @Test
    @DisplayName("Should create product successfully")
    fun testCreateProductSuccess() {
        // Создаем магазин
        val createShopRequest = CreateShopRequest(
            name = "Test Shop",
            description = "Test Description"
        )
        val shop = shopService.createShop(1L, createShopRequest)
        
        // Создаем товар
        val createProductRequest = CreateProductRequest(
            name = "Test Product",
            description = "Test Description",
            price = BigDecimal("100.00"),
            imageUrl = null,
            shopId = shop.id
        )
        
        val product = productService.createProduct(createProductRequest, 1L)
        
        assert(product.name == "Test Product")
        assert(product.status == ProductStatus.PENDING.name)
        assert(product.price == BigDecimal("100.00"))
    }
    
    @Test
    @DisplayName("Should fail creating product with invalid shop")
    fun testCreateProductInvalidShop() {
        val createProductRequest = CreateProductRequest(
            name = "Test Product",
            description = "Test Description",
            price = BigDecimal("100.00"),
            imageUrl = null,
            shopId = 999L
        )
        
        assertThrows<ResourceNotFoundException> {
            productService.createProduct(createProductRequest, 1L)
        }
    }
    
    @Test
    @DisplayName("Should get product by ID")
    fun testGetProductById() {
        val createShopRequest = CreateShopRequest(
            name = "Test Shop",
            description = null
        )
        val shop = shopService.createShop(1L, createShopRequest)
        
        val createProductRequest = CreateProductRequest(
            name = "Test Product",
            description = null,
            price = BigDecimal("50.00"),
            imageUrl = null,
            shopId = shop.id
        )
        
        val created = productService.createProduct(createProductRequest, 1L)
        val retrieved = productService.getProductById(created.id)
        
        assert(retrieved.id == created.id)
        assert(retrieved.name == "Test Product")
    }
}
