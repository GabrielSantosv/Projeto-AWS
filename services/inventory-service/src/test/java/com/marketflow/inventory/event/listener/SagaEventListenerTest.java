package com.marketflow.inventory.event.listener;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.inventory.repository.ProcessedEventRepository;
import com.marketflow.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;

class SagaEventListenerTest {

    private final SqsClient sqsClient = org.mockito.Mockito.mock(SqsClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final InventoryService inventoryService = org.mockito.Mockito.mock(InventoryService.class);
    private final ProcessedEventRepository processedEventRepository = org.mockito.Mockito.mock(ProcessedEventRepository.class);
    private final SagaEventListener listener = new SagaEventListener(
            sqsClient,
            objectMapper,
            inventoryService,
            processedEventRepository
    );

    @Test
    void processMessageIgnoresAlreadyProcessedEvent() throws Exception {
        String body = """
                {
                  "eventId": "evt-1",
                  "eventType": "EmployeeValidated",
                  "sagaId": "order-1",
                  "correlationId": "corr-1",
                  "timestamp": "2026-07-08T18:00:00Z",
                  "version": 1,
                  "payload": {
                    "orderId": "order-1",
                    "employeeId": "employee-1",
                    "items": []
                  }
                }
                """;
        when(processedEventRepository.existsById("evt-1")).thenReturn(true);

        listener.processMessage(body);

        verify(inventoryService, never()).reserveStock(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(processedEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
