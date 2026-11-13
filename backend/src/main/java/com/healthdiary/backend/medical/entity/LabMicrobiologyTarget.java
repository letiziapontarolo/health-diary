package com.healthdiary.backend.medical.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="lab_microbiology_targets")
public class LabMicrobiologyTarget {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="exam_result_id",nullable=false) private ExamResult examResult;
 @Column(name="position") private Integer position;
 @Column(name="target_name", length=250) private String targetName;
 @Column(name="result", length=20) private String result;
 @Column(name="result_text", columnDefinition="TEXT") private String resultText;
 @Column(name="method", length=100) private String method;
 @Column(name="notes", columnDefinition="TEXT") private String notes;
 protected LabMicrobiologyTarget(){}
 public LabMicrobiologyTarget(ExamResult examResult, Integer position, String targetName, String result, String resultText, String method, String notes) {
 this.examResult=examResult; update(position, targetName, result, resultText, method, notes);
 }
 public void update(Integer position, String targetName, String result, String resultText, String method, String notes) {
 this.position=position;
 this.targetName=targetName;
 this.result=result;
 this.resultText=resultText;
 this.method=method;
 this.notes=notes;
 }
 public Long getId(){return id;}
 public ExamResult getExamResult(){return examResult;}
 public Integer getPosition(){return position;}
 public String getTargetName(){return targetName;}
 public String getResult(){return result;}
 public String getResultText(){return resultText;}
 public String getMethod(){return method;}
 public String getNotes(){return notes;}
}
