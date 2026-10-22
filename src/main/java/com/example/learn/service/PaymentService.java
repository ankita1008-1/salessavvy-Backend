package com.example.learn.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.learn.entity.CartItem;
import com.example.learn.entity.Order;
import com.example.learn.entity.OrderItem;
import com.example.learn.entity.OrderStatus;
import com.example.learn.repository.CartRepository;
import com.example.learn.repository.OrderItemRepository;
import com.example.learn.repository.OrderRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

@Service
public class PaymentService {

    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;

    public PaymentService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CartRepository cartRepository) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
    }

    // Create Razorpay Order
    @Transactional
    public String createOrder(
            int userId,
            BigDecimal totalAmount,
            List<OrderItem> cartItems) throws RazorpayException {

        if (totalAmount == null ||
                totalAmount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Invalid total amount");
        }

        if (cartItems == null || cartItems.isEmpty()) {

            throw new IllegalArgumentException(
                    "Cart is empty");
        }

        // Create Razorpay client
        RazorpayClient razorpayClient =
                new RazorpayClient(
                        razorpayKeyId,
                        razorpayKeySecret
                );

        // Convert INR to paise
        long amountInPaise =
                totalAmount
                        .multiply(BigDecimal.valueOf(100))
                        .longValue();

        // Prepare Razorpay order request
        JSONObject orderRequest = new JSONObject();

        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put(
                "receipt",
                "txn_" + System.currentTimeMillis()
        );

        // Create Razorpay order
        com.razorpay.Order razorpayOrder =
                razorpayClient.orders.create(orderRequest);

        String razorpayOrderId =
                razorpayOrder.get("id").toString();

        // Save order in database
        Order order = new Order();

        order.setOrderId(razorpayOrderId);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        orderRepository.save(order);

        return razorpayOrderId;
    }

    // Verify Razorpay Payment
    @Transactional
    public boolean verifyPayment(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature,
            int userId) {

        try {

            if (razorpayOrderId == null ||
                    razorpayPaymentId == null ||
                    razorpaySignature == null) {

                return false;
            }

            // Prepare signature validation
            JSONObject attributes = new JSONObject();

            attributes.put(
                    "razorpay_order_id",
                    razorpayOrderId
            );

            attributes.put(
                    "razorpay_payment_id",
                    razorpayPaymentId
            );

            attributes.put(
                    "razorpay_signature",
                    razorpaySignature
            );

            // Verify Razorpay signature
            boolean isSignatureValid =
                    Utils.verifyPaymentSignature(
                            attributes,
                            razorpayKeySecret
                    );

            if (!isSignatureValid) {
                return false;
            }

            // Find order
            Order order =
                    orderRepository.findById(razorpayOrderId)
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Order not found"
                                    )
                            );

            // Check user
            if (order.getUserId() != userId) {
                throw new RuntimeException(
                        "Order does not belong to this user"
                );
            }

            // Update order status
            order.setStatus(OrderStatus.SUCCESS);
            order.setUpdatedAt(LocalDateTime.now());

            orderRepository.save(order);

            // Fetch user's cart items
            List<CartItem> cartItems =
                    cartRepository.findCartItemsWithProductDetails(
                            userId
                    );

            // Save order items
            for (CartItem cartItem : cartItems) {

                OrderItem orderItem =
                        new OrderItem();

                orderItem.setOrder(order);

                orderItem.setProductId(
                        cartItem.getProduct().getProductId()
                );

                orderItem.setQuantity(
                        cartItem.getQuantity()
                );

                orderItem.setPricePerUnit(
                        cartItem.getProduct().getPrice()
                );

                BigDecimal totalPrice =
                        cartItem.getProduct()
                                .getPrice()
                                .multiply(
                                        BigDecimal.valueOf(
                                                cartItem.getQuantity()
                                        )
                                );

                orderItem.setTotalPrice(totalPrice);

                orderItemRepository.save(orderItem);
            }

            // Clear user's cart
            cartRepository.deleteAllCartItemsByUserId(userId);

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    // Save order items
    @Transactional
    public void saveOrderItems(
            String orderId,
            List<OrderItem> items) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Order not found"
                                )
                        );

        for (OrderItem item : items) {

            item.setOrder(order);

            orderItemRepository.save(item);
        }
    }
}