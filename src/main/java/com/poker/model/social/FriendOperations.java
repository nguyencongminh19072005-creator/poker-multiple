package com.poker.model.social;

import com.poker.dao.JdbcDatabase;
import com.poker.dao.friend.FriendDao;
import com.poker.dto.FriendDto;

import java.sql.SQLException;
import java.util.List;

/** Friendship rules used by the WebSocket adapter. */
public final class FriendOperations {
    private final FriendDao friends;

    public FriendOperations(JdbcDatabase database) {
        friends = new FriendDao(database);
    }

    public long request(long requester, long recipient) throws SQLException {
        if (requester == recipient) {
            throw new IllegalArgumentException("Không thể tự kết bạn với chính mình");
        }
        FriendshipPair.of(requester, recipient);
        return friends.request(requester, recipient);
    }

    public List<FriendDto> friends(long userId) throws SQLException { return friends.friends(userId); }
    public List<FriendDto> incoming(long userId) throws SQLException { return friends.incoming(userId); }
    public boolean respond(long requestId, long recipient, boolean accept) throws SQLException {
        return friends.respond(requestId, recipient, accept);
    }
    public boolean remove(long userId, long friendId) throws SQLException {
        return friends.remove(userId, friendId);
    }
}
