package com.poker.dto;

public record RoomMemberDto(long userId, String username, Integer seatNumber,
                            String state, long tableChips) { }
