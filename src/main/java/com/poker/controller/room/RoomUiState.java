package com.poker.controller.room;

/** Trạng thái trình bày của phòng; không chứa hoặc thay thế luật Poker trên server. */
public enum RoomUiState {
    SPECTATING,
    WAITING,
    READY,
    PLAYING_NOT_MY_TURN,
    PLAYING_MY_TURN,
    GAME_FINISHED
}
