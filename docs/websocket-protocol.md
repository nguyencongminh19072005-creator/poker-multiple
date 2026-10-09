# Giao thức WebSocket của poker-mvc

Server chỉ mở `ws://<host>:8080/ws` theo mặc định (đổi cổng bằng `SERVER_PORT`). Client đặt `POKER_WS_URL` thành `ws://localhost:8081/ws` nếu chạy chung máy với server đã đặt `SERVER_PORT=8081`, hoặc `ws://<IP_SERVER>:8081/ws` nếu chạy máy khác. Không cấu hình IP của client. Mỗi app JavaFX giữ một WebSocket connection riêng; server lưu session/subscription theo connection. Cả yêu cầu/phản hồi và cập nhật realtime đều dùng cùng kết nối đó; không có REST hoặc STOMP endpoint.

Lệnh từ client:

```json
{"type":"request","id":"uuid","method":"GET","path":"/api/v1/rooms","body":{},"token":"access-token"}
```

`id` là mã duy nhất để ghép phản hồi với yêu cầu. `token` không cần cho đăng ký/đăng nhập. `method` và `path` giữ tên tuyến cũ để giới hạn phạm vi migration; chúng là trường JSON, không phải yêu cầu HTTP.

Phản hồi từ server:

```json
{"type":"response","id":"uuid","status":200,"body":{"items":[]}}
```

Lỗi vẫn có `status` tương ứng (ví dụ `401`, `404`) và `body.message`. Phản hồi thành công không có nội dung dùng `body: {}`.

Theo dõi sự kiện:

```json
{"type":"subscribe","destination":"/topic/lobby"}
```

Server đẩy thông báo thay đổi, sau đó client tải snapshot mới qua một lệnh WebSocket:

```json
{"type":"event","destination":"/topic/lobby","body":{}}
```

Các kênh hiện dùng: `/topic/lobby`, `/topic/room/{id}`, `/topic/game/{id}`, `/user/queue/notifications`. Sự kiện chỉ báo có thay đổi, không mang bài riêng hoặc dữ liệu tài khoản. Client lấy snapshot riêng sau thông báo. Trong lúc chơi, lá bài tẩy chỉ có trong phản hồi gửi cho chính người chơi. Khi server đã chia pot và phase là `FINISHED`, `publicState.players[].showdownCards` chứa bài của mọi người đã tham gia ván, kể cả người đã fold, để tất cả client cùng xem; trước đó trường này là danh sách rỗng.

Sau `POST /api/v1/rooms/{roomId}/chat/messages`, server gửi sự kiện `{"type":"event","destination":"/topic/room/{roomId}","body":{"type":"CHAT_CHANGED"}}`. Client trong phòng chỉ tải lại lịch sử chat qua lệnh có kiểm tra quyền thành viên; sự kiện không chứa nội dung của phòng riêng và không bắt client tải lại toàn bộ trạng thái game.

Snapshot sau khi chia pot có thêm `result` gồm `totalPot` (pot tranh chấp của ván vừa xong), `payouts` (tiền thắng theo user ID, không tính tiền cược không được call trả lại), `tiedWinnerUserIds` và `foldOnly`. Trước khi chia pot, `result` là `null`. Các trường và đường dẫn lệnh cũ không đổi; client dùng kết quả từ server để hiện THẮNG/THUA/HÒA, không tự tính từ bài trên màn hình.

Các lệnh game giữ nguyên đường dẫn JSON hiện có: `GET /api/v1/games/active/me`, `GET /api/v1/games/active/room/{roomId}`, `POST /api/v1/games/rooms/{roomId}/start`, `GET /api/v1/games/{gameId}/snapshot`, `POST /api/v1/games/{gameId}/leave` và `POST /app/game/{gameId}/action`. Không có HTTP endpoint tương ứng. Ván và bộ bài đang giữ trong bộ nhớ của một server; nếu server dừng giữa ván, lần khởi động tiếp theo hủy session chưa quyết toán và giữ nguyên số chip trên bàn trong MySQL (cược của ván dở không được áp dụng). Chưa có khôi phục chính xác ván dở, bộ đếm thời gian lượt hay ghi lịch sử hành động vào `poker_hands`/`player_actions`.
