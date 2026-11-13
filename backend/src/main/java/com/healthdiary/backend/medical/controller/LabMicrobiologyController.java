package com.healthdiary.backend.medical.controller;
import com.healthdiary.backend.medical.dto.*;
import com.healthdiary.backend.medical.service.LabMicrobiologyService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/exam-results/{resultId}/microbiology")
public class LabMicrobiologyController {
 private final LabMicrobiologyService service;
 public LabMicrobiologyController(LabMicrobiologyService service){this.service=service;}
 @GetMapping("/organisms") public List<OrganismResponse> organisms(@PathVariable Long resultId,Authentication a){return service.organisms(resultId,a.getName());}
 @PostMapping("/organisms") public ResponseEntity<OrganismResponse> addOrganism(@PathVariable Long resultId,@Valid @RequestBody OrganismRequest r,Authentication a){return ResponseEntity.status(HttpStatus.CREATED).body(service.addOrganism(resultId,a.getName(),r));}
 @PutMapping("/organisms/{id}") public OrganismResponse editOrganism(@PathVariable Long resultId,@PathVariable Long id,@Valid @RequestBody OrganismRequest r,Authentication a){return service.editOrganism(resultId,id,a.getName(),r);}
 @DeleteMapping("/organisms/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteOrganism(@PathVariable Long resultId,@PathVariable Long id,Authentication a){service.deleteOrganism(resultId,id,a.getName());}
 @GetMapping("/targets") public List<MicrobiologyTargetResponse> targets(@PathVariable Long resultId,Authentication a){return service.targets(resultId,a.getName());}
 @PostMapping("/targets") public ResponseEntity<MicrobiologyTargetResponse> addTarget(@PathVariable Long resultId,@Valid @RequestBody MicrobiologyTargetRequest r,Authentication a){return ResponseEntity.status(HttpStatus.CREATED).body(service.addTarget(resultId,a.getName(),r));}
 @PutMapping("/targets/{id}") public MicrobiologyTargetResponse editTarget(@PathVariable Long resultId,@PathVariable Long id,@Valid @RequestBody MicrobiologyTargetRequest r,Authentication a){return service.editTarget(resultId,id,a.getName(),r);}
 @DeleteMapping("/targets/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteTarget(@PathVariable Long resultId,@PathVariable Long id,Authentication a){service.deleteTarget(resultId,id,a.getName());}
}
