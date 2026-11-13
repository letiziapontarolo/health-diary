package com.healthdiary.backend.medical.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "measurement_units")
public class MeasurementUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 50)
    private String symbol;

    @Column(name = "name", length = 150)
    private String name;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private Instant createdAt;

    protected MeasurementUnit() {
    }

    public MeasurementUnit(String symbol, String name) {
        this.symbol = symbol;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
