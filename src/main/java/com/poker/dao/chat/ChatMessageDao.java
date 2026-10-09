package com.poker.dao.chat;

import com.poker.dao.JdbcDatabase;
import com.poker.dto.ChatMessageDto;
import java.sql.*;import java.util.*;

public final class ChatMessageDao {
    private final JdbcDatabase database; public ChatMessageDao(JdbcDatabase database){this.database=database;}
    public ChatMessageDto save(long roomId,long senderId,String clientMessageId,String content)throws SQLException{
        String insert="INSERT INTO chat_messages(room_id,sender_user_id,client_message_id,content) VALUES(?,?,?,?)";
        try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(insert,Statement.RETURN_GENERATED_KEYS)){ps.setLong(1,roomId);ps.setLong(2,senderId);ps.setString(3,clientMessageId);ps.setString(4,content.trim());ps.executeUpdate();try(ResultSet keys=ps.getGeneratedKeys()){if(!keys.next())throw new SQLException("Không lấy được message id");return find(c,keys.getLong(1));}}
    }
    public List<ChatMessageDto> history(long roomId,long beforeId,int limit)throws SQLException{
        String sql="SELECT m.id,m.room_id,m.sender_user_id,p.display_name,m.content,m.created_at FROM chat_messages m JOIN player_profiles p ON p.user_id=m.sender_user_id WHERE m.room_id=? AND m.id<? ORDER BY m.id DESC LIMIT ?";
        try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,roomId);ps.setLong(2,beforeId<=0?Long.MAX_VALUE:beforeId);ps.setInt(3,Math.min(Math.max(limit,1),100));try(ResultSet rs=ps.executeQuery()){List<ChatMessageDto> out=new ArrayList<>();while(rs.next())out.add(map(rs));Collections.reverse(out);return out;}}
    }
    private ChatMessageDto find(Connection c,long id)throws SQLException{try(PreparedStatement ps=c.prepareStatement("SELECT m.id,m.room_id,m.sender_user_id,p.display_name,m.content,m.created_at FROM chat_messages m JOIN player_profiles p ON p.user_id=m.sender_user_id WHERE m.id=?")){ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new SQLException("Không tìm thấy tin nhắn");return map(rs);}}}
    private static ChatMessageDto map(ResultSet rs)throws SQLException{return new ChatMessageDto(rs.getLong(1),"ROOM",rs.getLong(2),rs.getLong(3),null,rs.getString(4),rs.getString(5),rs.getTimestamp(6).toInstant());}
}
