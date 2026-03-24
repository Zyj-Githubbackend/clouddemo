package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.constant.RedisKeyConstant;
import org.example.common.exception.BusinessException;
import org.example.dto.ActivityCreateRequest;
import org.example.entity.Activity;
import org.example.entity.Registration;
import org.example.feign.UserServiceClient;
import org.example.mapper.ActivityMapper;
import org.example.mapper.RegistrationMapper;
import org.example.vo.ActivityVO;
import org.example.vo.RegistrationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ActivityService {
    
    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final StringRedisTemplate redisTemplate;
    private final UserServiceClient userServiceClient;
    
    public ActivityService(ActivityMapper activityMapper, 
                          RegistrationMapper registrationMapper,
                          StringRedisTemplate redisTemplate,
                          UserServiceClient userServiceClient) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.redisTemplate = redisTemplate;
        this.userServiceClient = userServiceClient;
    }
    
    public IPage<ActivityVO> listActivities(Integer page, Integer size, String status, String category, Long userId) {
        Page<Activity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null) {
            wrapper.eq(Activity::getStatus, status);
        }
        if (category != null) {
            wrapper.eq(Activity::getCategory, category);
        }
        
        wrapper.orderByDesc(Activity::getCreateTime);
        IPage<Activity> activityPage = activityMapper.selectPage(pageParam, wrapper);
        
        return activityPage.convert(activity -> {
            ActivityVO vo = new ActivityVO();
            BeanUtils.copyProperties(activity, vo);
            vo.setAvailableSlots(activity.getMaxParticipants() - activity.getCurrentParticipants());
            
            if (userId != null) {
                LambdaQueryWrapper<Registration> regWrapper = new LambdaQueryWrapper<>();
                regWrapper.eq(Registration::getUserId, userId)
                         .eq(Registration::getActivityId, activity.getId())
                         .eq(Registration::getStatus, "REGISTERED");
                vo.setIsRegistered(registrationMapper.selectCount(regWrapper) > 0);
            }
            
            return vo;
        });
    }
    
    public ActivityVO getActivityDetail(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        
        ActivityVO vo = new ActivityVO();
        BeanUtils.copyProperties(activity, vo);
        vo.setAvailableSlots(activity.getMaxParticipants() - activity.getCurrentParticipants());
        
        if (userId != null) {
            LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Registration::getUserId, userId)
                   .eq(Registration::getActivityId, activityId)
                   .eq(Registration::getStatus, "REGISTERED");
            vo.setIsRegistered(registrationMapper.selectCount(wrapper) > 0);
        }
        
        return vo;
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void createActivity(ActivityCreateRequest request, Long creatorId) {
        Activity activity = new Activity();
        BeanUtils.copyProperties(request, activity);
        activity.setCreatorId(creatorId);
        activity.setCurrentParticipants(0);
        activity.setStatus("RECRUITING");
        
        activityMapper.insert(activity);
        
        String stockKey = RedisKeyConstant.getActivityStockKey(activity.getId());
        redisTemplate.opsForValue().set(stockKey, 
            String.valueOf(activity.getMaxParticipants()), 
            7, TimeUnit.DAYS);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void registerActivity(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        
        if (!"RECRUITING".equals(activity.getStatus())) {
            throw new BusinessException("活动当前不在招募状态");
        }
        
        if (LocalDateTime.now().isAfter(activity.getRegistrationDeadline())) {
            throw new BusinessException("报名已截止");
        }
        
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getUserId, userId)
               .eq(Registration::getActivityId, activityId)
               .eq(Registration::getStatus, "REGISTERED");
        if (registrationMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("您已报名该活动");
        }
        
        String stockKey = RedisKeyConstant.getActivityStockKey(activityId);
        Long stock = redisTemplate.opsForValue().decrement(stockKey);
        
        if (stock == null || stock < 0) {
            redisTemplate.opsForValue().increment(stockKey);
            throw new BusinessException("活动名额已满");
        }
        
        Registration registration = new Registration();
        registration.setUserId(userId);
        registration.setActivityId(activityId);
        registration.setRegistrationTime(LocalDateTime.now());
        registration.setCheckInStatus(0);
        registration.setHoursConfirmed(0);
        registration.setStatus("REGISTERED");
        
        registrationMapper.insert(registration);
        activityMapper.incrementParticipants(activityId);
    }
    
    public List<RegistrationVO> getUserRegistrations(Long userId) {
        return registrationMapper.selectUserRegistrations(userId);
    }

    /**
     * 管理员查看报名记录；activityId 为空则返回全部（有效报名 status=REGISTERED）。
     */
    public List<RegistrationVO> listRegistrationsForAdmin(Long activityId) {
        if (activityId != null) {
            return registrationMapper.selectRegistrationsForAdminByActivityId(activityId);
        }
        return registrationMapper.selectAllRegistrationsForAdmin();
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void confirmHours(Long registrationId) {
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException("报名记录不存在");
        }
        
        if (registration.getHoursConfirmed() == 1) {
            throw new BusinessException("时长已核销");
        }
        
        Activity activity = activityMapper.selectById(registration.getActivityId());
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        
        registration.setHoursConfirmed(1);
        registration.setConfirmTime(LocalDateTime.now());
        registrationMapper.updateById(registration);
        
        userServiceClient.updateVolunteerHours(registration.getUserId(), activity.getVolunteerHours());
    }
}
