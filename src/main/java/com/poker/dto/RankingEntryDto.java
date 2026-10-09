package com.poker.dto;

public record RankingEntryDto(long rank, long userId, String username,
                              long rating, long gamesRated) { }
