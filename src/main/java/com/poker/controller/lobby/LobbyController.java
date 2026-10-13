package com.poker.controller.lobby;

import com.poker.network.client.ClientJson;
import com.poker.network.client.PokerSocketClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.poker.view.LobbyView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Điều khiển danh sách và thao tác tạo phòng ở Lobby. */
public final class LobbyController {
    private final PokerSocketClient api;
    public LobbyController(PokerSocketClient api) { this.api = api; }
    public CompletableFuture<JsonArray> rooms() { return api.list("/api/v1/rooms"); }
    public CompletableFuture<JsonObject> createRoom(JsonObject request) {
        return api.request("POST", "/api/v1/rooms", request);
    }

    public CompletableFuture<CreatedRoom> createRoom(String name, int maxPlayers, long smallBlind,
                                                      long bigBlind, long buyIn, boolean privateRoom,
                                                      String password) {
        JsonObject request = new JsonObject();
        request.addProperty("name", name);
        request.addProperty("roomType", privateRoom ? "PRIVATE" : "PUBLIC");
        request.addProperty("maxPlayers", maxPlayers);
        request.addProperty("smallBlind", smallBlind);
        request.addProperty("bigBlind", bigBlind);
        request.addProperty("buyIn", buyIn);
        if (privateRoom) request.addProperty("password", password);
        return createRoom(request).thenApply(detail -> new CreatedRoom(
                ClientJson.number(detail.getAsJsonObject("room"), "id"), detail));
    }

    public CompletableFuture<LobbyData> loadLobby() {
        CompletableFuture<JsonArray> rooms = rooms();
        CompletableFuture<JsonObject> profile = api.request("GET", "/api/v1/me", null);
        CompletableFuture<JsonObject> online = api.request("GET", "/api/v1/players/online/count", null);
        return rooms.thenCombine(profile, (items, me) -> new Object[]{items, me})
                .thenCombine(online, (parts, presence) -> {
            JsonArray items = (JsonArray) parts[0];
            JsonObject me = (JsonObject) parts[1];
            List<LobbyView.RoomModel> mapped = new ArrayList<>();
            for (JsonElement item : items) {
                JsonObject room = item.getAsJsonObject();
                long count = ClientJson.number(room, "seatedPlayers");
                mapped.add(new LobbyView.RoomModel(
                        "#" + ClientJson.number(room, "id"),
                        ClientJson.string(room, "name"),
                        "#" + ClientJson.number(room, "ownerUserId"),
                        count + "/" + ClientJson.number(room, "maxPlayers"),
                        "$" + ClientJson.number(room, "smallBlind") + "/$" + ClientJson.number(room, "bigBlind"),
                        "$" + ClientJson.number(room, "buyIn"),
                        ClientJson.string(room, "roomType"),
                        ClientJson.string(room, "status")));
            }
            return new LobbyData(mapped, ClientJson.number(me, "accountChips"),
                    ClientJson.number(presence, "onlinePlayers"));
        });
    }

    public record LobbyData(List<LobbyView.RoomModel> rooms, long accountChips, long onlinePlayers) { }
    public record CreatedRoom(long roomId, JsonObject detail) { }
}
