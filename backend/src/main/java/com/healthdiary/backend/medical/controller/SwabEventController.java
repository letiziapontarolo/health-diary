package com.healthdiary.backend.medical.controller;
import com.healthdiary.backend.medical.dto.*;
import com.healthdiary.backend.medical.service.SwabEventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api")
public class SwabEventController {
 private final SwabEventService service;
 public SwabEventController(SwabEventService service){this.service=service;}
 @GetMapping("/swab-sites") public List<SwabSiteResponse> sites(){return service.sites();}
 @GetMapping("/medical-events/{eventId}/swab-site") public SwabEventResponse get(@PathVariable Long eventId,Authentication a){return service.get(eventId,a.getName());}
 @PutMapping("/medical-events/{eventId}/swab-site") public SwabEventResponse save(@PathVariable Long eventId,@Valid @RequestBody SwabEventRequest r,Authentication a){return service.save(eventId,a.getName(),r);}
 @DeleteMapping("/medical-events/{eventId}/swab-site") public ResponseEntity<Void> delete(@PathVariable Long eventId,Authentication a){service.delete(eventId,a.getName());return ResponseEntity.noContent().build();}
}
