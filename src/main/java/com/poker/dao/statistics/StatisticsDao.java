package com.poker.dao.statistics;

import com.poker.dao.JdbcDatabase;import com.poker.model.analytics.PlayerStatistics;import java.sql.*;import java.time.LocalDate;import java.util.*;

public final class StatisticsDao {
    private final JdbcDatabase database;public StatisticsDao(JdbcDatabase database){this.database=database;}
    public Optional<PlayerStatistics> player(long userId)throws SQLException{String sql="SELECT total_games,total_hands,total_wins,total_losses,win_rate,total_chips_won,total_chips_lost,net_chip,largest_pot_won,average_playing_seconds FROM player_statistics WHERE user_id=?";try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,userId);try(ResultSet rs=ps.executeQuery()){return rs.next()?Optional.of(new PlayerStatistics(userId,rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4),rs.getBigDecimal(5),rs.getLong(6),rs.getLong(7),rs.getLong(8),rs.getLong(9),rs.getLong(10))):Optional.empty();}}}
    public List<Bucket> daily(long userId,LocalDate from,LocalDate to)throws SQLException{return buckets("daily_statistics","stat_date",userId,from,to);}
    public List<Bucket> weekly(long userId,LocalDate from,LocalDate to)throws SQLException{return buckets("weekly_statistics","week_start_date",userId,from,to);}
    private List<Bucket>buckets(String table,String dateColumn,long userId,LocalDate from,LocalDate to)throws SQLException{String sql="SELECT "+dateColumn+",hands_played,hands_won,hands_lost,hands_tied,chips_won,chips_lost,net_chips,largest_pot_won,playing_time_seconds,sessions_participated FROM "+table+" WHERE user_id=? AND "+dateColumn+" BETWEEN ? AND ? ORDER BY "+dateColumn;try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,userId);ps.setDate(2,java.sql.Date.valueOf(from));ps.setDate(3,java.sql.Date.valueOf(to));try(ResultSet rs=ps.executeQuery()){List<Bucket> out=new ArrayList<>();while(rs.next())out.add(new Bucket(rs.getObject(1,LocalDate.class),rs.getLong(2),rs.getLong(3),rs.getLong(4),rs.getLong(5),rs.getLong(6),rs.getLong(7),rs.getLong(8),rs.getLong(9),rs.getLong(10),rs.getLong(11)));return out;}}}
    public record Bucket(LocalDate date,long handsPlayed,long handsWon,long handsLost,long handsTied,long chipsWon,long chipsLost,long netChips,long largestPot,long playingSeconds,long sessions){}
}
