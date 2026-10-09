package com.poker.dao.friend;

import com.poker.dao.JdbcDatabase;import com.poker.dto.FriendDto;import java.sql.*;import java.util.*;

public final class FriendDao {
    private final JdbcDatabase database;public FriendDao(JdbcDatabase database){this.database=database;}
    public List<FriendDto> friends(long userId)throws SQLException{return query(userId,"ACCEPTED",false);}
    public List<FriendDto> incoming(long userId)throws SQLException{return query(userId,"PENDING",true);}
    private List<FriendDto> query(long userId,String status,boolean incoming)throws SQLException{
        String where=incoming?"f.recipient_user_id=?":"(f.requester_user_id=? OR f.recipient_user_id=?)";
        String sql="SELECT f.id,u.id,p.display_name,p.avatar_url,f.status,p.online_status FROM friendships f JOIN users u ON u.id=IF(f.requester_user_id=?,f.recipient_user_id,f.requester_user_id) JOIN player_profiles p ON p.user_id=u.id WHERE "+where+" AND f.status=? ORDER BY f.created_at DESC";
        try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(sql)){int i=1;ps.setLong(i++,userId);ps.setLong(i++,userId);if(!incoming)ps.setLong(i++,userId);ps.setString(i,status);try(ResultSet rs=ps.executeQuery()){List<FriendDto> out=new ArrayList<>();while(rs.next())out.add(new FriendDto(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6)));return out;}}
    }
    public long request(long requester,long recipient)throws SQLException{try(Connection c=database.open();PreparedStatement ps=c.prepareStatement("INSERT INTO friendships(requester_user_id,recipient_user_id,status) VALUES(?,?,'PENDING')",Statement.RETURN_GENERATED_KEYS)){ps.setLong(1,requester);ps.setLong(2,recipient);ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){if(!rs.next())throw new SQLException("Không lấy được request id");return rs.getLong(1);}}}
    public boolean respond(long requestId,long recipient,boolean accept)throws SQLException{try(Connection c=database.open();PreparedStatement ps=c.prepareStatement("UPDATE friendships SET status=?,responded_at=CURRENT_TIMESTAMP(6),version=version+1 WHERE id=? AND recipient_user_id=? AND status='PENDING'")){ps.setString(1,accept?"ACCEPTED":"REJECTED");ps.setLong(2,requestId);ps.setLong(3,recipient);return ps.executeUpdate()==1;}}
    public boolean remove(long userId,long friendId)throws SQLException{try(Connection c=database.open();PreparedStatement ps=c.prepareStatement("DELETE FROM friendships WHERE status='ACCEPTED' AND ((requester_user_id=? AND recipient_user_id=?) OR (requester_user_id=? AND recipient_user_id=?))")){ps.setLong(1,userId);ps.setLong(2,friendId);ps.setLong(3,friendId);ps.setLong(4,userId);return ps.executeUpdate()==1;}}
}
