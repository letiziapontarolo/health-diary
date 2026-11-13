package com.healthdiary.backend.medical.service;
import com.healthdiary.backend.medical.dto.*;
import com.healthdiary.backend.medical.entity.*;
import com.healthdiary.backend.medical.repository.*;
import com.healthdiary.backend.auth.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@Service
public class SwabEventService {
 private final SwabSiteRepository sites; private final MedicalEventSwabRepository swabs;
 private final MedicalEventRepository events; private final UserRepository users;
 public SwabEventService(SwabSiteRepository sites,MedicalEventSwabRepository swabs,MedicalEventRepository events,UserRepository users){this.sites=sites;this.swabs=swabs;this.events=events;this.users=users;}
 @Transactional(readOnly=true) public List<SwabSiteResponse> sites(){return sites.findByActiveTrueOrderByNameAsc().stream().map(s->new SwabSiteResponse(s.getId(),s.getName())).toList();}
 private MedicalEvent ownedSwab(Long id,String email){
  var user=users.findByEmailIgnoreCase(email).orElseThrow(()->notFound("User not found"));
  var event=events.findByIdAndUserId(id,user.getId()).orElseThrow(()->notFound("Event not found"));
  if(event.getService().getCategory()!=MedicalServiceCategory.LABORATORY || !event.getService().getName().equalsIgnoreCase("Analisi su tampone"))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Event is not a swab examination");
  return event;
 }
 @Transactional(readOnly=true) public SwabEventResponse get(Long eventId,String email){ownedSwab(eventId,email);return response(swabs.findById(eventId).orElseThrow(()->notFound("Swab site not set")));}
 @Transactional public SwabEventResponse save(Long eventId,String email,SwabEventRequest r){
  var event=ownedSwab(eventId,email);
  if(r==null || r.siteId()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Site required");
  var site=sites.findById(r.siteId()).filter(SwabSite::isActive).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Site unavailable"));
  String other=r.otherSiteText()==null?null:r.otherSiteText().trim();
  if("Altro".equalsIgnoreCase(site.getName())){if(other==null||other.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Specify the other site");}
  else if(other!=null&&!other.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Other site text is allowed only for Altro");
  else other=null;
  var record=swabs.findById(eventId).orElse(null);
  if(record==null)record=new MedicalEventSwab(event,site,other);else record.update(site,other);
  return response(swabs.save(record));
 }
 @Transactional public void delete(Long eventId,String email){ownedSwab(eventId,email);swabs.deleteById(eventId);}
 private SwabEventResponse response(MedicalEventSwab s){return new SwabEventResponse(s.getEventId(),s.getSite().getId(),s.getSite().getName(),s.getOtherSiteText());}
 private ResponseStatusException notFound(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);}
}
