package com.poker.dao.history;

import com.poker.dao.JdbcDatabase;import com.poker.dto.MatchHistoryDto;import java.sql.*;import java.util.*;

public final class MatchHistoryDao {
    private final JdbcDatabase database;public MatchHistoryDao(JdbcDatabase database){this.database=database;}
    public List<MatchHistoryDto> recent(long userId,int limit)throws SQLException{
        String sql="SELECT gs.id,r.name,rh.placement,rh.session_net,COUNT(DISTINCT ph.id),gs.ended_at FROM ranking_history rh JOIN game_sessions gs ON gs.id=rh.game_session_id JOIN rooms r ON r.id=gs.room_id LEFT JOIN poker_hands ph ON ph.game_session_id=gs.id WHERE rh.user_id=? GROUP BY gs.id,r.name,rh.placement,rh.session_net,gs.ended_at ORDER BY gs.ended_at DESC LIMIT ?";
        try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,userId);ps.setInt(2,Math.min(Math.max(limit,1),100));try(ResultSet rs=ps.executeQuery()){List<MatchHistoryDto> out=new ArrayList<>();while(rs.next())out.add(new MatchHistoryDto(rs.getLong(1),rs.getString(2),rs.getInt(3),rs.getLong(4),rs.getInt(5),rs.getTimestamp(6).toInstant()));return out;}}
    }
}
