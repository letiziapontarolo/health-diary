package com.healthdiary.backend.medical.service;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.entity.*;
import com.healthdiary.backend.medical.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
@Component
public class MicrobiologyAccess {
 private final UserRepository users; private final ExamResultRepository results;
 public MicrobiologyAccess(UserRepository users,ExamResultRepository results){this.users=users;this.results=results;}
 public ExamResult ownedResult(Long resultId,String email){
  var user=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
  var result=results.findById(resultId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Result not found"));
  if(!result.getExam().getUser().getId().equals(user.getId()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Result not found");
  if(result.getExam().getService().getCategory()!=MedicalServiceCategory.LABORATORY)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Not a laboratory result");
  return result;
 }
}
