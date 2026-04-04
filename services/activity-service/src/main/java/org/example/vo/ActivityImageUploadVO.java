package org.example.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class ActivityImageUploadVO implements Serializable {

    private String imageKey;

    private String imageUrl;
}
