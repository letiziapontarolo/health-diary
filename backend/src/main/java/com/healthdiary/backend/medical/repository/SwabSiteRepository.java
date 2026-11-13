package com.healthdiary.backend.medical.repository;
import com.healthdiary.backend.medical.entity.SwabSite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SwabSiteRepository extends JpaRepository<SwabSite,Long> {List<SwabSite> findByActiveTrueOrderByNameAsc();}
