package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.example.vo.FeedbackDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackMapper feedbackMapper;

    @Mock
    private FeedbackMessageMapper messageMapper;

    @Mock
    private FeedbackMessageAttachmentMapper attachmentMapper;

    @Mock
    private EventOutboxMapper eventOutboxMapper;

    @Mock
    private MinioStorageService minioStorageService;

    @Mock
    private IdempotencyHelper idempotencyHelper;

    private FeedbackService feedbackService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        feedbackService = new FeedbackService(
                feedbackMapper,
                messageMapper,
                attachmentMapper,
                eventOutboxMapper,
                minioStorageService,
                idempotencyHelper,
                objectMapper
        );
    }

    @Test
    void createFeedbackShouldNormalizeAttachmentsAndPublishOutbox() throws Exception {
        AtomicReference<Feedback> savedFeedback = new AtomicReference<>();
        AtomicReference<FeedbackMessage> savedMessage = new AtomicReference<>();
        List<FeedbackMessageAttachment> savedAttachments = new ArrayList<>();

        doAnswer(invocation -> {
            Feedback feedback = invocation.getArgument(0);
            feedback.setId(101L);
            savedFeedback.set(feedback);
            return 1;
        }).when(feedbackMapper).insert(any(Feedback.class));
        doAnswer(invocation -> {
            FeedbackMessage message = invocation.getArgument(0);
            message.setId(201L);
            savedMessage.set(message);
            return 1;
        }).when(messageMapper).insert(any(FeedbackMessage.class));
        doAnswer(invocation -> {
            FeedbackMessageAttachment attachment = invocation.getArgument(0);
            attachment.setId((long) savedAttachments.size() + 1);
            savedAttachments.add(attachment);
            return 1;
        }).when(attachmentMapper).insert(any(FeedbackMessageAttachment.class));

        when(idempotencyHelper.newMessageId()).thenReturn("message-1");
        when(feedbackMapper.selectById(101L)).thenAnswer(invocation -> savedFeedback.get());
        when(messageMapper.selectList(any())).thenAnswer(invocation -> List.of(savedMessage.get()));
        when(attachmentMapper.selectList(any())).thenAnswer(invocation -> savedAttachments);
        when(minioStorageService.buildFeedbackAttachmentUrl("feedback/a.png", "a.png"))
                .thenReturn("http://minio/feedback/a.png");

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setTitle("  设备问题  ");
        request.setCategory("BUG");
        request.setContent("  附件里有截图  ");
        request.setAttachments(List.of(
                attachment(" feedback/a.png ", " a.png ", "image/png", 120L, "IMAGE"),
                attachment("feedback/a.png", "duplicate.png", "image/png", 120L, "IMAGE"),
                attachment(" feedback/log.txt ", null, "text/plain", -5L, "FILE"),
                attachment("   ", "empty.png", "image/png", 1L, "IMAGE")
        ));

        FeedbackDetailVO detail = feedbackService.createFeedback(request, 7L);

        assertEquals(101L, detail.getId());
        assertEquals(FeedbackService.STATUS_OPEN, savedFeedback.get().getStatus());
        assertEquals("设备问题", savedFeedback.get().getTitle());
        assertEquals("NORMAL", savedFeedback.get().getPriority());
        assertEquals("附件里有截图", savedMessage.get().getContent());
        assertEquals("MIXED", savedMessage.get().getMessageType());
        assertEquals(2, savedAttachments.size());
        assertEquals("feedback/a.png", savedAttachments.get(0).getObjectKey());
        assertEquals("a.png", savedAttachments.get(0).getOriginalName());
        assertEquals("feedback/log.txt", savedAttachments.get(1).getObjectKey());
        assertEquals("feedback/log.txt", savedAttachments.get(1).getOriginalName());
        assertEquals(0L, savedAttachments.get(1).getFileSize());
        assertFalse(detail.getMessages().isEmpty());

        ArgumentCaptor<EventOutbox> outboxCaptor = ArgumentCaptor.forClass(EventOutbox.class);
        verify(eventOutboxMapper).insert(outboxCaptor.capture());
        EventOutbox outbox = outboxCaptor.getValue();
        assertEquals("message-1", outbox.getMessageId());
        assertEquals(MessagingConstants.ROUTING_FEEDBACK_CREATED, outbox.getEventType());
        JsonNode payload = objectMapper.readTree(outbox.getPayloadJson());
        assertEquals(101L, payload.path("feedbackId").asLong());
        assertEquals(201L, payload.path("messageId").asLong());
        assertEquals(7L, payload.path("userId").asLong());
    }

    @Test
    void createFeedbackShouldRejectUnsupportedCategory() {
        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setTitle("问题");
        request.setCategory("UNKNOWN");
        request.setContent("内容");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> feedbackService.createFeedback(request, 7L));

        assertEquals("Unsupported feedback category", ex.getMessage());
        verify(feedbackMapper, never()).insert(any(Feedback.class));
        verify(messageMapper, never()).insert(any(FeedbackMessage.class));
    }

    @Test
    void replyAsUserShouldRejectClosedFeedback() {
        Feedback feedback = feedback(11L, 7L, FeedbackService.STATUS_CLOSED);
        FeedbackMessageRequest request = new FeedbackMessageRequest();
        request.setContent("还有问题");

        when(feedbackMapper.selectById(11L)).thenReturn(feedback);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> feedbackService.replyAsUser(11L, request, 7L));

        assertEquals("Closed or rejected feedback cannot be updated", ex.getMessage());
        verify(messageMapper, never()).insert(any(FeedbackMessage.class));
        verify(feedbackMapper, never()).updateById(any(Feedback.class));
    }

    @Test
    void verifyAttachmentAccessShouldOnlyAllowOwnerOrAdmin() {
        FeedbackMessageAttachment attachment = new FeedbackMessageAttachment();
        attachment.setId(3L);
        attachment.setFeedbackId(11L);
        attachment.setObjectKey("feedback/private.png");
        Feedback feedback = feedback(11L, 7L, FeedbackService.STATUS_OPEN);

        when(attachmentMapper.selectOne(any())).thenReturn(attachment);
        when(feedbackMapper.selectById(11L)).thenReturn(feedback);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> feedbackService.verifyAttachmentAccess("feedback/private.png", 8L, FeedbackService.ROLE_VOLUNTEER));

        assertEquals(403, ex.getCode());

        FeedbackMessageAttachment allowed = feedbackService.verifyAttachmentAccess(
                "feedback/private.png",
                99L,
                FeedbackService.ROLE_ADMIN
        );
        assertNotNull(allowed);
        assertEquals("feedback/private.png", allowed.getObjectKey());
    }

    private FeedbackAttachmentRequest attachment(String key, String fileName, String contentType, Long fileSize, String fileType) {
        FeedbackAttachmentRequest attachment = new FeedbackAttachmentRequest();
        attachment.setAttachmentKey(key);
        attachment.setFileName(fileName);
        attachment.setContentType(contentType);
        attachment.setFileSize(fileSize);
        attachment.setFileType(fileType);
        return attachment;
    }

    private Feedback feedback(Long id, Long userId, String status) {
        Feedback feedback = new Feedback();
        feedback.setId(id);
        feedback.setUserId(userId);
        feedback.setTitle("反馈");
        feedback.setCategory("BUG");
        feedback.setStatus(status);
        feedback.setPriority("NORMAL");
        feedback.setLastReplierRole(FeedbackService.ROLE_VOLUNTEER);
        return feedback;
    }
}
