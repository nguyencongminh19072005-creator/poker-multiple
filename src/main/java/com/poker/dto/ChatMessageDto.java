package com.poker.dto;

import java.time.Instant;

public record ChatMessageDto(long id, String type, Long roomId, long senderId,
                             Long recipientId, String senderName, String content,
                             Instant createdAt) { }
