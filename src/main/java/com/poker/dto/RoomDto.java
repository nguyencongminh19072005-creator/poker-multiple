package com.poker.dto;

public record RoomDto(long id, String name, long ownerUserId, String roomType,
                      int maxPlayers, int seatedPlayers, long smallBlind,
                      long bigBlind, long buyIn, String status) { }
