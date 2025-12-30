package in.codefarm.order.service.as.producer.controller;

import in.codefarm.order.service.as.producer.dto.PaymentResponse;
import in.codefarm.order.service.as.producer.service.PaymentEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    
    private final PaymentEventProducer paymentEventProducer;
    
    public PaymentController(PaymentEventProducer paymentEventProducer) {
        this.paymentEventProducer = paymentEventProducer;
    }
    
    @PostMapping("/process/non-transactional")
    public ResponseEntity<PaymentResponse> processPaymentNonTransactionally(
        @RequestBody PaymentRequest request
    ) {
        try {
            String transactionId = paymentEventProducer.processPaymentNonTransactionally(
                request.orderId(),
                request.customerId(),
                request.amount()
            );
            return ResponseEntity.ok(PaymentResponse.success(request.orderId(), transactionId));
        } catch (Exception e) {
            log.error("Non-transactional payment processing failed for order: {}", request.orderId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PaymentResponse.error("Payment processing failed: " + e.getMessage(), request.orderId()));
        }
    }
    
    @PostMapping("/process/transactional")
    public ResponseEntity<PaymentResponse> processPaymentTransactionally(
        @RequestBody PaymentRequest request
    ) {
        try {
            String transactionId = paymentEventProducer.processPaymentTransactionally(
                request.orderId(),
                request.customerId(),
                request.amount()
            );
            return ResponseEntity.ok(PaymentResponse.success(request.orderId(), transactionId));
        } catch (Exception e) {
            log.error("Transactional payment processing failed for order: {}", request.orderId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PaymentResponse.error("Payment processing failed: " + e.getMessage(), request.orderId()));
        }
    }
    
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(
        @RequestBody PaymentRequest request
    ) {
        try {
            String transactionId = paymentEventProducer.processPaymentTransactionally(
                request.orderId(),
                request.customerId(),
                request.amount()
            );
            return ResponseEntity.ok(PaymentResponse.success(request.orderId(), transactionId));
        } catch (Exception e) {
            log.error("Payment processing failed for order: {}", request.orderId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PaymentResponse.error("Payment processing failed: " + e.getMessage(), request.orderId()));
        }
    }
    
    public record PaymentRequest(
        String orderId,
        String customerId,
        BigDecimal amount
    ) {
    }
}

