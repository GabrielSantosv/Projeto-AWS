package com.marketflow.inventory.controller;

import com.marketflow.inventory.controller.ProductDtos.AdjustStockRequest;
import com.marketflow.inventory.controller.ProductDtos.CreateProductRequest;
import com.marketflow.inventory.controller.ProductDtos.ProductResponse;
import com.marketflow.inventory.domain.Product;
import com.marketflow.inventory.service.InventoryService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final InventoryService inventoryService;

    public ProductController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = inventoryService.createProduct(
                request.sku(),
                request.name(),
                request.unitPrice(),
                request.quantityOnHand(),
                request.lowStockThreshold()
        );
        return ResponseEntity.created(URI.create("/products/" + product.getId())).body(toResponse(product));
    }

    @GetMapping("/{id}")
    ProductResponse getProduct(@PathVariable UUID id) {
        return toResponse(inventoryService.getProduct(id));
    }

    @GetMapping
    List<ProductResponse> listProducts() {
        return inventoryService.listProducts().stream()
                .map(ProductController::toResponse)
                .toList();
    }

    @PostMapping("/{id}/stock-adjustments")
    ProductResponse adjustStock(@PathVariable UUID id, @Valid @RequestBody AdjustStockRequest request) {
        return toResponse(inventoryService.adjustStock(id, request.delta()));
    }

    private static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getUnitPrice(),
                product.getQuantityOnHand(),
                product.getQuantityReserved(),
                product.getQuantityAvailable(),
                product.getLowStockThreshold()
        );
    }
}
