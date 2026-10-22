package com.example.learn.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.learn.entity.OrderItem;
import com.example.learn.entity.User;
import com.example.learn.service.PaymentService;
import com.razorpay.RazorpayException;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(
        origins = "http://localhost:5174",
        allowCredentials = "true"
)
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // Create Razorpay Order
    @PostMapping("/create")
    public ResponseEntity<String> createPaymentOrder(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {

        try {

            // Get authenticated user
            User user =
                    (User) request.getAttribute("authenticatedUser");

            if (user == null) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("User not authenticated");
            }

            // Get total amount
            Object totalAmountObject =
                    requestBody.get("totalAmount");

            if (totalAmountObject == null) {
                return ResponseEntity
                        .badRequest()
                        .body("Total amount is required");
            }

            BigDecimal totalAmount =
                    new BigDecimal(totalAmountObject.toString());

            // Get cart items
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cartItemsRaw =
                    (List<Map<String, Object>>) requestBody.get("cartItems");

            if (cartItemsRaw == null || cartItemsRaw.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body("Cart is empty");
            }

            // Convert cart items to OrderItem objects
            List<OrderItem> cartItems =
                    cartItemsRaw.stream()
                            .map(item -> {

                                OrderItem orderItem =
                                        new OrderItem();

                                int productId =
                                        ((Number) item.get("productId"))
                                                .intValue();

                                int quantity =
                                        ((Number) item.get("quantity"))
                                                .intValue();

                                BigDecimal pricePerUnit =
                                        new BigDecimal(
                                                item.get("price")
                                                        .toString()
                                        );

                                orderItem.setProductId(productId);
                                orderItem.setQuantity(quantity);
                                orderItem.setPricePerUnit(
                                        pricePerUnit
                                );

                                BigDecimal totalPrice =
                                        pricePerUnit.multiply(
                                                BigDecimal.valueOf(
                                                        quantity
                                                )
                                        );

                                orderItem.setTotalPrice(
                                        totalPrice
                                );

                                return orderItem;

                            })
                            .collect(Collectors.toList());

            // Create Razorpay order
            String razorpayOrderId =
                    paymentService.createOrder(
                            user.getUserId(),
                            totalAmount,
                            cartItems
                    );

            return ResponseEntity.ok(razorpayOrderId);

        } catch (RazorpayException e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            "Error creating Razorpay order: "
                                    + e.getMessage()
                    );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(
                            "Invalid request data: "
                                    + e.getMessage()
                    );
        }
    }

    // Verify Razorpay Payment
    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {

        try {

            // Get authenticated user
            User user =
                    (User) request.getAttribute("authenticatedUser");

            if (user == null) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("User not authenticated");
            }

            int userId = user.getUserId();

            // Get Razorpay details
            String razorpayOrderId =
                    (String) requestBody.get("razorpayOrderId");

            String razorpayPaymentId =
                    (String) requestBody.get("razorpayPaymentId");

            String razorpaySignature =
                    (String) requestBody.get("razorpaySignature");

            // Validate payment details
            if (razorpayOrderId == null ||
                    razorpayPaymentId == null ||
                    razorpaySignature == null) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "Missing Razorpay payment details"
                        );
            }

            // Verify payment
            boolean isVerified =
                    paymentService.verifyPayment(
                            razorpayOrderId,
                            razorpayPaymentId,
                            razorpaySignature,
                            userId
                    );

            if (isVerified) {

                return ResponseEntity
                        .ok("Payment verified successfully");

            } else {

                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(
                                "Payment verification failed"
                        );
            }

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            "Error verifying payment: "
                                    + e.getMessage()
                    );
        }
    }
}