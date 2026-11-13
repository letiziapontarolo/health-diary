package com.healthdiary.backend.medical.entity;
import jakarta.persistence.*;
@Entity @Table(name="medical_event_swabs")
public class MedicalEventSwab {
 @Id @Column(name="event_id") private Long eventId;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @MapsId @JoinColumn(name="event_id") private MedicalEvent event;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="swab_site_id",nullable=false) private SwabSite site;
 @Column(name="other_site_text",length=200) private String otherSiteText;
 protected MedicalEventSwab(){}
 public MedicalEventSwab(MedicalEvent event,SwabSite site,String otherSiteText){this.event=event;this.site=site;this.otherSiteText=otherSiteText;}
 public void update(SwabSite site,String otherSiteText){this.site=site;this.otherSiteText=otherSiteText;}
 public Long getEventId(){return eventId;} public SwabSite getSite(){return site;} public String getOtherSiteText(){return otherSiteText;}
}
