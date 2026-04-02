package org.example.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AIGenerateRequest implements Serializable {
    
    private String location;
    
    private String category;
    
    private String keywords;
    
    private BigDecimal volunteerHours;
}
