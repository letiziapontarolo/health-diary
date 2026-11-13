package com.healthdiary.backend.medical.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record SwabEventRequest(@NotNull Long siteId,@Size(max=200) String otherSiteText) {}
