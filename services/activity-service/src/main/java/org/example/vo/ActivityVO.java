package org.example.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityVO implements Serializable {
    
    private Long id;
    
    private String title;
    
    private String description;

    private String imageKey;

    private String imageUrl;

    private List<String> imageKeys;

    private List<String> imageUrls;
    
    private String location;
    
    private Integer maxParticipants;
    
    private Integer currentParticipants;
    
    private BigDecimal volunteerHours;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;

    private LocalDateTime registrationStartTime;
    
    private LocalDateTime registrationDeadline;
    
    private String status;
    
    private String category;
    
    private Boolean isRegistered;
    
    private Integer availableSlots;
}
