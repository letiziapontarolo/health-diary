package com.healthdiary.backend.medical.service;
import com.healthdiary.backend.medical.dto.*;
import com.healthdiary.backend.medical.entity.*;
import com.healthdiary.backend.medical.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@Service
public class LabMicrobiologyService {
 private final MicrobiologyAccess access; private final LabResultOrganismRepository organisms;
 private final LabMicrobiologyTargetRepository targets;
 public LabMicrobiologyService(MicrobiologyAccess access,LabResultOrganismRepository organisms,LabMicrobiologyTargetRepository targets){this.access=access;this.organisms=organisms;this.targets=targets;}
 private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 private ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found");}
 private void checkOrganism(OrganismRequest r){
  if(r==null)throw bad("Request required");
  if(!Set.of("BACTERIUM","FUNGUS","PARASITE","OTHER").contains(r.organismType()))throw bad("Invalid organism type");
  if(r.organismName()==null||r.organismName().isBlank()||r.organismName().trim().length()>250||r.position()==null||r.position()<1)throw bad("Invalid organism name or position");
  if(r.quantityNumeric()!=null && (r.quantityNumeric().signum()<0 || r.quantityNumeric().precision()>18 || r.quantityNumeric().scale()>6))throw bad("Invalid quantity");
  if(r.quantityUnit()!=null && (r.quantityUnit().isBlank()||r.quantityUnit().length()>50))throw bad("Invalid quantity unit");
 }
 private void checkTarget(MicrobiologyTargetRequest r){
  if(r==null||r.position()==null||r.position()<1||r.targetName()==null||r.targetName().isBlank()||r.targetName().trim().length()>250)throw bad("Invalid target");
  if(!Set.of("POSITIVE","NEGATIVE","INDETERMINATE","OTHER").contains(r.result()))throw bad("Invalid target result");
  if(r.method()!=null && (r.method().isBlank()||r.method().length()>100))throw bad("Invalid method");
 }
 private OrganismResponse map(LabResultOrganism o){return new OrganismResponse(o.getId(),o.getExamResult().getId(),o.getOrganismType(),o.getOrganismName(),o.getPosition(),o.getObservedForm(),o.getQuantityNumeric(),o.getQuantityUnit(),o.getQuantityText(),o.getAntimicrobialSusceptibility(),o.getIdentificationMethod(),o.getNotes());}
 private MicrobiologyTargetResponse map(LabMicrobiologyTarget t){return new MicrobiologyTargetResponse(t.getId(),t.getExamResult().getId(),t.getPosition(),t.getTargetName(),t.getResult(),t.getResultText(),t.getMethod(),t.getNotes());}
 @Transactional(readOnly=true) public List<OrganismResponse> organisms(Long resultId,String email){access.ownedResult(resultId,email);return organisms.findByExamResultIdOrderByPositionAsc(resultId).stream().map(this::map).toList();}
 @Transactional public OrganismResponse addOrganism(Long resultId,String email,OrganismRequest r){var result=access.ownedResult(resultId,email);checkOrganism(r);if(organisms.existsByExamResultIdAndPosition(resultId,r.position()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Position already used");return map(organisms.save(new LabResultOrganism(result,r.organismType(),r.organismName().trim(),r.position(),r.observedForm(),r.quantityNumeric(),r.quantityUnit(),r.quantityText(),r.antimicrobialSusceptibility(),r.identificationMethod(),r.notes())));}
 @Transactional public OrganismResponse editOrganism(Long resultId,Long id,String email,OrganismRequest r){access.ownedResult(resultId,email);checkOrganism(r);var o=organisms.findByIdAndExamResultId(id,resultId).orElseThrow(this::missing);if(organisms.existsByExamResultIdAndPositionAndIdNot(resultId,r.position(),id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Position already used");o.update(r.organismType(),r.organismName().trim(),r.position(),r.observedForm(),r.quantityNumeric(),r.quantityUnit(),r.quantityText(),r.antimicrobialSusceptibility(),r.identificationMethod(),r.notes());return map(o);}
 @Transactional public void deleteOrganism(Long resultId,Long id,String email){access.ownedResult(resultId,email);organisms.delete(organisms.findByIdAndExamResultId(id,resultId).orElseThrow(this::missing));}
 @Transactional(readOnly=true) public List<MicrobiologyTargetResponse> targets(Long resultId,String email){access.ownedResult(resultId,email);return targets.findByExamResultIdOrderByPositionAsc(resultId).stream().map(this::map).toList();}
 @Transactional public MicrobiologyTargetResponse addTarget(Long resultId,String email,MicrobiologyTargetRequest r){var result=access.ownedResult(resultId,email);checkTarget(r);if(targets.existsByExamResultIdAndPosition(resultId,r.position()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Position already used");return map(targets.save(new LabMicrobiologyTarget(result,r.position(),r.targetName().trim(),r.result(),r.resultText(),r.method(),r.notes())));}
 @Transactional public MicrobiologyTargetResponse editTarget(Long resultId,Long id,String email,MicrobiologyTargetRequest r){access.ownedResult(resultId,email);checkTarget(r);var t=targets.findByIdAndExamResultId(id,resultId).orElseThrow(this::missing);if(targets.existsByExamResultIdAndPositionAndIdNot(resultId,r.position(),id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Position already used");t.update(r.position(),r.targetName().trim(),r.result(),r.resultText(),r.method(),r.notes());return map(t);}
 @Transactional public void deleteTarget(Long resultId,Long id,String email){access.ownedResult(resultId,email);targets.delete(targets.findByIdAndExamResultId(id,resultId).orElseThrow(this::missing));}
}
