package com.poker.model.game.card;

import java.util.Objects;

/** An immutable playing-card value. */
public record Card(Rank rank, Suit suit) implements Comparable<Card> {

    public Card {
        Objects.requireNonNull(rank, "rank must not be null");
        Objects.requireNonNull(suit, "suit must not be null");
    }

    public Card(Suit suit, Rank rank) { this(rank, suit); }
    public Rank getRank() { return rank; }
    public Suit getSuit() { return suit; }
    public String getImageRelativePath() {
        return "png cards/card fronts/" + suit.folderName() + "/" +
                rank.fileName() + " of " + suit.folderName() + ".png";
    }
    public String getDisplayName() { return rank.symbol() + suit.symbol(); }
    @Override public int compareTo(Card other) { return Integer.compare(rank.strength(), other.rank.strength()); }
}
