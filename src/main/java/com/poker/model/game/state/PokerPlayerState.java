package com.poker.model.game.state;

/** A player's state inside an active Poker hand, independent of room readiness. */
public enum PokerPlayerState {
    ACTIVE,
    FOLDED,
    ALL_IN
}
