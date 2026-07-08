package com.marketflow.inventory.event;

public final class EventType {

    public static final String ORDER_CREATED = "OrderCreated";
    public static final String EMPLOYEE_VALIDATED = "EmployeeValidated";
    public static final String EMPLOYEE_VALIDATION_FAILED = "EmployeeValidationFailed";
    public static final String STOCK_RESERVED = "StockReserved";
    public static final String STOCK_RESERVATION_FAILED = "StockReservationFailed";
    public static final String BILLING_APPROVED = "BillingApproved";
    public static final String BILLING_REJECTED = "BillingRejected";
    public static final String INVOICE_ISSUED = "InvoiceIssued";
    public static final String INVOICE_REJECTED = "InvoiceRejected";
    public static final String STOCK_CONFIRMED = "StockConfirmed";
    public static final String STOCK_RESERVATION_RELEASED = "StockReservationReleased";
    public static final String ORDER_COMPLETED = "OrderCompleted";
    public static final String ORDER_CANCELED = "OrderCanceled";
    public static final String NOTIFICATION_SENT = "NotificationSent";
    public static final String NOTIFICATION_FAILED = "NotificationFailed";
    public static final String STOCK_LOW_DETECTED = "StockLowDetected";
    public static final String PURCHASE_ORDER_CREATED = "PurchaseOrderCreated";

    private EventType() {
    }
}
