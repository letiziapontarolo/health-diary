package com.healthdiary.backend.medical.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "exam_results")
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private MedicalEvent exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parameter_id", nullable = false)
    private LabParameter parameter;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private ExamResultValueType valueType;

    @Column(name = "numeric_value", precision = 18, scale = 6)
    private BigDecimal numericValue;

    @Column(name = "text_value", columnDefinition = "TEXT")
    private String textValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private MeasurementUnit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "interval_type", length = 20)
    private ExamResultIntervalType intervalType;

    @Column(name = "lower_bound", precision = 18, scale = 6)
    private BigDecimal lowerBound;

    @Column(name = "upper_bound", precision = 18, scale = 6)
    private BigDecimal upperBound;

    @Column(name = "lower_inclusive")
    private Boolean lowerInclusive;

    @Column(name = "upper_inclusive")
    private Boolean upperInclusive;

    @Column(name = "text_interval", columnDefinition = "TEXT")
    private String textInterval;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;

    protected ExamResult() {
    }

    public ExamResult(
            MedicalEvent exam,
            LabParameter parameter,
            Integer position,
            ExamResultValueType valueType,
            BigDecimal numericValue,
            String textValue,
            MeasurementUnit unit,
            ExamResultIntervalType intervalType,
            BigDecimal lowerBound,
            BigDecimal upperBound,
            Boolean lowerInclusive,
            Boolean upperInclusive,
            String textInterval) {

        this.exam = exam;
        this.parameter = parameter;
        this.position = position;
        this.valueType = valueType;
        this.numericValue = numericValue;
        this.textValue = textValue;
        this.unit = unit;
        this.intervalType = intervalType;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.lowerInclusive = lowerInclusive;
        this.upperInclusive = upperInclusive;
        this.textInterval = textInterval;
    }

    public void update(
            LabParameter parameter,
            Integer position,
            ExamResultValueType valueType,
            BigDecimal numericValue,
            String textValue,
            MeasurementUnit unit,
            ExamResultIntervalType intervalType,
            BigDecimal lowerBound,
            BigDecimal upperBound,
            Boolean lowerInclusive,
            Boolean upperInclusive,
            String textInterval) {

        this.parameter = parameter;
        this.position = position;
        this.valueType = valueType;
        this.numericValue = numericValue;
        this.textValue = textValue;
        this.unit = unit;
        this.intervalType = intervalType;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.lowerInclusive = lowerInclusive;
        this.upperInclusive = upperInclusive;
        this.textInterval = textInterval;
    }

    public Long getId() {
        return id;
    }

    public MedicalEvent getExam() {
        return exam;
    }

    public LabParameter getParameter() {
        return parameter;
    }

    public Integer getPosition() {
        return position;
    }

    public ExamResultValueType getValueType() {
        return valueType;
    }

    public BigDecimal getNumericValue() {
        return numericValue;
    }

    public String getTextValue() {
        return textValue;
    }

    public MeasurementUnit getUnit() {
        return unit;
    }

    public ExamResultIntervalType getIntervalType() {
        return intervalType;
    }

    public BigDecimal getLowerBound() {
        return lowerBound;
    }

    public BigDecimal getUpperBound() {
        return upperBound;
    }

    public Boolean getLowerInclusive() {
        return lowerInclusive;
    }

    public Boolean getUpperInclusive() {
        return upperInclusive;
    }

    public String getTextInterval() {
        return textInterval;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
