package com.poker.dao.ranking;

import com.poker.dao.JdbcDatabase;import com.poker.dto.RankingEntryDto;import java.sql.*;import java.time.Instant;import java.util.*;

public final class RankingDao {
    private final JdbcDatabase database;public RankingDao(JdbcDatabase database){this.database=database;}
    public Optional<RankingEntryDto> mine(long userId)throws SQLException{
        String sql="SELECT rank_no,user_id,username,rating,games_rated,avatar_url,chips_earned FROM ("+
                "SELECT u.id user_id,COALESCE(p.display_name,u.username) username,p.avatar_url,"+
                "COALESCE(pr.rating,1000) rating,COALESCE(pr.games_rated,0) games_rated,"+
                "COALESCE(s.total_chips_won,0) chips_earned,"+
                "ROW_NUMBER() OVER(ORDER BY COALESCE(s.total_chips_won,0) DESC,"+
                "COALESCE(pr.games_rated,0) DESC,u.id) rank_no "+
                "FROM users u LEFT JOIN player_rankings pr ON pr.user_id=u.id "+
                "LEFT JOIN player_profiles p ON p.user_id=u.id "+
                "LEFT JOIN player_statistics s ON s.user_id=u.id) ranked WHERE user_id=?";
        try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setLong(1,userId);
            try(ResultSet rs=ps.executeQuery()){
                return rs.next()?Optional.of(new RankingEntryDto(rs.getLong(1),rs.getLong(2),
                        rs.getString(3),rs.getLong(4),rs.getLong(5),rs.getString(6),
                        rs.getLong(7))):Optional.empty();
            }
        }
    }
    public List<RankingEntryDto> leaderboard(int limit)throws SQLException{
        String sql="SELECT u.id,COALESCE(p.display_name,u.username) username,COALESCE(pr.rating,1000) rating,"+
                "COALESCE(pr.games_rated,0) games_rated,p.avatar_url,"+
                "COALESCE(s.total_chips_won,0) chips_earned,"+
                "ROW_NUMBER() OVER(ORDER BY COALESCE(s.total_chips_won,0) DESC,"+
                "COALESCE(pr.games_rated,0) DESC,u.id) rank_no "+
                "FROM users u LEFT JOIN player_rankings pr ON pr.user_id=u.id "+
                "LEFT JOIN player_profiles p ON p.user_id=u.id "+
                "LEFT JOIN player_statistics s ON s.user_id=u.id "+
                "ORDER BY chips_earned DESC,games_rated DESC,u.id LIMIT ?";
        try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,Math.max(limit,1));
            try(ResultSet rs=ps.executeQuery()){
                List<RankingEntryDto> out=new ArrayList<>();
                while(rs.next()) out.add(new RankingEntryDto(rs.getLong(7),rs.getLong(1),rs.getString(2),
                        rs.getLong(3),rs.getLong(4),rs.getString(5),rs.getLong(6)));
                return out;
            }
        }
    }
    public List<History> history(long userId,int limit)throws SQLException{String sql="SELECT game_session_id,old_rating,new_rating,rating_delta,session_net,placement,participant_count,created_at FROM ranking_history WHERE user_id=? ORDER BY created_at DESC LIMIT ?";try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,userId);ps.setInt(2,Math.min(Math.max(limit,1),100));try(ResultSet rs=ps.executeQuery()){List<History> out=new ArrayList<>();while(rs.next())out.add(new History(rs.getLong(1),rs.getInt(2),rs.getInt(3),rs.getInt(4),rs.getLong(5),rs.getInt(6),rs.getInt(7),rs.getTimestamp(8).toInstant()));return out;}}}
    public record History(long gameSessionId,int oldRating,int newRating,int delta,long sessionNet,int placement,int participants,Instant createdAt){}
}
