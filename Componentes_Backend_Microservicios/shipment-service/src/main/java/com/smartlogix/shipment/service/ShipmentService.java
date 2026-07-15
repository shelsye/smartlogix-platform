package com.smartlogix.shipment.service;

import com.smartlogix.shipment.domain.AcceptedRouteSelection;
import com.smartlogix.shipment.domain.RegionRouteConfig;
import com.smartlogix.shipment.domain.Shipment;
import com.smartlogix.shipment.domain.ShipmentStatus;
import com.smartlogix.shipment.dto.*;
import com.smartlogix.shipment.exception.ShipmentNotFoundException;
import com.smartlogix.shipment.repository.AcceptedRouteSelectionRepository;
import com.smartlogix.shipment.repository.RegionRouteConfigRepository;
import com.smartlogix.shipment.repository.ShipmentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShipmentService {
    private final ShipmentRepository shipmentRepository;
    private final RegionRouteConfigRepository regionRepository;
    private final AcceptedRouteSelectionRepository routeSelectionRepository;

    public ShipmentService(ShipmentRepository shipmentRepository,
                           RegionRouteConfigRepository regionRepository,
                           AcceptedRouteSelectionRepository routeSelectionRepository) {
        this.shipmentRepository = shipmentRepository;
        this.regionRepository = regionRepository;
        this.routeSelectionRepository = routeSelectionRepository;
    }

    @Transactional(readOnly = true)
    public List<String> regions() {
        return regionRepository.findAllByOrderByRegionNameAsc().stream()
                .map(RegionRouteConfig::getRegionName).toList();
    }

    @Transactional(readOnly = true)
    public List<RouteOptionResponse> recommendRoutes(RouteRecommendationRequest request) {
        RegionRouteConfig config = loadRegion(request.region());
        int units = Math.max(1, request.units());
        int distance = config.getDistanceKm();
        double factor = config.getRemoteFactor();
        LocalDate today = LocalDate.now();

        int economyDays = Math.max(2, (int) Math.ceil(distance / 420.0) + 1);
        int balancedDays = Math.max(1, (int) Math.ceil(distance / 620.0) + 1);
        int expressDays = Math.max(1, (int) Math.ceil(distance / 900.0));

        BigDecimal economyPrice = money((2800 + distance * 3.4 + units * 300) * factor);
        BigDecimal balancedPrice = money((4100 + distance * 5.2 + units * 420) * factor);
        BigDecimal expressPrice = money((6900 + distance * 7.8 + units * 560) * factor);

        double balanceScore = score(balancedDays, balancedPrice, distance, economyPrice, expressDays);

        return List.of(
                new RouteOptionResponse("ECONOMICO", "Ruta Consolidada " + config.getRegionName(),
                        config.getEconomyCarrier(), economyPrice, economyDays, distance,
                        today.plusDays(economyDays), 0.78),
                new RouteOptionResponse("MEJOR_RUTA", "Ruta Equilibrada " + config.getRegionName(),
                        config.getBalancedCarrier(), balancedPrice, balancedDays, distance,
                        today.plusDays(balancedDays), balanceScore),
                new RouteOptionResponse("EXPRESS", "Entrega Express " + config.getRegionName(),
                        config.getExpressCarrier(), expressPrice, expressDays, distance,
                        today.plusDays(expressDays), 1.0)
        );
    }

    public AcceptedRouteResponse acceptRoute(Long userId, AcceptRouteRequest request) {
        String normalizedType = request.routeType().trim().toUpperCase(Locale.ROOT);
        RouteOptionResponse selected = recommendRoutes(
                new RouteRecommendationRequest(request.region(), request.totalUnits())).stream()
                .filter(option -> option.type().equals(normalizedType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de envío inválido. Use ECONOMICO, MEJOR_RUTA o EXPRESS."));

        AcceptedRouteSelection selection = routeSelectionRepository
                .findFirstByUserIdAndOrderNumberIsNullOrderByUpdatedAtDesc(userId)
                .orElseGet(AcceptedRouteSelection::new);

        if (selection.getSelectionId() == null) {
            selection.setSelectionId("SEL-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT));
            selection.setUserId(userId);
        }

        String canonicalRegion = loadRegion(request.region()).getRegionName();
        selection.setRegion(canonicalRegion);
        selection.setTotalUnits(request.totalUnits());
        selection.setRouteType(selected.type());
        selection.setRouteName(selected.routeName());
        selection.setCarrier(selected.carrier());
        selection.setRouteCode(buildRouteCode(selected.type(), canonicalRegion, selection.getSelectionId()));
        selection.setPrice(selected.price());
        selection.setEstimatedDays(selected.estimatedDays());
        selection.setDistanceKm(selected.distanceKm());
        selection.setEstimatedDeliveryDate(selected.estimatedDate());
        selection.setOrderNumber(null);

        return toAcceptedRouteResponse(routeSelectionRepository.saveAndFlush(selection));
    }

    @Transactional(readOnly = true)
    public Optional<AcceptedRouteResponse> currentSelection(Long userId) {
        return routeSelectionRepository.findFirstByUserIdAndOrderNumberIsNullOrderByUpdatedAtDesc(userId)
                .map(this::toAcceptedRouteResponse);
    }

    public Optional<AcceptedRouteResponse> clearCurrentSelection(Long userId) {
        Optional<AcceptedRouteSelection> current = routeSelectionRepository
                .findFirstByUserIdAndOrderNumberIsNullOrderByUpdatedAtDesc(userId);
        current.ifPresent(routeSelectionRepository::delete);
        return current.map(this::toAcceptedRouteResponse);
    }

    public ShipmentResponse createShipmentFromSelection(CreateShipmentFromSelectionRequest request) {
        Optional<Shipment> existing = shipmentRepository.findByOrderNumberIgnoreCase(request.orderNumber());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        AcceptedRouteSelection selection = routeSelectionRepository
                .findBySelectionIdAndUserId(request.selectionId().trim(), request.userId())
                .orElseThrow(() -> new IllegalArgumentException("La ruta aceptada no existe o no pertenece al usuario."));

        if (selection.getOrderNumber() != null && !selection.getOrderNumber().equalsIgnoreCase(request.orderNumber())) {
            throw new IllegalArgumentException("La ruta aceptada ya fue utilizada por otra orden.");
        }
        if (selection.getTotalUnits() != request.totalUnits()) {
            throw new IllegalArgumentException("La cantidad del carrito cambió. Calcula y acepta nuevamente la ruta.");
        }

        Shipment shipment = new Shipment();
        shipment.setTrackingCode("SLX-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(Locale.ROOT));
        shipment.setOrderNumber(request.orderNumber().trim().toUpperCase(Locale.ROOT));
        shipment.setDestinationAddress(request.destinationAddress().trim());
        shipment.setRegion(selection.getRegion());
        shipment.setTotalUnits(selection.getTotalUnits());
        shipment.setRouteType(selection.getRouteType());
        shipment.setRouteName(selection.getRouteName());
        shipment.setCarrier(selection.getCarrier());
        shipment.setRouteCode(selection.getRouteCode());
        shipment.setPrice(selection.getPrice());
        shipment.setEstimatedDays(selection.getEstimatedDays());
        shipment.setDistanceKm(selection.getDistanceKm());
        shipment.setEstimatedDeliveryDate(selection.getEstimatedDeliveryDate());
        shipment.setStatus(ShipmentStatus.PLANNED);

        Shipment saved = shipmentRepository.saveAndFlush(shipment);
        selection.setOrderNumber(saved.getOrderNumber());
        routeSelectionRepository.save(selection);
        return toResponse(saved);
    }

    public ShipmentResponse createShipment(CreateShipmentRequest request) {
        if (shipmentRepository.findByOrderNumberIgnoreCase(request.orderNumber()).isPresent()) {
            Shipment existing = shipmentRepository.findByOrderNumberIgnoreCase(request.orderNumber()).orElseThrow();
            return updatePlan(existing, new UpdateShipmentPlanRequest(request.destinationAddress(), request.region(),
                    request.routeType(), request.totalUnits()));
        }
        Shipment shipment = new Shipment();
        shipment.setTrackingCode("SLX-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(Locale.ROOT));
        shipment.setOrderNumber(request.orderNumber().trim().toUpperCase(Locale.ROOT));
        shipment.setStatus(ShipmentStatus.PLANNED);
        applyPlan(shipment, request.destinationAddress(), request.region(), request.routeType(), request.totalUnits());
        return toResponse(shipmentRepository.save(shipment));
    }

    public ShipmentResponse updateByOrderNumber(String orderNumber, UpdateShipmentPlanRequest request) {
        Shipment shipment = shipmentRepository.findByOrderNumberIgnoreCase(orderNumber)
                .orElseThrow(() -> new ShipmentNotFoundException("No existe envío para la orden " + orderNumber));
        return updatePlan(shipment, request);
    }

    public ShipmentResponse adaptCarrier(String trackingCode, UpdateShipmentPlanRequest request) {
        Shipment shipment = loadTracking(trackingCode);
        return updatePlan(shipment, request);
    }

    private ShipmentResponse updatePlan(Shipment shipment, UpdateShipmentPlanRequest request) {
        if (shipment.getStatus() != ShipmentStatus.PLANNED) {
            throw new IllegalArgumentException("Solo se puede modificar un envío planificado.");
        }
        applyPlan(shipment, request.destinationAddress(), request.region(), request.routeType(), request.totalUnits());
        Shipment saved = shipmentRepository.saveAndFlush(shipment);
        synchronizeLinkedSelection(saved);
        return toResponse(saved);
    }

    private void applyPlan(Shipment shipment, String address, String region, String routeType, int units) {
        String normalizedType = routeType.trim().toUpperCase(Locale.ROOT);
        RouteOptionResponse selected = recommendRoutes(new RouteRecommendationRequest(region, units)).stream()
                .filter(option -> option.type().equals(normalizedType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de envío inválido. Use ECONOMICO, MEJOR_RUTA o EXPRESS."));
        shipment.setDestinationAddress(address.trim());
        shipment.setRegion(loadRegion(region).getRegionName());
        shipment.setTotalUnits(units);
        shipment.setRouteType(selected.type());
        shipment.setRouteName(selected.routeName());
        shipment.setCarrier(selected.carrier());
        shipment.setRouteCode(buildRouteCode(selected.type(), shipment.getRegion(), shipment.getOrderNumber()));
        shipment.setPrice(selected.price());
        shipment.setEstimatedDays(selected.estimatedDays());
        shipment.setDistanceKm(selected.distanceKm());
        shipment.setEstimatedDeliveryDate(selected.estimatedDate());
    }

    private void synchronizeLinkedSelection(Shipment shipment) {
        routeSelectionRepository.findByOrderNumberIgnoreCase(shipment.getOrderNumber()).ifPresent(selection -> {
            selection.setRegion(shipment.getRegion());
            selection.setTotalUnits(shipment.getTotalUnits());
            selection.setRouteType(shipment.getRouteType());
            selection.setRouteName(shipment.getRouteName());
            selection.setCarrier(shipment.getCarrier());
            selection.setRouteCode(shipment.getRouteCode());
            selection.setPrice(shipment.getPrice());
            selection.setEstimatedDays(shipment.getEstimatedDays());
            selection.setDistanceKm(shipment.getDistanceKm());
            selection.setEstimatedDeliveryDate(shipment.getEstimatedDeliveryDate());
            routeSelectionRepository.save(selection);
        });
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getShipments() {
        return shipmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getByTrackingCode(String trackingCode) {
        return toResponse(loadTracking(trackingCode));
    }

    public ShipmentResponse updateStatus(String trackingCode, ShipmentStatus status) {
        Shipment shipment = loadTracking(trackingCode);
        shipment.setStatus(status);
        return toResponse(shipmentRepository.save(shipment));
    }

    public ShipmentResponse updateStatusByOrderNumber(String orderNumber, ShipmentStatus status) {
        Shipment shipment = shipmentRepository.findByOrderNumberIgnoreCase(orderNumber)
                .orElseThrow(() -> new ShipmentNotFoundException("No existe envío para la orden " + orderNumber));
        shipment.setStatus(status);
        return toResponse(shipmentRepository.save(shipment));
    }

    public void cancelByOrderNumber(String orderNumber) {
        shipmentRepository.findByOrderNumberIgnoreCase(orderNumber).ifPresent(shipment -> {
            shipment.setStatus(ShipmentStatus.CANCELLED);
            shipmentRepository.save(shipment);
        });
    }

    public void rollbackByOrderNumber(String orderNumber) {
        shipmentRepository.findByOrderNumberIgnoreCase(orderNumber).ifPresent(shipment -> {
            if (shipment.getStatus() == ShipmentStatus.PLANNED) {
                shipmentRepository.delete(shipment);
            }
        });
        routeSelectionRepository.findByOrderNumberIgnoreCase(orderNumber).ifPresent(selection -> {
            selection.setOrderNumber(null);
            routeSelectionRepository.save(selection);
        });
    }

    public void deleteShipment(String trackingCode) {
        Shipment shipment = loadTracking(trackingCode);
        if (shipment.getStatus() != ShipmentStatus.CANCELLED) {
            throw new IllegalArgumentException("Solo se pueden eliminar envíos cancelados.");
        }
        shipmentRepository.delete(shipment);
    }

    private RegionRouteConfig loadRegion(String region) {
        return regionRepository.findByRegionNameIgnoreCase(region.trim())
                .orElseThrow(() -> new IllegalArgumentException("Región inválida. Seleccione una de las 16 regiones de Chile."));
    }

    private Shipment loadTracking(String trackingCode) {
        return shipmentRepository.findByTrackingCodeIgnoreCase(trackingCode.trim())
                .orElseThrow(() -> new ShipmentNotFoundException("No existe el envío " + trackingCode));
    }

    private BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP);
    }

    private double score(int days, BigDecimal price, int distance, BigDecimal minPrice, int minDays) {
        double timeScore = Math.max(0, 1.0 - ((days - minDays) / 10.0));
        double costScore = minPrice.doubleValue() / Math.max(1.0, price.doubleValue());
        double distanceScore = 1.0 / (1.0 + distance / 3500.0);
        return Math.round((timeScore * 0.45 + costScore * 0.35 + distanceScore * 0.20) * 100.0) / 100.0;
    }

    private String buildRouteCode(String type, String region, String reference) {
        String regionCode = region.replaceAll("[^A-Za-zÁÉÍÓÚÑáéíóúñ]", "")
                .toUpperCase(Locale.ROOT);
        regionCode = regionCode.substring(0, Math.min(4, regionCode.length()));
        String referenceCode = reference.replaceAll("[^A-Za-z0-9]", "");
        referenceCode = referenceCode.substring(Math.max(0, referenceCode.length() - Math.min(4, referenceCode.length())));
        return "RTE-" + type.substring(0, Math.min(4, type.length())) + "-" + regionCode + "-" + referenceCode;
    }

    private AcceptedRouteResponse toAcceptedRouteResponse(AcceptedRouteSelection selection) {
        return new AcceptedRouteResponse(selection.getSelectionId(), selection.getRegion(), selection.getTotalUnits(),
                selection.getRouteType(), selection.getRouteName(), selection.getCarrier(), selection.getRouteCode(),
                selection.getPrice(), selection.getEstimatedDays(), selection.getDistanceKm(),
                selection.getEstimatedDeliveryDate(), selection.getOrderNumber(), selection.getUpdatedAt());
    }

    private ShipmentResponse toResponse(Shipment s) {
        return new ShipmentResponse(s.getTrackingCode(), s.getOrderNumber(), s.getDestinationAddress(), s.getRegion(),
                s.getRouteType(), s.getRouteName(), s.getCarrier(), s.getRouteCode(), s.getPrice(),
                s.getEstimatedDays(), s.getDistanceKm(), s.getEstimatedDeliveryDate(), s.getStatus(), s.getCreatedAt());
    }
}
