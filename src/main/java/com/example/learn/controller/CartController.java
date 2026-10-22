package com.example.learn.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.learn.entity.User;
import com.example.learn.repository.UserRepository;
import com.example.learn.service.CartService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(
        origins = "http://localhost:5174",
        allowCredentials = "true"
)
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;


    // =========================================================
    // GET CART ITEM COUNT
    // =========================================================

    @GetMapping("/items/count")
    public ResponseEntity<Integer> getCartItemCount(
            @RequestParam String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found with username: " + username));

        int count = cartService.getCartItemCount(
                user.getUserId()
        );

        return ResponseEntity.ok(count);
    }


    // =========================================================
    // GET ALL CART ITEMS
    // =========================================================

    @GetMapping("/items")
    public ResponseEntity<Map<String, Object>> getCartItems(
            HttpServletRequest request) {

        User user =
                (User) request.getAttribute("authenticatedUser");

        if (user == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        Map<String, Object> cartItems =
                cartService.getCartItems(
                        user.getUserId()
                );

        return ResponseEntity.ok(cartItems);
    }


    // =========================================================
    // ADD ITEM TO CART
    // =========================================================

    @PostMapping("/add")
    public ResponseEntity<String> addToCart(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {

        try {

            System.out.println("========== ADD TO CART START ==========");

            // Get authenticated user from token
            User user =
                    (User) httpRequest.getAttribute("authenticatedUser");

            System.out.println("Authenticated User: " + user);

            if (user == null) {

                System.out.println("User is NULL");

                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("User not authenticated");
            }

            System.out.println(
                    "User ID: " + user.getUserId()
            );

            System.out.println(
                    "Username: " + user.getUsername()
            );


            // Get product ID
            Object productIdObject =
                    request.get("productId");

            if (productIdObject == null) {

                System.out.println(
                        "Product ID is NULL"
                );

                return ResponseEntity
                        .badRequest()
                        .body("Product ID is required");
            }

            int productId =
                    ((Number) productIdObject).intValue();


            // Get quantity
            int quantity = 1;

            if (request.containsKey("quantity")
                    && request.get("quantity") != null) {

                quantity =
                        ((Number) request.get("quantity"))
                                .intValue();
            }


            System.out.println(
                    "Product ID: " + productId
            );

            System.out.println(
                    "Quantity: " + quantity
            );


            // Add product to cart
            cartService.addToCart(
                    user.getUserId(),
                    productId,
                    quantity
            );


            System.out.println(
                    "Product added successfully"
            );

            System.out.println(
                    "========== ADD TO CART END =========="
            );


            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(
                            "Product added to cart successfully"
                    );

        } catch (Exception e) {

            System.out.println(
                    "========== ADD TO CART ERROR =========="
            );

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            "Error: " + e.getMessage()
                    );
        }
    }


    // =========================================================
    // UPDATE CART ITEM QUANTITY
    // =========================================================

    @PutMapping("/update")
    public ResponseEntity<Void> updateCartItemQuantity(
            @RequestBody Map<String, Object> request) {

        String username =
                (String) request.get("username");

        int productId =
                ((Number) request.get("productId"))
                        .intValue();

        int quantity =
                ((Number) request.get("quantity"))
                        .intValue();


        User user =
                userRepository.findByUsername(username)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found with username: "
                                                + username
                                ));


        cartService.updateCartItemQuantity(
                user.getUserId(),
                productId,
                quantity
        );


        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }


    // =========================================================
    // DELETE CART ITEM
    // =========================================================

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteCartItem(
            @RequestBody Map<String, Object> request) {

        String username =
                (String) request.get("username");

        int productId =
                ((Number) request.get("productId"))
                        .intValue();


        User user =
                userRepository.findByUsername(username)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found with username: "
                                                + username
                                ));


        cartService.deleteCartItem(
                user.getUserId(),
                productId
        );


        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}