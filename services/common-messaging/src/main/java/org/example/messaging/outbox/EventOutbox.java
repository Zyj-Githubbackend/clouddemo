package org.example.messaging.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("event_outbox")
public class EventOutbox {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageId;

    private String eventType;

    private String aggregateType;

    private String aggregateId;

    private String payloadJson;

    private String status;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    private LocalDateTime createdAt;

    private LocalDateTime sentAt;
}
