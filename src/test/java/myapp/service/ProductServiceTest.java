package myapp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import myapp.domain.Product;
import myapp.domain.enumeration.ProductStatus;
import myapp.repository.ProductRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    private static Validator validator;

    // execute one time before all tests
    @BeforeAll
    public static void setUpValidator() {
        //verify if the object is valid according to the annotations
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // mock to simulate the repository behavior
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService; // Injects the mock into the service

    // Helper method to create a sample product with flexible parameters
    public static Product createProductSample(
        Long id,
        String title,
        String keywords,
        String description,
        int rating,
        int quantityInStock,
        String dimensions,
        BigDecimal price,
        ProductStatus status,
        Double weight,
        Instant dateAdded
    ) {
        //Create a Product instance with given parameters
        Product product = new Product()
            .id(id)
            .title(title)
            .keywords(keywords)
            .description(description)
            .rating(rating)
            .quantityInStock(quantityInStock)
            .dimensions(dimensions)
            .price(price)
            .status(status)
            .weight(weight)
            .dateAdded(dateAdded);

        return product;
    }

    // BEGIN TEST CASES - (with example for Title)
    @Test
    public void testTitleEquivalencePartitionTitle() {
        //Valid case Title == 3 char
        Product productWithValidTitle = createProductSample(
            1L,
            "NES",
            null,
            null,
            1,
            1,
            null,
            BigDecimal.TEN,
            ProductStatus.IN_STOCK,
            null,
            Instant.now()
        );
        Set<ConstraintViolation<Product>> violations_valid = validator.validate(productWithValidTitle);
        // Assert
        System.err.println(violations_valid);
        assertTrue(violations_valid.isEmpty());
        when(productRepository.save(productWithValidTitle)).thenReturn(productWithValidTitle);
        Product savedProduct = productService.save(productWithValidTitle);
        assertEquals(productWithValidTitle, savedProduct);

        //Invalid case Title < 3 char
        Product productWithTwoCharTitle = createProductSample(
            1L,
            "NE",
            null,
            null,
            1,
            1,
            null,
            BigDecimal.TEN,
            ProductStatus.IN_STOCK,
            null,
            Instant.now()
        );
        Set<ConstraintViolation<Product>> violations_invalid = validator.validate(productWithTwoCharTitle);
        // Assert
        assertEquals("title", violations_invalid.iterator().next().getPropertyPath().toString());
    }
}
