package com.poker.dao.admin;

import com.poker.dao.JdbcDatabase;import com.poker.model.User;import com.poker.model.auth.AccountStatus;import com.poker.model.auth.Role;import java.sql.*;import java.util.*;

public final class AdminDao {
    private final JdbcDatabase database;public AdminDao(JdbcDatabase database){this.database=database;}
    public Overview overview()throws SQLException{String sql="SELECT (SELECT COUNT(*) FROM users),(SELECT COUNT(*) FROM player_profiles WHERE online_status<>'OFFLINE'),(SELECT COUNT(*) FROM rooms WHERE status IN('WAITING','PLAYING')),(SELECT COALESCE(SUM(account_chips),0) FROM users)";try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(sql);ResultSet rs=ps.executeQuery()){rs.next();return new Overview(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4));}}
    public List<User> users(int limit)throws SQLException{String sql="SELECT u.id,u.username,p.display_name,u.email,u.role,u.account_status,u.account_chips,p.avatar_url FROM users u JOIN player_profiles p ON p.user_id=u.id ORDER BY u.id LIMIT ?";try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,Math.min(Math.max(limit,1),500));try(ResultSet rs=ps.executeQuery()){List<User> out=new ArrayList<>();while(rs.next())out.add(new User(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),Role.valueOf(rs.getString(5)),AccountStatus.valueOf(rs.getString(6)),rs.getLong(7),rs.getString(8)));return out;}}}
    public boolean setLocked(long userId,boolean locked)throws SQLException{try(Connection c=database.open();PreparedStatement ps=c.prepareStatement("UPDATE users SET account_status=? WHERE id=? AND role<>'ADMIN'")){ps.setString(1,locked?"LOCKED":"ACTIVE");ps.setLong(2,userId);return ps.executeUpdate()==1;}}
    public void audit(long adminId,String action,String targetType,Long targetId,String reason)throws SQLException{try(Connection c=database.open();PreparedStatement ps=c.prepareStatement("INSERT INTO admin_audit_log(admin_user_id,action_type,target_type,target_id,reason) VALUES(?,?,?,?,?)")){ps.setLong(1,adminId);ps.setString(2,action);ps.setString(3,targetType);if(targetId==null)ps.setNull(4,Types.BIGINT);else ps.setLong(4,targetId);ps.setString(5,reason);ps.executeUpdate();}}
    public record Overview(long totalUsers,long activeUsers,long activeRooms,long totalAccountChips){}
}
