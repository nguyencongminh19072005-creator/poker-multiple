# Poker Java MVC

## Chạy dự án trên Windows

Dự án gồm **server Java thuần** và **client JavaFX**. Server kết nối MySQL và mở WebSocket; mỗi client chỉ kết nối tới WebSocket của server, không kết nối MySQL trực tiếp. Cần JDK **26**, Docker Desktop (nếu dùng MySQL bằng Docker), và kết nối Internet trong lần Maven tải thư viện đầu tiên.

1. Kiểm tra JDK: chạy `java -version` và `./mvnw.cmd -version` trong thư mục `poker-mvc`. Cả hai cần dùng JDK 26. Nếu VS Code vẫn chọn JDK cũ, cấu hình **Java: Configure Java Runtime** và tải lại Java Projects.
2. Tại thư mục `poker-mvc`, sao chép [`.env.example`](.env.example) thành `.env`, rồi tự đặt `MYSQL_ROOT_PASSWORD` và `DB_PASSWORD` **giống nhau**. Đặt `JWT_SECRET` ngẫu nhiên dài ít nhất 32 byte để token còn hợp lệ sau khi khởi động lại server. `.env` là file riêng của từng máy, không được commit. Bản ở `D:\LTM` hiện cũng có `.env` riêng cho Compose ở thư mục cha và cấu hình Run của VS Code.
3. Chạy `docker compose up -d mysql-db` trong `poker-mvc`. [Cấu hình Docker](docker-compose.yaml) dùng MySQL 8, cổng máy host **3307**, database **poker_java** và mount [`database/init.sql`](database/init.sql). Redis có trong Compose nhưng không bắt buộc đối với server Java thuần. Nếu đang chạy Compose cũ ở `D:\LTM`, **chỉ dùng một trong hai cấu hình** để tránh trùng cổng/container; cấu hình ở thư mục cha đọc `D:\LTM\.env`, còn cấu hình trong `poker-mvc` đọc `poker-mvc\.env`. Dữ liệu MySQL cũ không tự chuyển sang container mới.
4. Trên **máy chạy server**, chương trình đọc `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT` và `JWT_SECRET` từ biến môi trường. URL mặc định trong `.env.example` là `jdbc:mysql://localhost:3307/poker_java?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh`. Với bản ở `D:\LTM`, chọn **Poker Server** trong Run and Debug: cấu hình `D:\LTM\.vscode\launch.json` đọc các biến từ `D:\LTM\.env`. Nếu clone repo riêng, cấu hình `envFile` tương ứng trong VS Code hoặc nạp `.env` vào terminal trước khi chạy `com.poker.ServerMain`. Ví dụ PowerShell khi muốn đặt thủ công:

   ```powershell
   $env:DB_URL = 'jdbc:mysql://localhost:3307/poker_java?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh'
   $env:DB_USERNAME = 'root'
   $env:DB_PASSWORD = '<mật khẩu bạn đặt trong .env>'
   $env:SERVER_PORT = '8080'
   $env:JWT_SECRET = '<chuỗi ngẫu nhiên ít nhất 32 byte>'
   ```

5. Chạy class `com.poker.ServerMain` với các biến môi trường ở bước 4. Chờ dòng `[SERVER] MySQL connected` và `WebSocket running`. Cổng 8080 phải còn trống; không chạy hai server cùng lúc trên một database.
6. Chạy **Poker Client (JavaFX)** trong VS Code hoặc `com.poker.ClientMain`. Khi client chạy cùng máy server, URL mặc định là `ws://localhost:8080/ws`. Máy khác chỉ cần JDK/app client và đặt `POKER_WS_URL=ws://<IP_MAY_SERVER>:8080/ws`; **không** cần MySQL hay chạy `ServerMain` trên máy client. Mở TCP 8080 trong firewall máy server, và bảo đảm hai máy truy cập được nhau. Ví dụ khi server còn dùng địa chỉ LAN `192.168.10.135`: `ws://192.168.10.135:8080/ws`; địa chỉ này có thể đổi khi mạng đổi. Với người chơi ở mạng Internet khác, chỉ IP LAN là chưa đủ; cần VPN hoặc hạ tầng mạng phù hợp.

Lệnh kiểm tra nhanh: `docker compose ps` tại thư mục chứa Compose đang dùng để xem MySQL, `Test-NetConnection localhost -Port 3307` để kiểm tra cổng database, và `.\mvnw.cmd test` tại `poker-mvc` để chạy kiểm thử. Nếu báo **Access denied**, kiểm tra `DB_USERNAME`/`DB_PASSWORD` có khớp tài khoản MySQL hiện tại. Nếu báo **Communications link failure**, kiểm tra Docker và cổng 3307. Docker chỉ chạy `init.sql` khi tạo database mới lần đầu; đổi mật khẩu trong `.env` **không** đổi mật khẩu của database đã tồn tại.

### Git và mật khẩu

`.gitignore` bỏ qua output Maven, file `.class`, metadata IDE, log, backup và `.env`; **không** bỏ qua source, assets đang dùng, `pom.xml`, Maven Wrapper, Compose hoặc `database/init.sql`. Repo `poker-multiple` có `docker-compose.yaml` và `.env.example` nhưng **không có `.env` thật**. File Compose cũ ở `D:\LTM` cũng đã đọc mật khẩu từ `D:\LTM\.env`. Nếu từng đẩy bản Compose cũ có mật khẩu lên một repo khác, mật khẩu đó vẫn có thể còn trong lịch sử Git; `.gitignore` hoặc commit sửa file không xóa được lịch sử. Khi đó hãy đổi mật khẩu ở MySQL và các nơi dùng lại mật khẩu ấy. Không đăng nội dung `docker compose config` lên mạng vì lệnh này có thể in ra biến đã được thay giá trị.

Để tránh thêm nhầm file trước khi push, kiểm tra `git status --short` và `git diff --cached --name-only`. Không chạy `git add .` ở thư mục cha `D:\LTM` nếu mục tiêu chỉ là repo `poker-multiple`.

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
