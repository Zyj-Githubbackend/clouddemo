package org.example.messaging;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mq_consume_record")
public class MqConsumeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageId;

    private String consumerName;

    private String status;

    private LocalDateTime consumedAt;
}
