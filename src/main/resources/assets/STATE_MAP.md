# Asset và trạng thái bàn Poker

Đường dẫn dưới đây tính từ `src/main/resources/assets/`. Ảnh được nạp qua
`AssetLoader` từ classpath; chữ, số tiền và trạng thái động do JavaFX vẽ, không
nhúng vào PNG. Không xóa tài nguyên chỉ vì tên thư mục có chữ `reference`:
`reference-v2` là bộ asset chính của màn bàn hiện tại.

| Trạng thái / thành phần | Tài nguyên hiện dùng | Nơi hiển thị |
| --- | --- | --- |
| WIN | Nhãn vàng và hiệu ứng phóng của `PokerTableContainer.showHandResult` / `PlayerNode.showWin`; không có PNG riêng | Controller gọi khi snapshot có kết quả chia pot từ server |
| LOSE | Overlay chữ `THUA` và dòng kết quả; không có PNG riêng | Controller dùng kết quả chia pot từ server |
| DRAW / TIE | Overlay `HÒA / CHIA POT` nếu người xem thắng trong pot được chia | Dựa trên danh sách người nhận chia pot do server gửi |
| ALL-IN | Chữ/nút JavaFX trong `ControlPanelView`, badge ghế trong `PlayerNode` | Đang dùng; `actions/btn-allin-flame.png` hiện không được gọi bởi view |
| FOLD, CHECK, CALL, RAISE, BET | Nút JavaFX `ControlPanelView`; action badge ở `PlayerNode`; giá CALL là dữ liệu động | Đang dùng; không dùng ảnh nút cũ để tránh chữ tĩnh |
| YOUR TURN | Chữ, timer và viền sáng JavaFX ở `ControlPanelView`/`PlayerNode`; `reference-v2/pack/icons/timer.png` | Đang dùng |
| WAITING, READY | Nhãn/badge JavaFX ở `RoomScreens`, `TableAreaNode`, `PlayerNode` | Đang dùng; `player/ready-status.png` và `unready-status.png` chưa được view gọi |
| DEALER, SMALL BLIND, BIG BLIND | `reference-v2/pack/markers/{dealer,small-blind,big-blind}.png` | Dấu dealer đang dùng trên bàn. Nhãn D/SB/BB hiện được vẽ ở ghế; ảnh marker SB/BB có sẵn nhưng chưa được view gọi trực tiếp |
| Chip, pot | `reference-v2/chips/chip-{1,5,25,100,500,1000}.png` | Cược gần ghế, pot giữa bàn, số dư ở lobby/profile |
| Card / back-card | `reference-v2/cards/card fronts/` (52 lá), `card backs/card back {red,blue}.png` | Bài chung, bài của mình, bài úp của đối thủ |
| Avatar, ghế | `reference-v2/avatars/avatar_player_1..6.png`, `reference-v2/pack/ornate/player-seat.png` | Ghế người chơi |
| Bàn, nền, dealer nữ | `reference-v2/table/poker-table-v2.png`, `reference-v2/backgrounds/casino.png`, `reference-v2/dealer/female-dealer.png` | Bàn và hậu cảnh |
| Khung chat | `reference-v2/pack/ornate/chat-panel.png` | Panel chat |

## Ảnh trùng và ảnh chưa dùng: chỉ kiểm kê, không xóa

- `actions/btn-{allin,call,check,fold,raise}.png` và `button/btn-{allin,call,check,fold,raise}.png` trùng nội dung từng cặp (SHA-256). Các nút game hiện vẽ bằng JavaFX nên các ảnh này là ứng viên dọn sau khi xác nhận các màn khác không dùng.
- `avatars/avatar_player_{1..5}.png` lần lượt trùng nội dung với `player/player-avatar.png`, `player-avatar-shark.png`, `player-avatar-cowboy.png`, `player-avatar-detective.png`, `player-avatar-girl.png`. Màn bàn dùng avatar trong `reference-v2/avatars/`; không tự động xóa bản cũ.
- `reference-v2/screens/01_lobby.png` đến `10_game_result_end_round.png` và `reference-v2/ASSET_GALLERY.html` là ảnh/tài liệu đối chiếu, không phải layer UI. Giữ để đối chiếu với thiết kế gốc.
- `reference-v2/table/poker-table.png` là bàn cũ; bàn đang dùng là `poker-table-v2.png`. Chưa xóa bàn cũ.
- `pages/`, `poker-components/`, nhiều ảnh `actions/`, `button/` và các frame v2 chưa được JavaFX hiện tại nạp trực tiếp. Chúng có thể phục vụ màn khác, preview hoặc thiết kế sau; cần kiểm tra toàn bộ getter/cấu hình trước khi xóa.

## Kết quả ván

Snapshot game có thêm `result` sau khi server chia pot: tổng pot tranh chấp,
tiền thắng của từng người và những người cùng chia một pot. Đây là dữ liệu
authoritative; giao diện không tự tính winner từ ảnh bài. Khi ván đã `FINISHED`,
server gửi bài của mọi người tham gia ván (kể cả người đã fold) trong
`showdownCards`; mỗi client lật hai lá của các ghế khác rồi mới hiện bảng kết
quả. Trong khi ván còn diễn ra, bài đối thủ không được gửi. Các trường snapshot
cũ và đường dẫn WebSocket không đổi.
