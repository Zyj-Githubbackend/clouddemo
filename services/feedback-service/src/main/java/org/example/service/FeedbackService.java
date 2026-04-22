package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.common.exception.BusinessException;
import org.example.dto.FeedbackAttachmentRequest;
import org.example.dto.FeedbackCreateRequest;
import org.example.dto.FeedbackMessageRequest;
import org.example.entity.Feedback;
import org.example.entity.FeedbackMessage;
import org.example.entity.FeedbackMessageAttachment;
import org.example.mapper.EventOutboxMapper;
import org.example.mapper.FeedbackMapper;
import org.example.mapper.FeedbackMessageAttachmentMapper;
import org.example.mapper.FeedbackMessageMapper;
import org.example.messaging.IdempotencyHelper;
import org.example.messaging.MessagingConstants;
import org.example.messaging.outbox.EventOutbox;
import org.example.messaging.outbox.OutboxStatus;
import org.example.vo.FeedbackAttachmentVO;
import org.example.vo.FeedbackDetailVO;
import org.example.vo.FeedbackMessageVO;
import org.example.vo.FeedbackVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_REPLIED = "REPLIED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String ROLE_VOLUNTEER = "VOLUNTEER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SYSTEM = "SYSTEM";

    private static final int MAX_ATTACHMENTS_PER_MESSAGE = 6;
    private static final Set<String> CATEGORIES = Set.of("QUESTION", "SUGGESTION", "BUG", "COMPLAINT", "OTHER");
    private static final Set<String> STATUSES = Set.of(STATUS_OPEN, STATUS_REPLIED, STATUS_CLOSED, STATUS_REJECTED);
    private static final Set<String> PRIORITIES = Set.of("LOW", "NORMAL", "HIGH", "URGENT");
    private static final Set<String> FILE_TYPES = Set.of("IMAGE", "FILE");

    private final FeedbackMapper feedbackMapper;
    private final FeedbackMessageMapper messageMapper;
    private final FeedbackMessageAttachmentMapper attachmentMapper;
    private final EventOutboxMapper eventOutboxMapper;
    private final MinioStorageService minioStorageService;
    private final IdempotencyHelper idempotencyHelper;
    private final ObjectMapper objectMapper;

    public FeedbackService(FeedbackMapper feedbackMapper,
                           FeedbackMessageMapper messageMapper,
                           FeedbackMessageAttachmentMapper attachmentMapper,
                           EventOutboxMapper eventOutboxMapper,
                           MinioStorageService minioStorageService,
                           IdempotencyHelper idempotencyHelper,
                           ObjectMapper objectMapper) {
        this.feedbackMapper = feedbackMapper;
        this.messageMapper = messageMapper;
        this.attachmentMapper = attachmentMapper;
        this.eventOutboxMapper = eventOutboxMapper;
        this.minioStorageService = minioStorageService;
        this.idempotencyHelper = idempotencyHelper;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public FeedbackDetailVO createFeedback(FeedbackCreateRequest request, Long userId) {
        validateCreateRequest(request);
        LocalDateTime now = LocalDateTime.now();

        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setTitle(request.getTitle().trim());
        feedback.setCategory(request.getCategory().trim());
        feedback.setStatus(STATUS_OPEN);
        feedback.setPriority("NORMAL");
        feedback.setLastMessageTime(now);
        feedback.setLastReplierRole(ROLE_VOLUNTEER);
        feedbackMapper.insert(feedback);

        FeedbackMessage message = createMessage(feedback, userId, ROLE_VOLUNTEER, request.getContent(), request.getAttachments());
        eventOutboxMapper.insert(buildFeedbackCreatedOutbox(feedback, message, userId));
        log.info("created feedback feedbackId={} userId={} category={} messageId={}",
                feedback.getId(), userId, feedback.getCategory(), message.getId());
        return getFeedbackDetail(feedback.getId(), userId, ROLE_VOLUNTEER);
    }

    private EventOutbox buildFeedbackCreatedOutbox(Feedback feedback, FeedbackMessage message, Long userId) {
        EventOutbox outbox = new EventOutbox();
        outbox.setMessageId(idempotencyHelper.newMessageId());
        outbox.setEventType(MessagingConstants.ROUTING_FEEDBACK_CREATED);
        outbox.setAggregateType("feedback");
        outbox.setAggregateId(String.valueOf(feedback.getId()));
        outbox.setPayloadJson(buildFeedbackCreatedPayload(feedback, message, userId));
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setRetryCount(0);
        outbox.setNextRetryTime(LocalDateTime.now());
        outbox.setCreatedAt(LocalDateTime.now());
        return outbox;
    }

    private String buildFeedbackCreatedPayload(Feedback feedback, FeedbackMessage message, Long userId) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("feedbackId", feedback.getId());
        payload.put("messageId", message.getId());
        payload.put("userId", userId);
        payload.put("title", feedback.getTitle());
        payload.put("category", feedback.getCategory());
        payload.put("status", feedback.getStatus());
        payload.put("stackId", System.getenv().getOrDefault("STACK_ID", "single"));
        payload.put("createdAt", feedback.getCreateTime() == null ? LocalDateTime.now().toString() : feedback.getCreateTime().toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize feedback.created payload", ex);
        }
    }

    public IPage<FeedbackVO> listMyFeedback(Integer page, Integer size, String status, Long userId) {
        LambdaQueryWrapper<Feedback> wrapper = baseListWrapper(status, null, null, null)
                .eq(Feedback::getUserId, userId);
        return listFeedbackPage(page, size, wrapper);
    }

    public IPage<FeedbackVO> listAdminFeedback(Integer page, Integer size, String status,
                                               String category, String priority, String keyword) {
        LambdaQueryWrapper<Feedback> wrapper = baseListWrapper(status, category, priority, keyword);
        return listFeedbackPage(page, size, wrapper);
    }

    public FeedbackDetailVO getFeedbackDetail(Long feedbackId, Long userId, String role) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        ensureCanView(feedback, userId, role);
        return toDetailVO(feedback);
    }

    @Transactional(rollbackFor = Exception.class)
    public FeedbackDetailVO replyAsUser(Long feedbackId, FeedbackMessageRequest request, Long userId) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        ensureOwner(feedback, userId);
        ensureCanReply(feedback);
        validateMessageRequest(request);

        FeedbackMessage message = createMessage(feedback, userId, ROLE_VOLUNTEER, request.getContent(), request.getAttachments());
        markLastMessage(feedback, STATUS_OPEN, ROLE_VOLUNTEER);
        log.info("user replied feedback feedbackId={} userId={} messageId={}",
                feedbackId, userId, message.getId());
        return getFeedbackDetail(feedbackId, userId, ROLE_VOLUNTEER);
    }

    @Transactional(rollbackFor = Exception.class)
    public FeedbackDetailVO replyAsAdmin(Long feedbackId, FeedbackMessageRequest request, Long adminId) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        ensureCanReply(feedback);
        validateMessageRequest(request);

        FeedbackMessage message = createMessage(feedback, adminId, ROLE_ADMIN, request.getContent(), request.getAttachments());
        markLastMessage(feedback, STATUS_REPLIED, ROLE_ADMIN);
        log.info("admin replied feedback feedbackId={} adminId={} messageId={}",
                feedbackId, adminId, message.getId());
        return getFeedbackDetail(feedbackId, adminId, ROLE_ADMIN);
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeByUser(Long feedbackId, Long userId) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        ensureOwner(feedback, userId);
        ensureNotTerminal(feedback);
        log.info("user closing feedback feedbackId={} userId={}", feedbackId, userId);
        closeFeedback(feedback, userId, "用户已确认解决并关闭反馈");
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeByAdmin(Long feedbackId, Long adminId) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        ensureNotTerminal(feedback);
        log.info("admin closing feedback feedbackId={} adminId={}", feedbackId, adminId);
        closeFeedback(feedback, adminId, "管理员已关闭该反馈");
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectByAdmin(Long feedbackId, Long adminId, String reason) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        ensureNotTerminal(feedback);
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("Reject reason cannot be empty");
        }
        String normalizedReason = reason.trim();
        if (normalizedReason.length() > 500) {
            throw new BusinessException("Reject reason cannot exceed 500 characters");
        }

        feedback.setStatus(STATUS_REJECTED);
        feedback.setRejectReason(normalizedReason);
        feedback.setClosedBy(adminId);
        feedback.setClosedTime(LocalDateTime.now());
        feedback.setLastMessageTime(LocalDateTime.now());
        feedback.setLastReplierRole(ROLE_SYSTEM);
        feedbackMapper.updateById(feedback);
        log.info("admin finished feedback terminal action feedbackId={} adminId={} status={}",
                feedbackId, adminId, feedback.getStatus());
        createSystemMessage(feedback.getId(), adminId, "管理员已驳回该反馈：" + normalizedReason);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePriority(Long feedbackId, String priority) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        if (!StringUtils.hasText(priority) || !PRIORITIES.contains(priority.trim())) {
            throw new BusinessException("Unsupported feedback priority");
        }
        feedback.setPriority(priority.trim());
        feedbackMapper.updateById(feedback);
        log.info("updated feedback priority feedbackId={} priority={}", feedbackId, feedback.getPriority());
    }

    public FeedbackMessageAttachment verifyAttachmentAccess(String objectKey, Long userId, String role) {
        if (!StringUtils.hasText(objectKey)) {
            throw new BusinessException("Attachment object key cannot be empty");
        }
        FeedbackMessageAttachment attachment = attachmentMapper.selectOne(
                new LambdaQueryWrapper<FeedbackMessageAttachment>()
                        .eq(FeedbackMessageAttachment::getObjectKey, objectKey)
                        .last("LIMIT 1")
        );
        if (attachment == null) {
            throw new BusinessException(403, "Attachment is not bound to an accessible feedback");
        }
        Feedback feedback = getFeedbackOrThrow(attachment.getFeedbackId());
        ensureCanView(feedback, userId, role);
        log.info("verified feedback attachment access feedbackId={} attachmentId={} userId={} role={}",
                feedback.getId(), attachment.getId(), userId, role);
        return attachment;
    }

    private IPage<FeedbackVO> listFeedbackPage(Integer page, Integer size, LambdaQueryWrapper<Feedback> wrapper) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        long total = feedbackMapper.selectCount(wrapper);
        List<Feedback> records = feedbackMapper.selectList(
                wrapper.last(limitClause(normalizedPage, normalizedSize))
        );
        Page<Feedback> result = new Page<>(normalizedPage, normalizedSize, total);
        result.setRecords(records);
        return result.convert(this::toVO);
    }

    private LambdaQueryWrapper<Feedback> baseListWrapper(String status, String category, String priority, String keyword) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            validateStatus(status);
            wrapper.eq(Feedback::getStatus, status.trim());
        }
        if (StringUtils.hasText(category)) {
            validateCategory(category);
            wrapper.eq(Feedback::getCategory, category.trim());
        }
        if (StringUtils.hasText(priority)) {
            validatePriority(priority);
            wrapper.eq(Feedback::getPriority, priority.trim());
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Feedback::getTitle, keyword.trim())
                    .or()
                    .like(Feedback::getRejectReason, keyword.trim()));
        }
        return wrapper.orderByDesc(Feedback::getLastMessageTime)
                .orderByDesc(Feedback::getUpdateTime)
                .orderByDesc(Feedback::getId);
    }

    private Feedback getFeedbackOrThrow(Long feedbackId) {
        Feedback feedback = feedbackMapper.selectById(feedbackId);
        if (feedback == null) {
            throw new BusinessException("Feedback not found");
        }
        return feedback;
    }

    private void validateCreateRequest(FeedbackCreateRequest request) {
        if (request == null) {
            throw new BusinessException("Feedback request cannot be empty");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessException("Feedback title cannot be empty");
        }
        if (request.getTitle().trim().length() > 200) {
            throw new BusinessException("Feedback title cannot exceed 200 characters");
        }
        validateCategory(request.getCategory());
        validateMessageContentAndAttachments(request.getContent(), request.getAttachments());
    }

    private void validateMessageRequest(FeedbackMessageRequest request) {
        if (request == null) {
            throw new BusinessException("Message request cannot be empty");
        }
        validateMessageContentAndAttachments(request.getContent(), request.getAttachments());
    }

    private void validateMessageContentAndAttachments(String content, List<FeedbackAttachmentRequest> attachments) {
        if (!StringUtils.hasText(content) && normalizeAttachments(attachments).isEmpty()) {
            throw new BusinessException("Message content or attachments cannot be empty");
        }
        if (content != null && content.length() > 5000) {
            throw new BusinessException("Message content cannot exceed 5000 characters");
        }
    }

    private void validateCategory(String category) {
        if (!StringUtils.hasText(category) || !CATEGORIES.contains(category.trim())) {
            throw new BusinessException("Unsupported feedback category");
        }
    }

    private void validateStatus(String status) {
        if (!StringUtils.hasText(status) || !STATUSES.contains(status.trim())) {
            throw new BusinessException("Unsupported feedback status");
        }
    }

    private void validatePriority(String priority) {
        if (!StringUtils.hasText(priority) || !PRIORITIES.contains(priority.trim())) {
            throw new BusinessException("Unsupported feedback priority");
        }
    }

    private void ensureCanView(Feedback feedback, Long userId, String role) {
        if (ROLE_ADMIN.equals(role) || feedback.getUserId().equals(userId)) {
            return;
        }
        throw new BusinessException(403, "You can only view your own feedback");
    }

    private void ensureOwner(Feedback feedback, Long userId) {
        if (!feedback.getUserId().equals(userId)) {
            throw new BusinessException(403, "You can only operate your own feedback");
        }
    }

    private void ensureCanReply(Feedback feedback) {
        ensureNotTerminal(feedback);
    }

    private void ensureNotTerminal(Feedback feedback) {
        if (STATUS_CLOSED.equals(feedback.getStatus()) || STATUS_REJECTED.equals(feedback.getStatus())) {
            throw new BusinessException("Closed or rejected feedback cannot be updated");
        }
    }

    private void closeFeedback(Feedback feedback, Long operatorId, String systemContent) {
        feedback.setStatus(STATUS_CLOSED);
        feedback.setClosedBy(operatorId);
        feedback.setClosedTime(LocalDateTime.now());
        feedback.setLastMessageTime(LocalDateTime.now());
        feedback.setLastReplierRole(ROLE_SYSTEM);
        feedbackMapper.updateById(feedback);
        createSystemMessage(feedback.getId(), operatorId, systemContent);
    }

    private FeedbackMessage createMessage(Feedback feedback, Long senderId, String senderRole,
                                          String content, List<FeedbackAttachmentRequest> rawAttachments) {
        List<AttachmentInput> attachments = normalizeAttachments(rawAttachments);
        FeedbackMessage message = new FeedbackMessage();
        message.setFeedbackId(feedback.getId());
        message.setSenderId(senderId);
        message.setSenderRole(senderRole);
        message.setContent(StringUtils.hasText(content) ? content.trim() : null);
        message.setMessageType(attachments.isEmpty() ? "TEXT" : "MIXED");
        messageMapper.insert(message);
        insertAttachments(feedback.getId(), message.getId(), attachments);
        log.info("created feedback message feedbackId={} messageId={} senderId={} senderRole={} attachmentCount={}",
                feedback.getId(), message.getId(), senderId, senderRole, attachments.size());
        return message;
    }

    private void createSystemMessage(Long feedbackId, Long senderId, String content) {
        FeedbackMessage message = new FeedbackMessage();
        message.setFeedbackId(feedbackId);
        message.setSenderId(senderId);
        message.setSenderRole(ROLE_SYSTEM);
        message.setContent(content);
        message.setMessageType("SYSTEM");
        messageMapper.insert(message);
        log.info("created feedback system message feedbackId={} messageId={} senderId={}",
                feedbackId, message.getId(), senderId);
    }

    private void markLastMessage(Feedback feedback, String status, String role) {
        feedback.setStatus(status);
        feedback.setLastMessageTime(LocalDateTime.now());
        feedback.setLastReplierRole(role);
        feedbackMapper.updateById(feedback);
    }

    private void insertAttachments(Long feedbackId, Long messageId, List<AttachmentInput> attachments) {
        for (AttachmentInput input : attachments) {
            FeedbackMessageAttachment attachment = new FeedbackMessageAttachment();
            attachment.setFeedbackId(feedbackId);
            attachment.setMessageId(messageId);
            attachment.setObjectKey(input.attachmentKey());
            attachment.setOriginalName(input.fileName());
            attachment.setContentType(input.contentType());
            attachment.setFileSize(input.fileSize());
            attachment.setFileType(input.fileType());
            attachmentMapper.insert(attachment);
        }
    }

    private List<AttachmentInput> normalizeAttachments(List<FeedbackAttachmentRequest> rawAttachments) {
        if (rawAttachments == null || rawAttachments.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> seenKeys = new LinkedHashSet<>();
        List<AttachmentInput> normalized = new ArrayList<>();
        for (FeedbackAttachmentRequest attachment : rawAttachments) {
            if (attachment == null || !StringUtils.hasText(attachment.getAttachmentKey())) {
                continue;
            }
            String attachmentKey = attachment.getAttachmentKey().trim();
            if (!seenKeys.add(attachmentKey)) {
                continue;
            }
            if (normalized.size() >= MAX_ATTACHMENTS_PER_MESSAGE) {
                throw new BusinessException("Each message can have at most 6 attachments");
            }
            String fileType = StringUtils.hasText(attachment.getFileType()) ? attachment.getFileType().trim() : "FILE";
            if (!FILE_TYPES.contains(fileType)) {
                throw new BusinessException("Unsupported feedback attachment file type");
            }
            String fileName = StringUtils.hasText(attachment.getFileName())
                    ? attachment.getFileName().trim()
                    : attachmentKey;
            String contentType = StringUtils.hasText(attachment.getContentType())
                    ? attachment.getContentType().trim()
                    : null;
            normalized.add(new AttachmentInput(
                    attachmentKey,
                    fileName,
                    contentType,
                    attachment.getFileSize() == null ? 0L : Math.max(0L, attachment.getFileSize()),
                    fileType
            ));
        }
        return normalized;
    }

    private FeedbackDetailVO toDetailVO(Feedback feedback) {
        FeedbackDetailVO vo = new FeedbackDetailVO();
        BeanUtils.copyProperties(feedback, vo);

        List<FeedbackMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<FeedbackMessage>()
                        .eq(FeedbackMessage::getFeedbackId, feedback.getId())
                        .orderByAsc(FeedbackMessage::getCreateTime)
                        .orderByAsc(FeedbackMessage::getId)
        );
        if (messages == null || messages.isEmpty()) {
            vo.setMessages(Collections.emptyList());
            return vo;
        }

        List<Long> messageIds = messages.stream().map(FeedbackMessage::getId).toList();
        Map<Long, List<FeedbackAttachmentVO>> attachmentsByMessage = new HashMap<>();
        List<FeedbackMessageAttachment> attachments = attachmentMapper.selectList(
                new LambdaQueryWrapper<FeedbackMessageAttachment>()
                        .in(FeedbackMessageAttachment::getMessageId, messageIds)
                        .orderByAsc(FeedbackMessageAttachment::getId)
        );
        for (FeedbackMessageAttachment attachment : attachments) {
            attachmentsByMessage
                    .computeIfAbsent(attachment.getMessageId(), key -> new ArrayList<>())
                    .add(toAttachmentVO(attachment));
        }

        List<FeedbackMessageVO> messageVOs = new ArrayList<>();
        for (FeedbackMessage message : messages) {
            FeedbackMessageVO messageVO = new FeedbackMessageVO();
            BeanUtils.copyProperties(message, messageVO);
            messageVO.setAttachments(attachmentsByMessage.getOrDefault(message.getId(), Collections.emptyList()));
            messageVOs.add(messageVO);
        }
        vo.setMessages(messageVOs);
        return vo;
    }

    private FeedbackVO toVO(Feedback feedback) {
        FeedbackVO vo = new FeedbackVO();
        BeanUtils.copyProperties(feedback, vo);
        return vo;
    }

    private FeedbackAttachmentVO toAttachmentVO(FeedbackMessageAttachment attachment) {
        FeedbackAttachmentVO vo = new FeedbackAttachmentVO();
        vo.setId(attachment.getId());
        vo.setAttachmentKey(attachment.getObjectKey());
        vo.setFileName(attachment.getOriginalName());
        vo.setContentType(attachment.getContentType());
        vo.setFileSize(attachment.getFileSize());
        vo.setFileType(attachment.getFileType());
        vo.setUrl(minioStorageService.buildFeedbackAttachmentUrl(
                attachment.getObjectKey(),
                attachment.getOriginalName()
        ));
        vo.setCreateTime(attachment.getCreateTime());
        return vo;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    private String limitClause(int page, int size) {
        int offset = (page - 1) * size;
        return "LIMIT " + size + " OFFSET " + offset;
    }

    private record AttachmentInput(
            String attachmentKey,
            String fileName,
            String contentType,
            Long fileSize,
            String fileType
    ) {
    }
}
