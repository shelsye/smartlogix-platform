package com.smartlogix.order.controller;

import com.smartlogix.order.dto.*;
import com.smartlogix.order.security.AuthenticatedUser;
import com.smartlogix.order.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(Authentication authentication, @Valid @RequestBody CreateOrderRequest request) {
        AuthenticatedUser user = current(authentication);
        return service.createOrder(user.userId(), user.email(), request);
    }

    @PostMapping("/symbolic-external")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponse symbolicExternal(Authentication authentication, @Valid @RequestBody CreateSymbolicExternalOrderRequest request) {
        AuthenticatedUser user = current(authentication);
        return service.createSymbolicExternalOrder(user.userId(), user.email(), request);
    }

    @GetMapping("/my")
    public List<OrderResponse> myOrders(Authentication authentication) {
        return service.getMyOrders(current(authentication).userId());
    }

    @GetMapping("/coupon-status")
    public CouponStatusResponse couponStatus(Authentication authentication) {
        return service.couponStatus(current(authentication).userId());
    }

    @PutMapping("/my/{orderNumber}")
    public OrderResponse updateMyOrder(Authentication authentication, @PathVariable String orderNumber,
                                       @Valid @RequestBody UpdateCustomerOrderRequest request) {
        return service.updateMyOrder(current(authentication).userId(), orderNumber, request);
    }

    @DeleteMapping("/my/{orderNumber}")
    public OrderResponse cancelMyOrder(Authentication authentication, @PathVariable String orderNumber) {
        return service.cancelMyOrder(current(authentication).userId(), orderNumber);
    }

    @GetMapping("/my/{orderNumber}/receipt")
    public ReceiptResponse myReceipt(Authentication authentication, @PathVariable String orderNumber) {
        return service.getMyReceipt(current(authentication).userId(), orderNumber);
    }

    @GetMapping
    public List<OrderResponse> allOrders() { return service.getAllOrders(); }

    @GetMapping("/reports/dashboard")
    public DashboardReportResponse report() { return service.dashboardReport(); }

    @GetMapping("/{orderNumber}")
    public OrderResponse find(@PathVariable String orderNumber) { return service.getOrderByNumber(orderNumber); }

    @GetMapping("/{orderNumber}/receipt")
    public ReceiptResponse receipt(@PathVariable String orderNumber) { return service.getReceipt(orderNumber); }

    @PatchMapping("/{orderNumber}/status")
    public OrderResponse status(@PathVariable String orderNumber, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return service.updateStatus(orderNumber, request);
    }

    @DeleteMapping("/{orderNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String orderNumber) { service.deleteCancelledOrder(orderNumber); }

    private AuthenticatedUser current(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("No fue posible identificar al usuario autenticado.");
        }
        return user;
    }
}
