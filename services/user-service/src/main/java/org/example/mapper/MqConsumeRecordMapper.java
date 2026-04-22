package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.messaging.MqConsumeRecord;

@Mapper
public interface MqConsumeRecordMapper extends BaseMapper<MqConsumeRecord> {
}
