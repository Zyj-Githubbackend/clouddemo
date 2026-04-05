package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.dto.ActivityRegisteredCount;
import org.example.entity.Registration;
import org.example.vo.RegistrationVO;

import java.util.List;

@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {
    
    @Select("SELECT r.id, r.user_id AS userId, r.activity_id AS activityId, a.title AS activityTitle, a.location, " +
            "a.volunteer_hours AS volunteerHours, a.start_time AS startTime, r.registration_time AS registrationTime, " +
            "r.check_in_status AS checkInStatus, r.check_in_time AS checkInTime, " +
            "r.hours_confirmed AS hoursConfirmed, r.confirm_time AS confirmTime, r.status " +
            "FROM vol_registration r " +
            "LEFT JOIN vol_activity a ON r.activity_id = a.id " +
            "WHERE r.user_id = #{userId} " +
            "ORDER BY r.create_time DESC")
    List<RegistrationVO> selectUserRegistrations(Long userId);

    @Select("SELECT r.id, r.user_id AS userId, r.activity_id AS activityId, a.title AS activityTitle, a.location, " +
            "a.volunteer_hours AS volunteerHours, a.start_time AS startTime, r.registration_time AS registrationTime, " +
            "r.check_in_status AS checkInStatus, r.check_in_time AS checkInTime, " +
            "r.hours_confirmed AS hoursConfirmed, r.confirm_time AS confirmTime, r.status " +
            "FROM vol_registration r " +
            "INNER JOIN vol_activity a ON r.activity_id = a.id " +
            "WHERE r.user_id = #{userId} AND r.status = 'REGISTERED' AND r.hours_confirmed = 1 " +
            "ORDER BY r.confirm_time DESC, r.create_time DESC")
    List<RegistrationVO> selectConfirmedRegistrationsByUserId(@Param("userId") Long userId);

    @Select("SELECT r.id, r.user_id AS userId, r.activity_id AS activityId, a.title AS activityTitle, a.location, " +
            "a.volunteer_hours AS volunteerHours, a.start_time AS startTime, r.registration_time AS registrationTime, " +
            "r.check_in_status AS checkInStatus, r.check_in_time AS checkInTime, " +
            "r.hours_confirmed AS hoursConfirmed, r.confirm_time AS confirmTime, r.status, " +
            "u.username, u.real_name AS realName, u.student_no AS studentNo, u.phone " +
            "FROM vol_registration r " +
            "INNER JOIN vol_activity a ON r.activity_id = a.id " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.status = 'REGISTERED' " +
            "ORDER BY r.registration_time DESC")
    List<RegistrationVO> selectAllRegistrationsForAdmin();

    @Select("SELECT r.id, r.user_id AS userId, r.activity_id AS activityId, a.title AS activityTitle, a.location, " +
            "a.volunteer_hours AS volunteerHours, a.start_time AS startTime, r.registration_time AS registrationTime, " +
            "r.check_in_status AS checkInStatus, r.check_in_time AS checkInTime, " +
            "r.hours_confirmed AS hoursConfirmed, r.confirm_time AS confirmTime, r.status, " +
            "u.username, u.real_name AS realName, u.student_no AS studentNo, u.phone " +
            "FROM vol_registration r " +
            "INNER JOIN vol_activity a ON r.activity_id = a.id " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.status = 'REGISTERED' AND r.activity_id = #{activityId} " +
            "ORDER BY r.registration_time DESC")
    List<RegistrationVO> selectRegistrationsForAdminByActivityId(@Param("activityId") Long activityId);

    @Select("<script>SELECT activity_id AS activityId, COUNT(*) AS cnt FROM vol_registration WHERE status = 'REGISTERED' "
            + "AND activity_id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "GROUP BY activity_id</script>")
    List<ActivityRegisteredCount> countRegisteredGroupByActivityId(@Param("ids") List<Long> ids);
}
