package com.poker.network.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.poker.model.game.card.Card;
import com.poker.model.game.card.Rank;
import com.poker.model.game.card.Suit;

import java.util.ArrayList;
import java.util.List;

/** Small conversion helpers for the server's JSON snapshots. */
public final class ClientJson {
    private ClientJson() { }

    public static String string(JsonObject data, String key) {
        return data == null || !data.has(key) || data.get(key).isJsonNull()
                ? "" : data.get(key).getAsString();
    }

    public static long number(JsonObject data, String key) {
        try { return Long.parseLong(string(data, key)); }
        catch (NumberFormatException error) { return 0; }
    }

    public static boolean bool(JsonObject data, String key) {
        return data != null && data.has(key) && !data.get(key).isJsonNull()
                && data.get(key).getAsBoolean();
    }

    public static long parseId(String value) {
        try { return Long.parseLong(value.replace("#", "").trim()); }
        catch (NumberFormatException error) { return -1; }
    }

    public static String cause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage();
    }

    public static List<Card> cards(JsonArray raw) {
        List<Card> result = new ArrayList<>();
        if (raw == null) return result;
        for (JsonElement element : raw) {
            JsonObject card = element.getAsJsonObject();
            result.add(new Card(Suit.valueOf(string(card, "suit")), Rank.valueOf(string(card, "rank"))));
        }
        return result;
    }
}
