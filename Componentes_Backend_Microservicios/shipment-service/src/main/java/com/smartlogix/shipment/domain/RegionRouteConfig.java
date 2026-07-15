package com.smartlogix.shipment.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "region_route_config")
public class RegionRouteConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String regionName;

    @Column(nullable = false)
    private int distanceKm;

    @Column(nullable = false)
    private double remoteFactor;

    @Column(nullable = false, length = 60)
    private String economyCarrier;

    @Column(nullable = false, length = 60)
    private String balancedCarrier;

    @Column(nullable = false, length = 60)
    private String expressCarrier;

    public Long getId() { return id; }
    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }
    public int getDistanceKm() { return distanceKm; }
    public void setDistanceKm(int distanceKm) { this.distanceKm = distanceKm; }
    public double getRemoteFactor() { return remoteFactor; }
    public void setRemoteFactor(double remoteFactor) { this.remoteFactor = remoteFactor; }
    public String getEconomyCarrier() { return economyCarrier; }
    public void setEconomyCarrier(String economyCarrier) { this.economyCarrier = economyCarrier; }
    public String getBalancedCarrier() { return balancedCarrier; }
    public void setBalancedCarrier(String balancedCarrier) { this.balancedCarrier = balancedCarrier; }
    public String getExpressCarrier() { return expressCarrier; }
    public void setExpressCarrier(String expressCarrier) { this.expressCarrier = expressCarrier; }
}
