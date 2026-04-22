package org.example.messaging;

import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class IdempotencyHelper {

    public String resolveMessageId(Message message) {
        String fromHeader = message.getMessageProperties().getHeader("messageId");
        if (StringUtils.hasText(fromHeader)) {
            return fromHeader;
        }
        String fromMessageProperties = message.getMessageProperties().getMessageId();
        if (StringUtils.hasText(fromMessageProperties)) {
            return fromMessageProperties;
        }
        return fallbackDigest(message.getBody());
    }

    public String newMessageId() {
        return UUID.randomUUID().toString();
    }

    private String fallbackDigest(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(body == null ? new byte[0] : body);
            return "sha256-" + HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            return "rand-" + UUID.nameUUIDFromBytes((body == null ? new byte[0] : body));
        }
    }
}
