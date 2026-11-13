package com.healthdiary.backend.medical.entity;
import jakarta.persistence.*;
@Entity @Table(name="swab_sites")
public class SwabSite {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=100) private String name;
 @Column(name="is_active",nullable=false) private boolean active;
 protected SwabSite(){}
 public Long getId(){return id;} public String getName(){return name;} public boolean isActive(){return active;}
}
