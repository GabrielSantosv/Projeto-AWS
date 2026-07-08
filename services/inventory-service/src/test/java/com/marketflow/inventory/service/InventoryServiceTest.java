package com.marketflow.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketflow.inventory.domain.Product;
import com.marketflow.inventory.domain.ReservationStatus;
import com.marketflow.inventory.domain.StockReservation;
import com.marketflow.inventory.event.EventEnvelope;
import com.marketflow.inventory.event.EventType;
import com.marketflow.inventory.event.dto.OrderItem;
import com.marketflow.inventory.event.publisher.EventPublisher;
import com.marketflow.inventory.repository.ProductRepository;
import com.marketflow.inventory.repository.StockReservationRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockReservationRepository reservationRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void reserveStockPublishesStockReservedWhenAllItemsAreAvailable() {
        Product product = product("SKU-1", 10, 2);
        OrderItem item = new OrderItem(product.getId(), 3);
        when(productRepository.findByIdForUpdate(product.getId())).thenReturn(Optional.of(product));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.reserveStock("order-1", "corr-1", List.of(item));

        assertThat(product.getQuantityReserved()).isEqualTo(3);
        assertThat(product.getQuantityAvailable()).isEqualTo(7);
        ArgumentCaptor<EventEnvelope<?>> eventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(EventType.STOCK_RESERVED);
    }

    @Test
    void reserveStockRollsBackPreviousReservationsWhenLaterItemHasInsufficientStock() {
        Product firstProduct = product("SKU-1", 10, 2);
        Product secondProduct = product("SKU-2", 1, 1);
        OrderItem firstItem = new OrderItem(firstProduct.getId(), 4);
        OrderItem secondItem = new OrderItem(secondProduct.getId(), 2);
        when(productRepository.findByIdForUpdate(firstProduct.getId())).thenReturn(Optional.of(firstProduct));
        when(productRepository.findByIdForUpdate(secondProduct.getId())).thenReturn(Optional.of(secondProduct));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.reserveStock("order-1", "corr-1", List.of(firstItem, secondItem));

        assertThat(firstProduct.getQuantityReserved()).isZero();
        ArgumentCaptor<EventEnvelope<?>> eventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(EventType.STOCK_RESERVATION_FAILED);
    }

    @Test
    void reserveStockPublishesFailureWhenProductDoesNotExist() {
        Product product = product("SKU-1", 10, 2);
        OrderItem missingItem = new OrderItem(product.getId(), 1);
        when(productRepository.findByIdForUpdate(product.getId())).thenReturn(Optional.empty());

        inventoryService.reserveStock("order-1", "corr-1", List.of(missingItem));

        ArgumentCaptor<EventEnvelope<?>> eventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(EventType.STOCK_RESERVATION_FAILED);
    }

    @Test
    void confirmStockIsIdempotentAndDoesNotDeductTwice() {
        Product product = product("SKU-1", 10, 2);
        product.reserve(3);
        StockReservation reservation = new StockReservation("order-1", product, 3);
        when(reservationRepository.findByOrderIdAndStatus("order-1", ReservationStatus.RESERVED))
                .thenReturn(List.of(reservation))
                .thenReturn(List.of());
        when(productRepository.findByIdForUpdate(product.getId())).thenReturn(Optional.of(product));

        inventoryService.confirmStock("order-1", "corr-1");
        inventoryService.confirmStock("order-1", "corr-1");

        assertThat(product.getQuantityOnHand()).isEqualTo(7);
        assertThat(product.getQuantityReserved()).isZero();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void releaseReservationReturnsReservedQuantityToAvailability() {
        Product product = product("SKU-1", 10, 2);
        product.reserve(3);
        StockReservation reservation = new StockReservation("order-1", product, 3);
        when(reservationRepository.findByOrderIdAndStatus("order-1", ReservationStatus.RESERVED))
                .thenReturn(List.of(reservation));
        when(productRepository.findByIdForUpdate(product.getId())).thenReturn(Optional.of(product));

        inventoryService.releaseReservation("order-1", "corr-1", "Billing rejected");

        assertThat(product.getQuantityOnHand()).isEqualTo(10);
        assertThat(product.getQuantityReserved()).isZero();
        assertThat(product.getQuantityAvailable()).isEqualTo(10);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void adjustStockRejectsWriteOffBelowReservedQuantity() {
        Product product = product("SKU-1", 10, 2);
        product.reserve(7);
        when(productRepository.findByIdForUpdate(product.getId())).thenReturn(Optional.of(product));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> inventoryService.adjustStock(product.getId(), -4))
                .isInstanceOf(IllegalArgumentException.class);

        verify(eventPublisher, never()).publish(any());
    }

    private static Product product(String sku, int quantityOnHand, int lowStockThreshold) {
        return new Product(sku, "Product " + sku, BigDecimal.TEN, quantityOnHand, lowStockThreshold);
    }
}
