package com.poker.model.game;

import com.poker.model.game.pot.PotType;
import com.poker.model.game.pot.UncalledBetReturn;
import com.poker.model.game.card.Card;
import com.poker.model.game.card.Rank;
import com.poker.model.game.card.Suit;
import com.poker.model.game.state.GamePhase;
import com.poker.model.game.state.PokerPlayer;
import com.poker.model.game.state.PokerPlayerState;
import com.poker.model.game.settlement.HandSettlementResult;
import com.poker.model.game.settlement.PotAward;
import com.poker.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HandResultTests {
    @Test
    void foldedPlayersCardsStayPrivateUntilSettlementThenEveryoneCanSeeThem() {
        List<Card> holeCards = List.of(new Card(Rank.ACE, Suit.SPADES),
                new Card(Rank.KING, Suit.HEARTS));
        PokerPlayer folded = new PokerPlayer(3L, 2, 900, 0, 100,
                PokerPlayerState.FOLDED, holeCards);
        GameOperations.HandResult result = new GameOperations.HandResult(
                200, Map.of(1L, 200L), List.of(), true);

        assertThat(GameOperations.showdownCardsFor(GamePhase.RIVER, null, folded)).isEmpty();
        assertThat(GameOperations.showdownCardsFor(GamePhase.FINISHED, null, folded)).isEmpty();
        assertThat(GameOperations.showdownCardsFor(GamePhase.FINISHED, result, folded))
                .containsExactlyElementsOf(holeCards);
    }

    @Test
    void displaysContestedPotAndActualWinnersWithoutCountingUncalledReturn() {
        PotAward main = new PotAward(PotType.MAIN, 0, 100, List.of(1L, 2L),
                Optional.empty(), 50, List.of(), Map.of(1L, 50L, 2L, 50L));
        PotAward side = new PotAward(PotType.SIDE, 1, 40, List.of(2L),
                Optional.empty(), 40, List.of(), Map.of(2L, 40L));
        HandSettlementResult settled = new HandSettlementResult(Map.of(), List.of(),
                List.of(main, side), List.of(new UncalledBetReturn(2L, 20)),
                Map.of(1L, 50L, 2L, 110L), Map.of(1L, 50L, 2L, 110L),
                160, false, 1_000, 1_000);

        GameOperations.HandResult result = GameOperations.HandResult.from(settled);

        assertThat(result.totalPot()).isEqualTo(140);
        assertThat(result.payouts()).containsEntry(1L, 50L).containsEntry(2L, 90L);
        assertThat(result.tiedWinnerUserIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(result.foldOnly()).isFalse();
        com.google.gson.JsonObject wire = com.google.gson.JsonParser.parseString(
                JsonUtil.toJson(result)).getAsJsonObject();
        assertThat(wire.get("totalPot").getAsLong()).isEqualTo(140);
        assertThat(wire.getAsJsonObject("payouts").get("2").getAsLong()).isEqualTo(90);
    }
}
