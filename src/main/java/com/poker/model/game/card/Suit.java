package com.poker.model.game.card;

/** The four suits in a standard deck of playing cards. */
public enum Suit {
    CLUBS,
    DIAMONDS,
    HEARTS,
    SPADES;

    public String folderName() { return name().toLowerCase(); }
    public String symbol() {
        return switch (this) {
            case CLUBS -> "♣";
            case DIAMONDS -> "♦";
            case HEARTS -> "♥";
            case SPADES -> "♠";
        };
    }
}
