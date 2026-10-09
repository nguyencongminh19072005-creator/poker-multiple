# Bundled game assets

Xem [STATE_MAP.md](STATE_MAP.md) để biết asset/trạng thái nào đang được JavaFX
sử dụng, trạng thái kết quả nào chưa nối dữ liệu và danh sách ảnh trùng/chỉ
tham khảo. Chưa xóa asset nào trong đợt rà soát này.

Bộ asset giao diện Texas Hold'em Poker chuẩn hóa, được cắt trực tiếp từ thiết kế sprite sheet và tổ chức theo nhóm:

## Danh mục Assets

| Nhóm | Tên file | Mô tả |
| :--- | :--- | :--- |
| **Pages** | `pages/lobby.png` | Giao diện Lobby phòng chơi |
| | `pages/create-room.png` | Giao diện tạo bàn chơi |
| | `pages/waiting-room.png` | Giao diện phòng chờ |
| | `pages/poker-game-room.png` | Bàn Poker chính hoàn chỉnh |
| | `pages/spectator-view.png` | Giao diện người xem (Spectator) |
| | `pages/game-result.png` | Bảng kết quả ván bài / Winner |
| **Modals / Popup** | `modals/join-private-room.png` | Popup nhập mật khẩu phòng riêng |
| | `modals/room-detail.png` | Popup xem trước thông tin phòng |
| | `modals/connection-lost.png` | Popup thông báo mất kết nối |
| **Chat** | `chat/room-chat.png` | Khung chat trong phòng |
| | `chat/room-chat-inroom.png` | Khung chat thu nhỏ trên bàn game |
| **Poker components**| `poker-components/poker-table.png` | Bàn Poker sạch không người chơi (transparent BG) |
| | `poker-components/playing-card-back.png` | Mặt sau lá bài đỏ hoa văn (transparent BG) |
| | `poker-components/playing-cards.png` | Dải đầy đủ bộ bài tây 52 lá |
| | `poker-components/poker-chips.png` | Bảng các loại chip cược |
| | `poker-components/dealer-button.png` | Nút Dealer tròn (D) |
| | `poker-components/small-blind.png` | Nút Small Blind (SB) |
| | `poker-components/big-blind.png` | Nút Big Blind (BB) |
| **Player** | `player/player-seat.png` | Khung vị trí và bục người chơi |
| | `player/player-avatar.png` | Avatar người chơi mẫu |
| | `player/host-crown.png` | Biểu tượng vương miện chủ phòng |
| | `player/ready-status.png` | Huy hiệu trạng thái Sẵn sàng (READY) |
| | `player/unready-status.png` | Huy hiệu trạng thái Chưa sẵn sàng |
| **Actions** | `actions/btn-fold.png` | Nút Bỏ bài (Fold) |
| | `actions/btn-check.png` | Nút Xem bài (Check) |
| | `actions/btn-call.png` | Nút Theo cược (Call) |
| | `actions/btn-raise.png` | Nút Tố cược (Raise) |
| | `actions/btn-allin.png` | Nút Tất tay (All-in) |
| **Realtime** | `realtime/timer.png` | Đồng hồ đếm ngược lượt đánh (Turn Timer) |
| | `realtime/online-status.png` | Đèn trạng thái trực tuyến (Online) |
| | `realtime/reconnecting.png` | Biểu tượng / spinner đang kết nối lại |

## Các thư mục tương thích ngược

- `table/poker-table.png`: Bàn poker sạch, nền trong suốt.
- `cards/card-back.png`: Mặt sau lá bài chuẩn.
- `chips/`: Các mệnh giá chip riêng lẻ (`chip_white_1.png`, `chip_red_5.png`, `chip_green_25.png`, `chip_black_100.png`, `chip_purple_500.png`, `chip_orange_1000.png`).
- `button/`: Các nút thao tác dạng ảnh cắt riêng.
- `logo/poker-logo.png`: Logo Poker World Champions.
- `backgrounds/`: Các ảnh nền casino, auth, lobby.

`AssetLoader` tự động nhận diện và cung cấp các hàm getter thuận tiện cho toàn bộ danh mục trên.

