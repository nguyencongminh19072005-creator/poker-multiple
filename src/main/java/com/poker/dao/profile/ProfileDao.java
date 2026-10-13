package com.poker.dao.profile;

import com.poker.dao.JdbcDatabase;
import com.poker.dto.ProfileDto;
import java.sql.*;
import java.util.Optional;

public final class ProfileDao {
    private final JdbcDatabase database;
    public ProfileDao(JdbcDatabase database){this.database=database;}
    public Optional<ProfileDto> find(long userId)throws SQLException{
        String sql="SELECT p.user_id,p.display_name,p.avatar_url,p.online_status,u.account_chips FROM player_profiles p JOIN users u ON u.id=p.user_id WHERE p.user_id=?";
        try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,userId);try(ResultSet rs=ps.executeQuery()){return rs.next()?Optional.of(new ProfileDto(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getLong(5))):Optional.empty();}}
    }
    public void update(long userId,String displayName,String avatarUrl)throws SQLException{
        try(Connection c=database.open();PreparedStatement ps=c.prepareStatement("UPDATE player_profiles SET display_name=?,avatar_url=? WHERE user_id=?")){ps.setString(1,displayName);ps.setString(2,avatarUrl);ps.setLong(3,userId);ps.executeUpdate();}
    }
    public void setPresence(long userId,String status)throws SQLException{
        try(Connection c=database.open();PreparedStatement ps=c.prepareStatement("UPDATE player_profiles SET online_status=? WHERE user_id=?")){ps.setString(1,status);ps.setLong(2,userId);ps.executeUpdate();}
    }
    public void markAllOffline()throws SQLException{
        try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(
                "UPDATE player_profiles SET online_status='OFFLINE' WHERE online_status<>'OFFLINE'")){
            ps.executeUpdate();
        }
    }
    public long countOnline()throws SQLException{
        try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(
                "SELECT COUNT(*) FROM player_profiles WHERE online_status<>'OFFLINE'");
                ResultSet rs=ps.executeQuery()){
            rs.next();return rs.getLong(1);
        }
    }
}
