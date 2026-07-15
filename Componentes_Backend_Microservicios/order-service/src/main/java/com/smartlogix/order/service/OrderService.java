package com.smartlogix.order.service;

import com.smartlogix.order.client.*;
import com.smartlogix.order.domain.*;
import com.smartlogix.order.dto.*;
import com.smartlogix.order.exception.OrderNotFoundException;
import com.smartlogix.order.exception.OrderProcessingException;
import com.smartlogix.order.repository.PurchaseOrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {
    private final PurchaseOrderRepository orderRepository;
    private final String welcomeCouponCode;
    private final InventoryClient inventoryClient;
    private final ShipmentClient shipmentClient;
    private final AuthClient authClient;

    public OrderService(PurchaseOrderRepository orderRepository,
                        InventoryClient inventoryClient,
                        ShipmentClient shipmentClient,
                        AuthClient authClient,
                        @org.springframework.beans.factory.annotation.Value("${smartlogix.welcome-coupon-code}") String welcomeCouponCode) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.shipmentClient = shipmentClient;
        this.authClient = authClient;
        this.welcomeCouponCode = welcomeCouponCode.trim().toUpperCase(Locale.ROOT);
    }

    public OrderResponse createOrder(Long customerUserId, String authenticatedEmail, CreateOrderRequest request) {
        String email = normalizeEmail(authenticatedEmail);
        validatePayment(request.payment());
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String coupon = normalizeCoupon(request.couponCode());
        UserCouponResponse couponDefinition = validateCoupon(customerUserId, coupon);
        boolean useCoupon = couponDefinition != null;
        int discountPercentage = useCoupon ? couponDefinition.discountPercentage() : 0;
        List<PreparedLine> prepared = prepareLines(request.lines());
        List<PreparedLine> reserved = new ArrayList<>();
        boolean shipmentCreated = false;

        try {
            reserveAll(prepared, reserved);
            int totalUnits = prepared.stream().mapToInt(PreparedLine::quantity).sum();
            ShipmentResponse shipment = shipmentClient.createFromSelection(new ShipmentSelectionRequest(
                    request.routeSelectionId().trim(), customerUserId, orderNumber,
                    request.shippingAddress().trim(), totalUnits));
            if (shipment == null || shipment.trackingCode() == null || shipment.price() == null) {
                throw new OrderProcessingException("El servicio de envíos no devolvió una planificación válida.");
            }
            shipmentCreated = true;

            PurchaseOrder order = new PurchaseOrder();
            order.setOrderNumber(orderNumber);
            order.setCustomerName(request.customerName().trim());
            order.setCustomerUserId(customerUserId);
            order.setCustomerEmail(email);
            order.setShippingAddress(request.shippingAddress().trim());
            order.setShippingRegion(shipment.region());
            order.setShippingType(shipment.routeType());
            order.setShippingCarrier(shipment.carrier());
            order.setShippingRouteName(shipment.routeName());
            order.setShippingRouteCode(shipment.routeCode());
            order.setShippingEstimatedDays(shipment.estimatedDays());
            order.setShippingDistanceKm(shipment.distanceKm());
            order.setShippingEstimatedDeliveryDate(shipment.estimatedDeliveryDate());
            order.setShippingPrice(shipment.price());
            order.setTrackingCode(shipment.trackingCode());
            order.setStatus(OrderStatus.PENDING);
            order.setCouponCode(useCoupon ? couponDefinition.code() : null);
            order.setDiscountApplied(useCoupon);
            order.setDiscountPercentage(discountPercentage);
            order.setStockFinalized(false);
            order.setStockReleased(false);
            applyLinesAndTotals(order, prepared, discountPercentage);
            attachPaymentAndReceipt(order, request.payment());

            PurchaseOrder saved = orderRepository.saveAndFlush(order);
            if (useCoupon) {
                authClient.consume(customerUserId, couponDefinition.code(), saved.getOrderNumber());
            }
            return toResponse(saved);
        } catch (RuntimeException ex) {
            releaseQuietly(reserved);
            if (shipmentCreated) rollbackShipmentQuietly(orderNumber);
            if (useCoupon) releaseCouponQuietly(customerUserId, couponDefinition.code(), orderNumber);
            if (ex instanceof OrderProcessingException) throw ex;
            throw new OrderProcessingException(cleanRemoteMessage(ex));
        }
    }

    public OrderResponse createSymbolicExternalOrder(Long customerUserId, String authenticatedEmail, CreateSymbolicExternalOrderRequest request) {
        String email = normalizeEmail(authenticatedEmail);
        validatePayment(request.payment());
        BigDecimal externalPrice = request.externalPrice().setScale(0, RoundingMode.HALF_UP);
        if (externalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderProcessingException("El precio externo debe ser mayor a 0.");
        }

        String orderNumber = "EXT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String store = safeText(request.externalStore(), "Tienda externa", 80);
        String seller = safeText(request.externalSeller(), "Proveedor externo", 80);
        String productName = safeText(request.externalProductName(), "Producto externo", 90);

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber(orderNumber);
        order.setCustomerName(safeText(request.customerName(), email, 120));
        order.setCustomerUserId(customerUserId);
        order.setCustomerEmail(email);
        order.setShippingAddress(safeText(request.shippingAddress(), "Compra simbólica generada desde SmartLogix Price Finder", 255));
        order.setShippingRegion(safeText(request.shippingRegion(), "Compra simbólica", 80));
        order.setShippingType("SIMBOLICO_EXTERNO");
        order.setShippingCarrier(store);
        order.setShippingRouteName("Comparador real multitienda · " + seller);
        order.setShippingRouteCode("PRICE-FINDER");
        order.setShippingEstimatedDays(0);
        order.setShippingDistanceKm(0);
        order.setShippingEstimatedDeliveryDate(LocalDate.now());
        order.setShippingPrice(BigDecimal.ZERO);
        order.setSubtotal(externalPrice);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(externalPrice);
        order.setStatus(OrderStatus.DELIVERED);
        order.setTrackingCode("SIMB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        order.setCouponCode(null);
        order.setDiscountApplied(false);
        order.setDiscountPercentage(0);
        order.setStockFinalized(true);
        order.setStockReleased(true);

        OrderLine line = new OrderLine();
        line.setSku("EXT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        line.setProductName(safeText(productName + " · " + store, productName, 120));
        line.setQuantity(1);
        line.setUnitPrice(externalPrice);
        order.addLine(line);

        attachPaymentAndReceipt(order, request.payment());
        return toResponse(orderRepository.saveAndFlush(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long customerUserId) {
        return orderRepository.findByCustomerUserIdOrderByCreatedAtDesc(customerUserId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        return toResponse(load(orderNumber));
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getMyReceipt(Long customerUserId, String orderNumber) {
        return toReceiptResponse(loadOwned(customerUserId, orderNumber));
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceipt(String orderNumber) {
        return toReceiptResponse(load(orderNumber));
    }

    @Transactional(readOnly = true)
    public CouponStatusResponse couponStatus(Long customerUserId) {
        UserCouponResponse coupon = authClient.couponStatus(customerUserId, welcomeCouponCode);
        return new CouponStatusResponse(coupon.code(), coupon.discountPercentage(), coupon.available(), coupon.message());
    }

    public OrderResponse updateMyOrder(Long customerUserId, String orderNumber, UpdateCustomerOrderRequest request) {
        PurchaseOrder order = loadOwned(customerUserId, orderNumber);
        requirePending(order);

        List<PreparedLine> oldLines = order.getLines().stream()
                .map(line -> new PreparedLine(new ProductSnapshotResponse(line.getSku(), line.getProductName(),
                        line.getUnitPrice(), line.getQuantity(), line.getQuantity(), true, true), line.getQuantity()))
                .toList();

        releaseAllStrict(oldLines);
        List<PreparedLine> newPrepared;
        List<PreparedLine> newReserved = new ArrayList<>();
        try {
            newPrepared = prepareLines(request.lines());
            reserveAll(newPrepared, newReserved);
            int totalUnits = newPrepared.stream().mapToInt(PreparedLine::quantity).sum();
            ShipmentResponse shipment = shipmentClient.update(order.getOrderNumber(), new UpdateShipmentRequest(
                    request.shippingAddress().trim(), request.shippingRegion().trim(),
                    request.shippingType().trim().toUpperCase(Locale.ROOT), totalUnits));
            if (shipment == null || shipment.price() == null) {
                throw new OrderProcessingException("No fue posible recalcular el envío.");
            }

            order.setShippingAddress(request.shippingAddress().trim());
            order.setShippingRegion(shipment.region());
            order.setShippingType(shipment.routeType());
            order.setShippingCarrier(shipment.carrier());
            order.setShippingRouteName(shipment.routeName());
            order.setShippingRouteCode(shipment.routeCode());
            order.setShippingEstimatedDays(shipment.estimatedDays());
            order.setShippingDistanceKm(shipment.distanceKm());
            order.setShippingEstimatedDeliveryDate(shipment.estimatedDeliveryDate());
            order.setShippingPrice(shipment.price());
            order.setTrackingCode(shipment.trackingCode());
            applyLinesAndTotals(order, newPrepared, order.getDiscountPercentage());
            synchronizeFinancialDocuments(order);
            return toResponse(orderRepository.save(order));
        } catch (RuntimeException ex) {
            releaseQuietly(newReserved);
            reserveQuietly(oldLines);
            throw new OrderProcessingException(cleanRemoteMessage(ex));
        }
    }

    public OrderResponse cancelMyOrder(Long customerUserId, String orderNumber) {
        PurchaseOrder order = loadOwned(customerUserId, orderNumber);
        requirePending(order);
        releaseOrderStock(order);
        shipmentClient.cancel(order.getOrderNumber());
        order.setStatus(OrderStatus.CANCELLED);
        refundPaymentAndVoidReceipt(order);
        return toResponse(orderRepository.save(order));
    }

    public OrderResponse updateStatus(String orderNumber, UpdateOrderStatusRequest request) {
        PurchaseOrder order = load(orderNumber);
        OrderStatus newStatus = request.status();
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new OrderProcessingException("La orden ya está finalizada y no puede cambiar de estado.");
        }

        if (newStatus == OrderStatus.CANCELLED) {
            releaseOrderStock(order);
            shipmentClient.cancel(order.getOrderNumber());
            refundPaymentAndVoidReceipt(order);
        } else if (newStatus == OrderStatus.DELIVERED) {
            dispatchOrderStock(order);
            shipmentClient.updateStatus(order.getOrderNumber(), "DELIVERED");
        } else if (newStatus == OrderStatus.PENDING) {
            shipmentClient.updateStatus(order.getOrderNumber(), "PLANNED");
        } else if (newStatus == OrderStatus.PREPARING) {
            shipmentClient.updateStatus(order.getOrderNumber(), "PREPARING");
        } else if (newStatus == OrderStatus.SHIPPED) {
            shipmentClient.updateStatus(order.getOrderNumber(), "IN_TRANSIT");
        }

        order.setStatus(newStatus);
        if (request.reason() != null && !request.reason().isBlank()) {
            order.setRejectionReason(request.reason().trim());
        }
        return toResponse(orderRepository.save(order));
    }

    public void deleteCancelledOrder(String orderNumber) {
        PurchaseOrder order = load(orderNumber);
        if (order.getStatus() != OrderStatus.CANCELLED) {
            throw new OrderProcessingException("Solo se pueden eliminar órdenes canceladas.");
        }
        orderRepository.delete(order);
    }

    @Transactional(readOnly = true)
    public DashboardReportResponse dashboardReport() {
        List<PurchaseOrder> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        List<PurchaseOrder> sales = orders.stream().filter(this::countsAsSale).toList();
        BigDecimal income = sales.stream().map(PurchaseOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byRegion = new TreeMap<>();
        Map<String, BigDecimal> byDay = new TreeMap<>();
        Map<String, BigDecimal> byMonth = new TreeMap<>();
        Map<String, Long> statuses = orders.stream().collect(Collectors.groupingBy(
                o -> o.getStatus().name(), TreeMap::new, Collectors.counting()));
        Map<String, ProductAccumulator> products = new HashMap<>();

        DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("yyyy-MM");
        for (PurchaseOrder order : sales) {
            byRegion.merge(order.getShippingRegion(), order.getTotalAmount(), BigDecimal::add);
            byDay.merge(order.getCreatedAt().format(dayFormat), order.getTotalAmount(), BigDecimal::add);
            byMonth.merge(order.getCreatedAt().format(monthFormat), order.getTotalAmount(), BigDecimal::add);
            for (OrderLine line : order.getLines()) {
                products.compute(line.getSku(), (sku, current) -> {
                    ProductAccumulator value = current == null
                            ? new ProductAccumulator(line.getSku(), line.getProductName(), 0) : current;
                    return new ProductAccumulator(value.sku(), value.name(), value.units() + line.getQuantity());
                });
            }
        }

        List<ProductSalesMetric> topProducts = products.values().stream()
                .sorted(Comparator.comparingLong(ProductAccumulator::units).reversed())
                .limit(10)
                .map(p -> new ProductSalesMetric(p.sku(), p.name(), p.units()))
                .toList();

        return new DashboardReportResponse(
                orders.size(),
                sales.size(),
                orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.PREPARING).count(),
                orders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count(),
                orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count(),
                income,
                toMetrics(byRegion),
                toMetrics(byDay),
                toMetrics(byMonth),
                topProducts,
                statuses);
    }

    private List<PreparedLine> prepareLines(List<OrderLineRequest> requests) {
        Set<String> seen = new HashSet<>();
        List<PreparedLine> prepared = new ArrayList<>();
        for (OrderLineRequest request : requests) {
            String sku = request.sku().trim().toUpperCase(Locale.ROOT);
            if (!seen.add(sku)) {
                throw new OrderProcessingException("El producto " + sku + " está repetido en la orden.");
            }
            ProductSnapshotResponse snapshot = inventoryClient.snapshot(sku, request.quantity());
            if (!snapshot.active() || !snapshot.available()) {
                throw new OrderProcessingException("Lo sentimos. En estos momentos " + snapshot.productName() + " no tiene stock disponible.");
            }
            prepared.add(new PreparedLine(snapshot, request.quantity()));
        }
        return prepared;
    }

    private void reserveAll(List<PreparedLine> lines, List<PreparedLine> reserved) {
        for (PreparedLine line : lines) {
            inventoryClient.reserve(line.snapshot().sku(), line.quantity());
            reserved.add(line);
        }
    }

    private void applyLinesAndTotals(PurchaseOrder order, List<PreparedLine> prepared, int discountPercentage) {
        order.clearLines();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (PreparedLine preparedLine : prepared) {
            ProductSnapshotResponse snapshot = preparedLine.snapshot();
            OrderLine line = new OrderLine();
            line.setSku(snapshot.sku());
            line.setProductName(snapshot.productName());
            line.setQuantity(preparedLine.quantity());
            line.setUnitPrice(snapshot.price());
            order.addLine(line);
            subtotal = subtotal.add(snapshot.price().multiply(BigDecimal.valueOf(preparedLine.quantity())));
        }
        BigDecimal discount = discountPercentage > 0
                ? subtotal.multiply(BigDecimal.valueOf(discountPercentage))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discount);
        order.setTotalAmount(subtotal.subtract(discount).add(order.getShippingPrice()));
    }

    private UserCouponResponse validateCoupon(Long customerUserId, String coupon) {
        if (coupon == null) return null;
        UserCouponResponse response;
        try {
            response = authClient.couponStatus(customerUserId, coupon);
        } catch (RuntimeException ex) {
            throw new OrderProcessingException(cleanRemoteMessage(ex));
        }
        if (response == null || !response.available()) {
            throw new OrderProcessingException("El cupón " + coupon + " ya fue utilizado o no está disponible.");
        }
        return response;
    }

    private void releaseOrderStock(PurchaseOrder order) {
        if (order.isStockReleased() || order.isStockFinalized()) return;
        for (OrderLine line : order.getLines()) inventoryClient.release(line.getSku(), line.getQuantity());
        order.setStockReleased(true);
    }

    private void dispatchOrderStock(PurchaseOrder order) {
        if (order.isStockFinalized() || order.isStockReleased()) return;
        for (OrderLine line : order.getLines()) inventoryClient.dispatch(line.getSku(), line.getQuantity());
        order.setStockFinalized(true);
    }

    private void releaseAllStrict(List<PreparedLine> lines) {
        for (PreparedLine line : lines) inventoryClient.release(line.snapshot().sku(), line.quantity());
    }

    private void releaseQuietly(List<PreparedLine> lines) {
        for (PreparedLine line : lines) {
            try { inventoryClient.release(line.snapshot().sku(), line.quantity()); } catch (RuntimeException ignored) { }
        }
    }

    private void reserveQuietly(List<PreparedLine> lines) {
        for (PreparedLine line : lines) {
            try { inventoryClient.reserve(line.snapshot().sku(), line.quantity()); } catch (RuntimeException ignored) { }
        }
    }

    private void cancelShipmentQuietly(String orderNumber) {
        try { shipmentClient.cancel(orderNumber); } catch (RuntimeException ignored) { }
    }

    private void rollbackShipmentQuietly(String orderNumber) {
        try { shipmentClient.rollback(orderNumber); } catch (RuntimeException ignored) { }
    }

    private void releaseCouponQuietly(Long customerUserId, String couponCode, String orderNumber) {
        try { authClient.release(customerUserId, couponCode, orderNumber); } catch (RuntimeException ignored) { }
    }

    private PurchaseOrder load(String orderNumber) {
        return orderRepository.findByOrderNumberIgnoreCase(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Orden no encontrada: " + orderNumber));
    }

    private PurchaseOrder loadOwned(Long customerUserId, String orderNumber) {
        PurchaseOrder order = load(orderNumber);
        if (!order.getCustomerUserId().equals(customerUserId)) {
            throw new OrderNotFoundException("Orden no encontrada: " + orderNumber);
        }
        return order;
    }

    private void requirePending(PurchaseOrder order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderProcessingException("Solo puedes editar o cancelar una orden pendiente.");
        }
    }

    private boolean countsAsSale(PurchaseOrder order) {
        return order.getStatus() != OrderStatus.CANCELLED;
    }

    private List<MetricValue> toMetrics(Map<String, BigDecimal> values) {
        return values.entrySet().stream().map(e -> new MetricValue(e.getKey(), e.getValue())).toList();
    }

    private void attachPaymentAndReceipt(PurchaseOrder order, PaymentRequest request) {
        validatePayment(request);
        String digits = request.cardNumber().replaceAll("\\s+", "");

        PaymentTransaction payment = new PaymentTransaction();
        payment.setPaymentReference("PAY-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT));
        payment.setAuthorizationCode(UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT));
        payment.setCardBrand(cardBrand(digits));
        payment.setMaskedCard("**** **** **** " + digits.substring(digits.length() - 4));
        payment.setCardHolderName(request.cardHolderName().trim());
        payment.setInstallments(request.installments());
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.APPROVED);
        payment.setPaidAt(OffsetDateTime.now());
        order.setPayment(payment);

        ElectronicReceipt receipt = new ElectronicReceipt();
        receipt.setReceiptNumber("BOL-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(Locale.ROOT));
        receipt.setIssuedAt(OffsetDateTime.now());
        receipt.setVerificationCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT));
        receipt.setVoided(false);
        order.setReceipt(receipt);
        synchronizeFinancialDocuments(order);
    }

    private void synchronizeFinancialDocuments(PurchaseOrder order) {
        if (order.getPayment() != null) {
            order.getPayment().setAmount(order.getTotalAmount());
        }
        if (order.getReceipt() != null) {
            BigDecimal net = order.getTotalAmount()
                    .divide(BigDecimal.valueOf(1.19), 0, RoundingMode.HALF_UP);
            order.getReceipt().setNetAmount(net);
            order.getReceipt().setTaxAmount(order.getTotalAmount().subtract(net));
            order.getReceipt().setTotalAmount(order.getTotalAmount());
        }
    }

    private void refundPaymentAndVoidReceipt(PurchaseOrder order) {
        if (order.getPayment() != null && order.getPayment().getStatus() == PaymentStatus.APPROVED) {
            order.getPayment().setStatus(PaymentStatus.REFUNDED);
            order.getPayment().setRefundedAt(OffsetDateTime.now());
        }
        if (order.getReceipt() != null) {
            order.getReceipt().setVoided(true);
        }
    }

    private void validatePayment(PaymentRequest request) {
        String digits = request.cardNumber().replaceAll("\\s+", "");
        if (digits.length() < 13 || digits.length() > 19 || !passesLuhn(digits)) {
            throw new OrderProcessingException("La tarjeta ficticia no es válida. Usa un número que pase la validación Luhn.");
        }
        YearMonth expiry;
        try {
            expiry = YearMonth.of(request.expiryYear(), request.expiryMonth());
        } catch (RuntimeException ex) {
            throw new OrderProcessingException("La fecha de vencimiento de la tarjeta es inválida.");
        }
        if (expiry.isBefore(YearMonth.from(LocalDate.now()))) {
            throw new OrderProcessingException("La tarjeta ficticia está vencida.");
        }
        if (request.securityCode() == null || !request.securityCode().matches("\\d{3,4}")) {
            throw new OrderProcessingException("El código de seguridad es inválido.");
        }
    }

    private boolean passesLuhn(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private String cardBrand(String digits) {
        if (digits.startsWith("4")) return "VISA";
        if (digits.startsWith("34") || digits.startsWith("37")) return "AMERICAN EXPRESS";
        if (digits.matches("^(5[1-5]|2[2-7]).*")) return "MASTERCARD";
        return "TARJETA";
    }

    private ReceiptResponse toReceiptResponse(PurchaseOrder order) {
        if (order.getReceipt() == null || order.getPayment() == null) {
            throw new OrderProcessingException("La orden no tiene una boleta electrónica asociada.");
        }
        List<ReceiptLineResponse> lines = order.getLines().stream()
                .map(line -> new ReceiptLineResponse(line.getSku(), line.getProductName(), line.getQuantity(),
                        line.getUnitPrice(), line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()))))
                .toList();
        ElectronicReceipt receipt = order.getReceipt();
        PaymentTransaction payment = order.getPayment();
        return new ReceiptResponse(receipt.getReceiptNumber(), order.getOrderNumber(), receipt.getIssuedAt(),
                order.getCustomerName(), order.getCustomerEmail(), order.getShippingAddress(), order.getShippingRegion(),
                order.getSubtotal(), order.getDiscountAmount(), order.getShippingPrice(), receipt.getNetAmount(),
                receipt.getTaxAmount(), receipt.getTotalAmount(), payment.getPaymentReference(),
                payment.getAuthorizationCode(), payment.getCardBrand(), payment.getMaskedCard(),
                payment.getInstallments(), payment.getStatus(), receipt.getVerificationCode(), receipt.isVoided(), lines);
    }

    private String safeText(String value, String fallback, int maxLength) {
        String clean = value == null || value.isBlank() ? fallback : value.trim();
        if (clean == null || clean.isBlank()) clean = "Sin información";
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength);
    }

    private String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private String normalizeCoupon(String coupon) {
        return coupon == null || coupon.isBlank() ? null : coupon.trim().toUpperCase(Locale.ROOT);
    }

    private String cleanRemoteMessage(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) return "No fue posible procesar la orden.";
        int index = message.indexOf("\"message\":\"");
        if (index >= 0) {
            int start = index + 11;
            int end = message.indexOf('"', start);
            if (end > start) return message.substring(start, end);
        }
        return message;
    }

    private OrderResponse toResponse(PurchaseOrder order) {
        List<OrderLineResponse> lines = order.getLines().stream()
                .map(line -> new OrderLineResponse(line.getSku(), line.getProductName(), line.getQuantity(),
                        line.getUnitPrice(), line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()))))
                .toList();
        PaymentTransaction payment = order.getPayment();
        ElectronicReceipt receipt = order.getReceipt();
        return new OrderResponse(order.getOrderNumber(), order.getCustomerName(), order.getCustomerEmail(),
                order.getShippingAddress(), order.getShippingRegion(), order.getShippingType(),
                order.getShippingCarrier(), order.getShippingRouteName(), order.getShippingRouteCode(),
                order.getShippingEstimatedDays(), order.getShippingDistanceKm(), order.getShippingEstimatedDeliveryDate(),
                order.getShippingPrice(), order.getSubtotal(), order.getDiscountAmount(), order.getTotalAmount(),
                order.isDiscountApplied(), order.getCouponCode(),
                order.getStatus(), order.getTrackingCode(), order.getRejectionReason(), order.getCreatedAt(),
                order.getUpdatedAt(), payment == null ? null : payment.getStatus(),
                payment == null ? null : payment.getPaymentReference(),
                payment == null ? null : payment.getMaskedCard(),
                receipt == null ? null : receipt.getReceiptNumber(), lines);
    }

    private record PreparedLine(ProductSnapshotResponse snapshot, int quantity) {}
    private record ProductAccumulator(String sku, String name, long units) {}
}
