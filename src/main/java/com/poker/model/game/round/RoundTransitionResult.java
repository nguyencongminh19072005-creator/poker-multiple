package com.poker.model.game.round;

import com.poker.model.game.betting.BettingResult;
import com.poker.model.game.card.Card;
import com.poker.model.game.state.GamePhase;

import java.util.List;
import java.util.UUID;

/** Immutable result of one coordinated action, turn update, and optional street transition. */
public record RoundTransitionResult(
        BettingResult bettingResult,
        GamePhase resultingPhase,
        Long previousActorUserId,
        Long currentActorUserId,
        boolean bettingRoundCompleted,
        boolean streetAdvanced,
        boolean handDecidedByFold,
        List<Card> communityCardsDealt,
        long stateVersion,
        UUID turnId) {

    public RoundTransitionResult {
        communityCardsDealt = List.copyOf(communityCardsDealt);
    }
}
