package org.example.dto;

import lombok.Data;

/**
 * 按活动统计有效报名（REGISTERED）人数，用于与 vol_activity.current_participants 对齐。
 */
@Data
public class ActivityRegisteredCount {
    private Long activityId;
    private Long cnt;
}
