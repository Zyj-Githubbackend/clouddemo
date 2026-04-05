package org.example.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityCreateRequest implements Serializable {
    
    private String title;
    
    private String description;

    private String imageKey;

    private List<String> imageKeys;
    
    private String location;
    
    private Integer maxParticipants;
    
    private BigDecimal volunteerHours;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;

    private LocalDateTime registrationStartTime;
    
    private LocalDateTime registrationDeadline;
    
    private String category;
}
