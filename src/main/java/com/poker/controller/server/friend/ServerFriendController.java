package com.poker.controller.server.friend;

import com.poker.dao.JdbcDatabase;
import com.poker.dto.FriendDto;
import com.poker.model.social.FriendOperations;

import java.sql.SQLException;
import java.util.List;

public final class ServerFriendController {
    private final FriendOperations friends;

    public ServerFriendController(JdbcDatabase database) { friends = new FriendOperations(database); }
    public List<FriendDto> friends(long userId) throws SQLException { return friends.friends(userId); }
    public List<FriendDto> incoming(long userId) throws SQLException { return friends.incoming(userId); }
    public long request(long requester, long recipient) throws SQLException {
        return friends.request(requester, recipient);
    }
    public boolean respond(long requestId, long recipient, boolean accept) throws SQLException {
        return friends.respond(requestId, recipient, accept);
    }
    public boolean remove(long userId, long friendId) throws SQLException {
        return friends.remove(userId, friendId);
    }
}
