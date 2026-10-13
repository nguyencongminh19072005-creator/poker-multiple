package com.poker.view.room;

import java.util.ArrayList;
import java.util.List;
import com.poker.model.game.card.Card;

/** Mutable state used only to render one seat; not the authoritative poker player. */
public class SeatViewModel {
    public enum PlayerStatus {
        ACTIVE,
        FOLDED,
        ALL_IN,
        OUT
    }

    public enum PlayerRole {
        NONE,
        DEALER,
        SMALL_BLIND,
        BIG_BLIND
    }

    private final int id;
    private final String name;
    private final String avatarFileName;
    private final boolean isHuman;
    
    private int seatIndex;
    private int chips;
    private int currentBet;
    private PlayerStatus status;
    private PlayerRole role;
    private String lastAction;
    private final List<Card> holeCards = new ArrayList<>();

    public SeatViewModel(int id, String name, String avatarFileName, int initialChips, boolean isHuman) {
        this.id = id;
        this.name = name;
        this.avatarFileName = avatarFileName;
        this.chips = initialChips;
        this.isHuman = isHuman;
        this.status = PlayerStatus.ACTIVE;
        this.role = PlayerRole.NONE;
        this.lastAction = "";
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAvatarFileName() {
        return avatarFileName;
    }

    public int getSeatIndex() {
        return seatIndex;
    }

    public void setSeatIndex(int seatIndex) {
        this.seatIndex = seatIndex;
    }

    public boolean isHuman() {
        return isHuman;
    }

    public int getChips() {
        return chips;
    }

    public void setChips(int chips) {
        this.chips = chips;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public PlayerRole getRole() {
        return role;
    }

    public void setRole(PlayerRole role) {
        this.role = role;
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
    }

    public List<Card> getHoleCards() {
        return holeCards;
    }

    public void addCard(Card card) {
        holeCards.add(card);
    }

}
