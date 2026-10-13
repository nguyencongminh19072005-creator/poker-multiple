package com.poker.dto;

import java.time.Instant;

public record MatchHistoryDto(long gameSessionId, String roomName, int placement,
                              long chipChange, int handsPlayed, Instant finishedAt) { }
