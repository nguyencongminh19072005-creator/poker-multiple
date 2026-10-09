package com.poker.dao.ranking;

import com.poker.dao.JdbcDatabase;import com.poker.dto.RankingEntryDto;import java.sql.*;import java.time.Instant;import java.util.*;

public final class RankingDao {
    private final JdbcDatabase database;public RankingDao(JdbcDatabase database){this.database=database;}
    public Optional<RankingEntryDto> mine(long userId)throws SQLException{
        String sql="SELECT rank_no,user_id,username,rating,games_rated FROM ("+
                "SELECT pr.user_id,u.username,pr.rating,pr.games_rated,"+
                "DENSE_RANK() OVER(ORDER BY pr.rating DESC,pr.games_rated DESC) rank_no "+
                "FROM player_rankings pr JOIN users u ON u.id=pr.user_id) ranked WHERE user_id=?";
        try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setLong(1,userId);
            try(ResultSet rs=ps.executeQuery()){
                return rs.next()?Optional.of(new RankingEntryDto(rs.getLong(1),rs.getLong(2),
                        rs.getString(3),rs.getLong(4),rs.getLong(5))):Optional.empty();
            }
        }
    }
    public List<RankingEntryDto> leaderboard(int limit)throws SQLException{String sql="SELECT pr.user_id,u.username,pr.rating,pr.games_rated,DENSE_RANK() OVER(ORDER BY pr.rating DESC,pr.games_rated DESC) rank_no FROM player_rankings pr JOIN users u ON u.id=pr.user_id ORDER BY pr.rating DESC,pr.games_rated DESC,pr.user_id LIMIT ?";try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,Math.min(Math.max(limit,1),100));try(ResultSet rs=ps.executeQuery()){List<RankingEntryDto> out=new ArrayList<>();while(rs.next())out.add(new RankingEntryDto(rs.getLong(5),rs.getLong(1),rs.getString(2),rs.getLong(3),rs.getLong(4)));return out;}}}
    public List<History> history(long userId,int limit)throws SQLException{String sql="SELECT game_session_id,old_rating,new_rating,rating_delta,session_net,placement,participant_count,created_at FROM ranking_history WHERE user_id=? ORDER BY created_at DESC LIMIT ?";try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,userId);ps.setInt(2,Math.min(Math.max(limit,1),100));try(ResultSet rs=ps.executeQuery()){List<History> out=new ArrayList<>();while(rs.next())out.add(new History(rs.getLong(1),rs.getInt(2),rs.getInt(3),rs.getInt(4),rs.getLong(5),rs.getInt(6),rs.getInt(7),rs.getTimestamp(8).toInstant()));return out;}}}
    public record History(long gameSessionId,int oldRating,int newRating,int delta,long sessionNet,int placement,int participants,Instant createdAt){}
}
