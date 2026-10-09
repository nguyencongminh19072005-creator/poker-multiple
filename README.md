# Poker Java MVC

Đây là bản mã riêng trong `D:\LTM\poker-mvc`; dự án cũ ở `D:\LTM\src\main` không bị ghi đè.

## Cấu trúc đơn giản

```text
poker-mvc/
├─ database/init.sql                 # Toàn bộ schema MySQL trong một file
├─ src/main/java/com/poker/
│  ├─ ClientMain.java                # Chạy JavaFX client
│  ├─ ServerMain.java                # Chạy WebSocket server Java thuần
│  ├─ controller/                    # Chia theo admin/auth/chat/friend/game/lobby/room/...
│  ├─ model/                         # Dữ liệu và luật nghiệp vụ
│  │  ├─ game/                      # Betting, deck, hand, pot, round, state
│  │  ├─ auth/, room/, social/      # Model các phần khác
│  │  └─ analytics/, ranking/       # Thống kê, xếp hạng
│  ├─ view/                          # Toàn bộ giao diện JavaFX hiện tại
│  ├─ dto/                           # Kết quả truy vấn tổng hợp và dữ liệu truyền
│  ├─ dao/                           # DAO JDBC, ngang cấp model
│  ├─ network/client/                # Một kết nối WebSocket cho client
│  ├─ network/server/websocket/      # Server và các handler lệnh WebSocket
│  └─ util/                          # Tiện ích dùng chung
├─ src/main/resources/assets/        # 194 asset giao diện
└─ src/test/java/com/poker/          # Kiểm thử luật game và giao tiếp mạng
```

Không còn cây package `model/core/.../domain/...` hay Flyway. File `database/init.sql` được gộp theo đúng thứ tự V1–V12 và dành cho database `poker_java`. Chỉ import SQL vào một database mới, chưa có dữ liệu cần giữ; chạy lại trên database đã có bảng sẽ báo trùng bảng.

Chạy `run-preview.cmd` để xem giao diện/hoạt ảnh độc lập. Kiểm tra mã bằng `mvnw.cmd test` với JDK 26.

**Quy tắc package:** DAO nằm ngoài `model`; View/Controller không mở kết nối MySQL. `UserDto` đã chuyển thành Model `User`, `Player` dùng riêng cho ghế đã đổi thành `SeatViewModel`, và DAO thống kê trả về Model `PlayerStatistics`. DTO tổng hợp vẫn giữ. WebSocket handler chuyển lệnh đến controller phía server, controller gọi Model, Model quyết định luật và DAO thực thi SQL. Các quy tắc tham gia/rời phòng, buy-in, hoàn chip, chuyển chủ phòng hiện nằm trong `RoomOperations`; `RoomDao` giữ các truy vấn/cập nhật trong transaction.

**Giao thức hiện tại:** `ServerMain` chỉ mở WebSocket tại `ws://localhost:8080/ws` theo mặc định; không có HTTP server, REST endpoint hay STOMP. Mỗi lần chạy `ClientMain` tạo đúng một `PokerSocketClient` và giữ một kết nối WebSocket riêng cho lệnh/phản hồi lẫn thông báo realtime. Server quản lý session và subscription theo từng socket. Client chỉ cần biết địa chỉ **máy server**, không cần cấu hình IP của chính client. Frame lệnh gồm `type=request`, `id`, `method`, `path`, `body`, `token`; phản hồi gồm `type=response`, `id`, `status`, `body`. Các đường dẫn lệnh cũ được giữ làm tên lệnh trong JSON, không phải HTTP URL.

Ví dụ muốn dùng cổng 8081: trên máy server đặt `SERVER_PORT=8081` rồi chạy `ServerMain`; trên từng máy client đặt `POKER_WS_URL=ws://<IP_SERVER>:8081/ws` rồi chạy `ClientMain`. Khi chạy chung máy, dùng `POKER_WS_URL=ws://localhost:8081/ws`. Cho phép TCP 8081 qua firewall của máy server. Hai biến này phải khớp cổng; không đổi một bên mà giữ bên kia ở 8080.

**Bảo mật:** Đặt `JWT_SECRET` dài ít nhất 32 byte trên máy server; không ghi giá trị thật vào Git. Nếu không đặt, server tự sinh khóa ngẫu nhiên mỗi lần chạy để tránh dùng chung khóa mặc định công khai; access token cũ sẽ hết hiệu lực khi server khởi động lại. Khi kết nối từ Internet, dùng VPN hoặc WebSocket bảo mật (`wss://`) thay vì phơi cổng `ws://` không mã hóa.

Luồng đang dùng: `JavaFX View → client Controller → PokerSocketClient → WebSocket → PokerWebSocketServer → CommandHandler → server Controller → Model operation → DAO → MySQL`. `ConnectionManager` giữ subscription và phiên xác thực riêng cho mỗi socket. `java.net.http.HttpClient` chỉ được dùng để tạo WebSocket theo API JDK, không gửi REST request.

**Giới hạn còn lại:** runtime một ván hiện chỉ sống trong bộ nhớ server. Khởi động lại giữa ván sẽ hủy ván dở, không khôi phục bộ bài/lượt; chip trên bàn trong MySQL giữ nguyên như trước ván dở. Server đã có timer lượt 15 giây với Check/Fold tự động, nhưng chưa ghi đầy đủ lịch sử hand/action, cập nhật xếp hạng sau ván và kiểm thử tích hợp với MySQL thật trong bộ test tự động. Không nên coi đây là bản multiplayer hoàn chỉnh. Bản sao lưu trước khi dọn: `D:\LTM\backups\poker-mvc-pre-structure-clean-2026-10-05.zip`.

## Online/offline và phòng chờ

Client gửi heartbeat tới server mỗi 5 giây. Đóng cửa sổ bình thường sẽ gửi logout ngay; nếu ứng dụng bị tắt đột ngột hoặc mất mạng, server đánh dấu offline và dọn membership phòng chờ sau khoảng 20–25 giây. Chip còn trên bàn chờ được trả về tài khoản; phòng không còn thành viên được chuyển sang `CLOSED` và biến mất khỏi Lobby (giữ dòng trong database để không phá lịch sử). Khi khởi động lại server, các phiên online của tiến trình cũ và phòng chờ bỏ dở cũng được dọn.

Để áp dụng mã mới, cần dừng server và client cũ rồi chạy lại `ServerMain` trước, sau đó mới chạy `ClientMain`. Không chạy hai server cùng một database. Khi client mất kết nối giữa ván, server đánh dấu ghế `DISCONNECTED` và tự xử lý lượt có thể Check/Fold. Nếu cả server khởi động lại, ván dở bị hủy thay vì quyết toán pot chưa hoàn tất.
