package com.poker.controller.friend;

import com.poker.network.client.ClientJson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.scene.Parent;
import com.poker.view.FriendsView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Luồng màn hình bạn bè của Trịnh Quốc An; dữ liệu đi qua WebSocket. */
public final class FriendScreenController {
    private final FriendController friends;
    private final Consumer<Parent> show;
    private final Runnable back;
    private final Consumer<String> error;
    private FriendsView view;

    public FriendScreenController(FriendController friends, Consumer<Parent> show,
                           Runnable back, Consumer<String> error) {
        this.friends = friends;
        this.show = show;
        this.back = back;
        this.error = error;
    }

    public void show() {
        view = new FriendsView(new FriendsView.Actions() {
            @Override public void onBack() { back.run(); }
            @Override public void onSendRequest(String value) {
                long userId = ClientJson.parseId(value);
                if (userId <= 0) { error.accept("Nhập ID người chơi hợp lệ"); return; }
                perform(friends.sendRequest(userId), ignored -> { view.clearFriendId(); refresh(); });
            }
            @Override public void onRemove(long userId) { perform(friends.remove(userId), ignored -> refresh()); }
            @Override public void onAccept(long requestId) { perform(friends.accept(requestId), ignored -> refresh()); }
            @Override public void onReject(long requestId) { perform(friends.reject(requestId), ignored -> refresh()); }
        });
        show.accept(view);
        refresh();
    }

    public void refresh() {
        if (view == null) return;
        var list = friends.listFriends();
        var incoming = friends.incomingRequests();
        CompletableFuture.allOf(list, incoming).thenRun(() -> Platform.runLater(() -> {
            List<FriendsView.FriendModel> friendModels = new ArrayList<>();
            for (JsonElement element : list.join()) {
                JsonObject entry = element.getAsJsonObject();
                JsonObject player = entry.getAsJsonObject("otherPlayer");
                friendModels.add(new FriendsView.FriendModel(ClientJson.number(player, "userId"),
                        ClientJson.string(player, "displayName"), ClientJson.string(entry, "presenceStatus")));
            }
            List<FriendsView.RequestModel> requestModels = new ArrayList<>();
            for (JsonElement element : incoming.join()) {
                JsonObject entry = element.getAsJsonObject();
                if (!"PENDING".equals(ClientJson.string(entry, "status"))) continue;
                requestModels.add(new FriendsView.RequestModel(ClientJson.number(entry, "requestId"),
                        ClientJson.string(entry.getAsJsonObject("otherPlayer"), "displayName")));
            }
            if (view != null) view.showData(friendModels, requestModels);
        })).exceptionally(failure -> { Platform.runLater(() -> error.accept(ClientJson.cause(failure))); return null; });
    }

    public void clear() { view = null; }

    private <T> void perform(CompletableFuture<T> task, Consumer<T> success) {
        task.thenAccept(value -> Platform.runLater(() -> success.accept(value)))
                .exceptionally(failure -> { Platform.runLater(() -> error.accept(ClientJson.cause(failure))); return null; });
    }
}
