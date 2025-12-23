package ru.itmo.productservice.repository

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import ru.itmo.productservice.model.entity.Product
import ru.itmo.productservice.model.entity.Shop
import ru.itmo.productservice.model.enums.ProductStatus
import java.math.BigDecimal

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Product Repository Tests")
class ProductRepositoryTest {
    
    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15").apply {
            withDatabaseName("itmo_market_test")
            withUsername("itmo_user")
            withPassword("itmo_password")
        }
    }
    
    @Autowired
    private lateinit var productRepository: ProductRepository
    
    @Autowired
    private lateinit var shopRepository: ShopRepository
    
    private lateinit var shop: Shop
    
    @BeforeEach
    fun setUp() {
        shop = Shop(
            name = "Test Shop",
            sellerId = 1L
        )
        shopRepository.save(shop)
    }
    
    @Test
    @DisplayName("Should find all products by shop ID")
    fun testFindByShopId() {
        val product1 = Product(
            name = "Product 1",
            price = BigDecimal("100"),
            shopId = shop.id,
            sellerId = 1L
        )
        val product2 = Product(
            name = "Product 2",
            price = BigDecimal("200"),
            shopId = shop.id,
            sellerId = 1L
        )
        
        productRepository.saveAll(listOf(product1, product2))
        
        val pageable = PageRequest.of(0, 20)
        val result = productRepository.findAllByShopId(shop.id, pageable)
        
        assert(result.content.size == 2)
    }
    
    @Test
    @DisplayName("Should search products by keyword")
    fun testSearchByKeyword() {
        val product = Product(
            name = "Amazing Laptop",
            description = "High performance laptop",
            price = BigDecimal("1000"),
            shopId = shop.id,
            sellerId = 1L,
            status = ProductStatus.APPROVED
        )
        
        productRepository.save(product)
        
        val pageable = PageRequest.of(0, 20)
        val result = productRepository.searchApprovedProducts("laptop", pageable)
        
        assert(result.content.size == 1)
    }
}
