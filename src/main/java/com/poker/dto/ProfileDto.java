package com.poker.dto;

public record ProfileDto(long userId, String displayName, String avatarUrl,
                         String onlineStatus, long accountChips) { }
