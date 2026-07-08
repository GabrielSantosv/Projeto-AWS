package com.marketflow.inventory.service;

import com.marketflow.inventory.domain.Product;
import com.marketflow.inventory.domain.ReservationStatus;
import com.marketflow.inventory.domain.StockReservation;
import com.marketflow.inventory.event.EventEnvelope;
import com.marketflow.inventory.event.EventType;
import com.marketflow.inventory.event.dto.OrderItem;
import com.marketflow.inventory.event.dto.StockConfirmedPayload;
import com.marketflow.inventory.event.dto.StockLowDetectedPayload;
import com.marketflow.inventory.event.dto.StockReservationFailedPayload;
import com.marketflow.inventory.event.dto.StockReservationReleasedPayload;
import com.marketflow.inventory.event.dto.StockReservedPayload;
import com.marketflow.inventory.event.publisher.EventPublisher;
import com.marketflow.inventory.exception.ProductNotFoundException;
import com.marketflow.inventory.repository.ProductRepository;
import com.marketflow.inventory.repository.StockReservationRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockReservationRepository reservationRepository;
    private final EventPublisher eventPublisher;

    public InventoryService(
            ProductRepository productRepository,
            StockReservationRepository reservationRepository,
            EventPublisher eventPublisher
    ) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Product createProduct(String sku, String name, BigDecimal unitPrice, int quantityOnHand, int lowStockThreshold) {
        if (productRepository.existsBySku(sku)) {
            throw new IllegalArgumentException("Product SKU already exists: " + sku);
        }
        return productRepository.save(new Product(sku, name, unitPrice, quantityOnHand, lowStockThreshold));
    }

    @Transactional(readOnly = true)
    public Product getProduct(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Product> listProducts() {
        return productRepository.findAll();
    }

    @Transactional
    public void reserveStock(String orderId, String correlationId, List<OrderItem> items) {
        List<StockReservation> createdReservations = new ArrayList<>();
        String failureReason = null;

        for (OrderItem item : items) {
            Product product = productRepository.findByIdForUpdate(item.productId()).orElse(null);
            if (product == null) {
                failureReason = "Product not found: " + item.productId();
                break;
            }
            if (!product.canReserve(item.quantity())) {
                failureReason = "Insufficient stock for product: " + item.productId();
                break;
            }

            product.reserve(item.quantity());
            StockReservation reservation = reservationRepository.save(new StockReservation(orderId, product, item.quantity()));
            createdReservations.add(reservation);
        }

        if (failureReason != null) {
            releaseCreatedReservations(createdReservations);
            eventPublisher.publish(EventEnvelope.create(
                    EventType.STOCK_RESERVATION_FAILED,
                    orderId,
                    correlationId,
                    new StockReservationFailedPayload(orderId, failureReason, items)
            ));
            return;
        }

        eventPublisher.publish(EventEnvelope.create(
                EventType.STOCK_RESERVED,
                orderId,
                correlationId,
                new StockReservedPayload(orderId, items)
        ));
        createdReservations.forEach(reservation -> checkLowStock(orderId, correlationId, reservation.getProduct()));
    }

    @Transactional
    public void confirmStock(String orderId, String correlationId) {
        List<StockReservation> reservations = reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);
        List<OrderItem> confirmedItems = new ArrayList<>();

        for (StockReservation reservation : reservations) {
            Product product = productRepository.findByIdForUpdate(reservation.getProduct().getId())
                    .orElseThrow(() -> new ProductNotFoundException(reservation.getProduct().getId()));
            product.confirmReserved(reservation.getQuantity());
            reservation.confirm();
            confirmedItems.add(new OrderItem(product.getId(), reservation.getQuantity()));
        }

        eventPublisher.publish(EventEnvelope.create(
                EventType.STOCK_CONFIRMED,
                orderId,
                correlationId,
                new StockConfirmedPayload(orderId, confirmedItems)
        ));
    }

    @Transactional
    public void releaseReservation(String orderId, String correlationId, String reason) {
        List<StockReservation> reservations = reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);
        List<OrderItem> releasedItems = new ArrayList<>();

        for (StockReservation reservation : reservations) {
            Product product = productRepository.findByIdForUpdate(reservation.getProduct().getId())
                    .orElseThrow(() -> new ProductNotFoundException(reservation.getProduct().getId()));
            product.releaseReserved(reservation.getQuantity());
            reservation.release();
            releasedItems.add(new OrderItem(product.getId(), reservation.getQuantity()));
        }

        eventPublisher.publish(EventEnvelope.create(
                EventType.STOCK_RESERVATION_RELEASED,
                orderId,
                correlationId,
                new StockReservationReleasedPayload(orderId, reason, releasedItems)
        ));
    }

    @Transactional
    public Product adjustStock(UUID productId, int delta) {
        Product product = productRepository.findByIdForUpdate(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        product.adjustStock(delta);
        checkLowStock(productId.toString(), UUID.randomUUID().toString(), product);
        return product;
    }

    private void releaseCreatedReservations(List<StockReservation> reservations) {
        for (StockReservation reservation : reservations) {
            reservation.getProduct().releaseReserved(reservation.getQuantity());
            reservation.release();
        }
    }

    private void checkLowStock(String sagaId, String correlationId, Product product) {
        if (product.getQuantityAvailable() <= product.getLowStockThreshold()) {
            eventPublisher.publish(EventEnvelope.create(
                    EventType.STOCK_LOW_DETECTED,
                    sagaId,
                    correlationId,
                    new StockLowDetectedPayload(
                            product.getId(),
                            product.getSku(),
                            product.getName(),
                            product.getQuantityAvailable(),
                            product.getLowStockThreshold()
                    )
            ));
        }
    }
}
