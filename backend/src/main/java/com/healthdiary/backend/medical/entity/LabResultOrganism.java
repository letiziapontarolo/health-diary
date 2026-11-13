package com.healthdiary.backend.medical.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="lab_result_organisms")
public class LabResultOrganism {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="exam_result_id",nullable=false) private ExamResult examResult;
 @Column(name="organism_type", length=20) private String organismType;
 @Column(name="organism_name", length=250) private String organismName;
 @Column(name="position") private Integer position;
 @Column(name="observed_form", length=150) private String observedForm;
 @Column(name="quantity_numeric") private BigDecimal quantityNumeric;
 @Column(name="quantity_unit", length=50) private String quantityUnit;
 @Column(name="quantity_text", columnDefinition="TEXT") private String quantityText;
 @Column(name="antimicrobial_susceptibility", columnDefinition="TEXT") private String antimicrobialSusceptibility;
 @Column(name="identification_method", length=150) private String identificationMethod;
 @Column(name="notes", columnDefinition="TEXT") private String notes;
 protected LabResultOrganism(){}
 public LabResultOrganism(ExamResult examResult, String organismType, String organismName, Integer position, String observedForm, BigDecimal quantityNumeric, String quantityUnit, String quantityText, String antimicrobialSusceptibility, String identificationMethod, String notes) {
 this.examResult=examResult; update(organismType, organismName, position, observedForm, quantityNumeric, quantityUnit, quantityText, antimicrobialSusceptibility, identificationMethod, notes);
 }
 public void update(String organismType, String organismName, Integer position, String observedForm, BigDecimal quantityNumeric, String quantityUnit, String quantityText, String antimicrobialSusceptibility, String identificationMethod, String notes) {
 this.organismType=organismType;
 this.organismName=organismName;
 this.position=position;
 this.observedForm=observedForm;
 this.quantityNumeric=quantityNumeric;
 this.quantityUnit=quantityUnit;
 this.quantityText=quantityText;
 this.antimicrobialSusceptibility=antimicrobialSusceptibility;
 this.identificationMethod=identificationMethod;
 this.notes=notes;
 }
 public Long getId(){return id;}
 public ExamResult getExamResult(){return examResult;}
 public String getOrganismType(){return organismType;}
 public String getOrganismName(){return organismName;}
 public Integer getPosition(){return position;}
 public String getObservedForm(){return observedForm;}
 public BigDecimal getQuantityNumeric(){return quantityNumeric;}
 public String getQuantityUnit(){return quantityUnit;}
 public String getQuantityText(){return quantityText;}
 public String getAntimicrobialSusceptibility(){return antimicrobialSusceptibility;}
 public String getIdentificationMethod(){return identificationMethod;}
 public String getNotes(){return notes;}
}
