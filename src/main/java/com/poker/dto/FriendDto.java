package com.poker.dto;

public record FriendDto(long requestId, long userId, String displayName, String avatarUrl,
                        String status, String presenceStatus) { }
