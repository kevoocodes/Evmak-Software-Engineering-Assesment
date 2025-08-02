package com.evmak.parking_management.service;

import com.evmak.parking_management.entity.Payment;
import com.evmak.parking_management.entity.ParkingSession;
import com.evmak.parking_management.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${x-payment-provider.api.url:https://api.x-payment-provider.com}")
    private String xPaymentApiUrl;

    @Value("${x-payment-provider.api.key:your-api-key-here}")
    private String xPaymentApiKey;

    @Value("${x-payment-provider.merchant.id:MERCHANT_001}")
    private String merchantId;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class PaymentRequest {
        public Long sessionId;
        public BigDecimal amount;
        public String currency = "TZS";
        public Payment.PaymentMethod paymentMethod;
        public String customerPhone;
        public String customerEmail;
        public String cardNumber;
        public String cardExpiry;
        public String cardCvv;
        public String mobileMoneyProvider; // VODACOM, AIRTEL, TIGO, HALOTEL

        // Getters and setters
        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public Payment.PaymentMethod getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(Payment.PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

        public String getCustomerPhone() { return customerPhone; }
        public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

        public String getCustomerEmail() { return customerEmail; }
        public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

        public String getCardExpiry() { return cardExpiry; }
        public void setCardExpiry(String cardExpiry) { this.cardExpiry = cardExpiry; }

        public String getCardCvv() { return cardCvv; }
        public void setCardCvv(String cardCvv) { this.cardCvv = cardCvv; }

        public String getMobileMoneyProvider() { return mobileMoneyProvider; }
        public void setMobileMoneyProvider(String mobileMoneyProvider) { this.mobileMoneyProvider = mobileMoneyProvider; }
    }

    public static class PaymentResult {
        public final boolean success;
        public final String message;
        public final Payment payment;
        public final String errorCode;
        public final String transactionId;

        public PaymentResult(boolean success, String message, Payment payment, String errorCode, String transactionId) {
            this.success = success;
            this.message = message;
            this.payment = payment;
            this.errorCode = errorCode;
            this.transactionId = transactionId;
        }

        public static PaymentResult success(Payment payment, String message, String transactionId) {
            return new PaymentResult(true, message, payment, null, transactionId);
        }

        public static PaymentResult failure(String message, String errorCode) {
            return new PaymentResult(false, message, null, errorCode, null);
        }
    }

    @Transactional
    public PaymentResult initiatePayment(ParkingSession session, PaymentRequest request) {
        try {
            // Create local payment record
            Payment payment = new Payment(session, request.getAmount(), request.getPaymentMethod());
            payment.setCurrency(request.getCurrency());
            payment.setPaymentProvider("X-PAYMENT-PROVIDER");
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment = paymentRepository.save(payment);

            // Call X-PAYMENT-PROVIDER API based on payment method
            PaymentResult result;
            if (request.getPaymentMethod() == Payment.PaymentMethod.CARD) {
                result = processCardPayment(payment, request);
            } else if (request.getPaymentMethod() == Payment.PaymentMethod.MOBILE_MONEY) {
                result = processMobileMoneyPayment(payment, request);
            } else {
                return PaymentResult.failure("Unsupported payment method", "UNSUPPORTED_METHOD");
            }

            // Update payment status based on result
            if (result.success) {
                payment.setExternalPaymentId(result.transactionId);
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                payment.setCompletedAt(LocalDateTime.now());
            } else {
                payment.setStatus(Payment.PaymentStatus.FAILED);
            }
            
            payment = paymentRepository.save(payment);
            return new PaymentResult(result.success, result.message, payment, result.errorCode, result.transactionId);

        } catch (Exception e) {
            return PaymentResult.failure("Payment processing failed: " + e.getMessage(), "PROCESSING_ERROR");
        }
    }

    private PaymentResult processCardPayment(Payment payment, PaymentRequest request) {
        try {
            // Prepare X-PAYMENT-PROVIDER card payment payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("merchant_id", merchantId);
            payload.put("payment_reference", payment.getPaymentReference());
            payload.put("amount", request.getAmount().toString());
            payload.put("currency", request.getCurrency());
            payload.put("payment_method", "CARD");
            
            Map<String, Object> cardData = new HashMap<>();
            cardData.put("card_number", request.getCardNumber());
            cardData.put("expiry_date", request.getCardExpiry());
            cardData.put("cvv", request.getCardCvv());
            payload.put("card_details", cardData);
            
            Map<String, Object> customer = new HashMap<>();
            customer.put("email", request.getCustomerEmail());
            customer.put("phone", request.getCustomerPhone());
            payload.put("customer", customer);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + xPaymentApiKey);
            headers.set("X-API-Version", "2024-01");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            // Call X-PAYMENT-PROVIDER card payment endpoint
            ResponseEntity<Map> response = restTemplate.postForEntity(
                xPaymentApiUrl + "/v1/payments/card", entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if ("success".equals(responseBody.get("status"))) {
                    String transactionId = (String) responseBody.get("transaction_id");
                    return PaymentResult.success(payment, "Card payment processed successfully", transactionId);
                } else {
                    String errorMessage = (String) responseBody.get("message");
                    String errorCode = (String) responseBody.get("error_code");
                    return PaymentResult.failure(errorMessage, errorCode);
                }
            } else {
                return PaymentResult.failure("Payment gateway error", "GATEWAY_ERROR");
            }

        } catch (Exception e) {
            return PaymentResult.failure("Card payment failed: " + e.getMessage(), "CARD_PAYMENT_ERROR");
        }
    }

    private PaymentResult processMobileMoneyPayment(Payment payment, PaymentRequest request) {
        try {
            // Prepare X-PAYMENT-PROVIDER mobile money payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("merchant_id", merchantId);
            payload.put("payment_reference", payment.getPaymentReference());
            payload.put("amount", request.getAmount().toString());
            payload.put("currency", request.getCurrency());
            payload.put("payment_method", "MOBILE_MONEY");
            payload.put("provider", request.getMobileMoneyProvider());
            payload.put("customer_phone", request.getCustomerPhone());

            Map<String, Object> customer = new HashMap<>();
            customer.put("phone", request.getCustomerPhone());
            if (request.getCustomerEmail() != null) {
                customer.put("email", request.getCustomerEmail());
            }
            payload.put("customer", customer);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + xPaymentApiKey);
            headers.set("X-API-Version", "2024-01");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            // Call X-PAYMENT-PROVIDER mobile money endpoint
            ResponseEntity<Map> response = restTemplate.postForEntity(
                xPaymentApiUrl + "/v1/payments/mobile-money", entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if ("success".equals(responseBody.get("status"))) {
                    String transactionId = (String) responseBody.get("transaction_id");
                    return PaymentResult.success(payment, "Mobile money payment initiated successfully", transactionId);
                } else {
                    String errorMessage = (String) responseBody.get("message");
                    String errorCode = (String) responseBody.get("error_code");
                    return PaymentResult.failure(errorMessage, errorCode);
                }
            } else {
                return PaymentResult.failure("Payment gateway error", "GATEWAY_ERROR");
            }

        } catch (Exception e) {
            return PaymentResult.failure("Mobile money payment failed: " + e.getMessage(), "MOBILE_MONEY_ERROR");
        }
    }

    @Transactional
    public PaymentResult verifyPayment(String paymentReference) {
        try {
            Optional<Payment> paymentOpt = paymentRepository.findByPaymentReference(paymentReference);
            if (paymentOpt.isEmpty()) {
                return PaymentResult.failure("Payment not found", "PAYMENT_NOT_FOUND");
            }

            Payment payment = paymentOpt.get();

            // Call X-PAYMENT-PROVIDER verification endpoint
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + xPaymentApiKey);
            headers.set("X-API-Version", "2024-01");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                xPaymentApiUrl + "/v1/payments/" + payment.getExternalPaymentId() + "/verify",
                HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                String status = (String) responseBody.get("status");
                
                // Update payment status based on verification result
                switch (status) {
                    case "completed":
                        payment.setStatus(Payment.PaymentStatus.COMPLETED);
                        payment.setCompletedAt(LocalDateTime.now());
                        break;
                    case "failed":
                        payment.setStatus(Payment.PaymentStatus.FAILED);
                        break;
                    case "pending":
                        payment.setStatus(Payment.PaymentStatus.PENDING);
                        break;
                    default:
                        payment.setStatus(Payment.PaymentStatus.FAILED);
                }

                payment = paymentRepository.save(payment);
                return PaymentResult.success(payment, "Payment verification completed", payment.getExternalPaymentId());
            }

            return PaymentResult.failure("Verification failed", "VERIFICATION_ERROR");

        } catch (Exception e) {
            return PaymentResult.failure("Payment verification failed: " + e.getMessage(), "VERIFICATION_ERROR");
        }
    }

    public PaymentResult refundPayment(String paymentReference, BigDecimal refundAmount) {
        try {
            Optional<Payment> paymentOpt = paymentRepository.findByPaymentReference(paymentReference);
            if (paymentOpt.isEmpty()) {
                return PaymentResult.failure("Payment not found", "PAYMENT_NOT_FOUND");
            }

            Payment payment = paymentOpt.get();

            if (!payment.isCompleted()) {
                return PaymentResult.failure("Cannot refund incomplete payment", "PAYMENT_NOT_COMPLETED");
            }

            if (refundAmount.compareTo(payment.getAmount()) > 0) {
                return PaymentResult.failure("Refund amount exceeds payment amount", "INVALID_REFUND_AMOUNT");
            }

            // Prepare refund payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("merchant_id", merchantId);
            payload.put("original_payment_id", payment.getExternalPaymentId());
            payload.put("refund_amount", refundAmount.toString());
            payload.put("currency", payment.getCurrency());
            payload.put("reason", "Customer requested refund");

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + xPaymentApiKey);
            headers.set("X-API-Version", "2024-01");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            // Call X-PAYMENT-PROVIDER refund endpoint
            ResponseEntity<Map> response = restTemplate.postForEntity(
                xPaymentApiUrl + "/v1/payments/refund", entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if ("success".equals(responseBody.get("status"))) {
                    payment.setStatus(Payment.PaymentStatus.REFUNDED);
                    payment = paymentRepository.save(payment);
                    
                    String refundId = (String) responseBody.get("refund_id");
                    return PaymentResult.success(payment, "Refund processed successfully", refundId);
                }
            }

            return PaymentResult.failure("Refund processing failed", "REFUND_ERROR");

        } catch (Exception e) {
            return PaymentResult.failure("Refund failed: " + e.getMessage(), "REFUND_ERROR");
        }
    }
}