# Bộ asset Poker cổ điển v2

Nguồn tham khảo: `C:\Users\Admin\Downloads\poker_custom_assets_v2.zip`. ZIP gốc gồm **10 ảnh PNG toàn màn hình đã ghép phẳng** (`screens/`), không có layer chip/avatar/nút tách sẵn. Vì vậy không thể lấy chính xác mọi vật thể không bị che từ ZIP. Bộ dưới đây là các asset **dựng lại theo ảnh mẫu**, tách riêng, không chứa chữ hoặc dữ liệu mẫu cố định. 10 ảnh gốc được giữ nguyên để đối chiếu.

## Tài nguyên sẵn dùng

| Thư mục | Nội dung | Nguồn/cách tạo |
| --- | --- | --- |
| `pack/frames/` | 11 khung: phòng, ghế, chat, popup, tìm kiếm, nhập chat, danh sách, tiêu đề, kết quả | Vẽ lại bằng Java2D, PNG có vùng ngoài trong suốt |
| `pack/buttons/` | 6 nền nút: đỏ, xanh, xanh lá, cam, vàng, vô hiệu | Vẽ lại bằng Java2D, không gắn chữ để dùng lại cho Fold/Check/Call/Raise/All-in... |
| `pack/icons/` | 16 biểu tượng: chất bài, vương miện, khóa, mắt, Wi-Fi mất kết nối, timer, check, close, send, users, chat, search, online | Vẽ lại bằng Java2D, nền trong suốt |
| `pack/markers/` | Dealer, Small Blind, Big Blind | Vẽ lại bằng Java2D |
| `pack/decor/` | Chồng chip trang trí | Ghép từ chip đã có |
| `chips/` | 6 chip 1, 5, 25, 100, 500, 1000 | Ảnh độ phân giải cao tạo theo phong cách ảnh mẫu |
| `avatars/` | 6 avatar người chơi | Ảnh độ phân giải cao tạo theo phong cách ảnh mẫu |
| `dealer/female-dealer.png` | Nhân vật nữ chia bài, PNG nền trong suốt, đứng ở giữa phía trên bàn | Tạo mới bằng công cụ imagegen tích hợp theo phong cách giao diện casino cổ điển |
| `backgrounds/` | Nền casino cổ điển | Ảnh tạo theo phong cách ảnh mẫu |
| `cards/` | Bộ bài poker | Giữ bộ đã có trong dự án theo yêu cầu |
| `table/poker-table-v2.png` | Bàn nỉ xanh, viền da đỏ và vàng đang dùng trong game | Tạo lại từ ảnh màn chơi mẫu bằng công cụ tạo/chỉnh ảnh; vùng ngoài bàn trong suốt |
| `table/poker-table.png` | Bàn cũ | Giữ để đối chiếu, không còn dùng trong màn chơi |
| `screens/` | 10 ảnh gốc từ ZIP | Chỉ để tham khảo, không đưa nguyên ảnh vào màn hình game |

Tổng: **37 PNG UI tách rời** trong `pack/`, thêm chip, avatar, nền, bài và bàn. Các khung/nút có chữ và giá trị động phải được vẽ chữ bằng JavaFX phía trên nền PNG. Không cắt nguyên nút có chữ từ screenshot vì sẽ dính văn bản tiếng Anh và bị mờ khi phóng lớn.

## Ánh xạ màn hình

| Ảnh gốc | Asset dùng cho giao diện |
| --- | --- |
| `01_lobby.png` | `frames/room-list.png`, `frames/room-card.png`, `frames/search-field.png`, `buttons/red.png`, `buttons/blue.png`, `icons/search.png`, `icons/users.png`, chip |
| `02_create_room.png` | `frames/settings-panel.png`, `buttons/red.png`, `icons/lock.png`, `icons/users.png`, bài |
| `03_join_private_room.png` | `frames/modal-cream.png`, `icons/lock.png`, `buttons/blue.png` |
| `04_waiting_room.png` | `frames/player-seat.png`, `frames/chat-panel.png`, `buttons/green.png`, `buttons/red.png`, `icons/crown.png`, avatar |
| `05_poker_game_room.png` | `table/poker-table-v2.png`, bài, đủ 6 chip, avatar, `frames/player-seat.png`, `markers/`, `buttons/` |
| Giao diện bàn chơi hiện tại | `dealer/female-dealer.png` là lớp trang trí, không phải ghế; các ghế được xoay theo góc nhìn từng người chơi |
| `06_room_chat.png` | `frames/chat-panel.png`, `frames/chat-input.png`, `buttons/red.png`, `icons/chat.png`, `icons/send.png`, avatar |
| `07_spectator_view.png` | Bàn, bài úp, `icons/eye.png`, `frames/player-seat.png` |
| `08_room_detail_preview.png` | `frames/modal-dark.png`, `frames/settings-panel.png`, chip |
| `09_reconnect_connection_lost.png` | `frames/modal-cream.png`, `icons/wifi-off.png`, `icons/timer.png` |
| `10_game_result_end_round.png` | `frames/result-panel.png`, `icons/crown.png`, `buttons/green.png`, chip và avatar |

## Sử dụng trong JavaFX

Nạp qua classpath: `AssetLoader.loadImageSized("reference-v2/pack/frames/chat-panel.png", 350, 450)`. Nút/khung nên có lớp `Label` hoặc `TextField` phía trên; không lưu số tiền, tên người chơi hay trạng thái trong ảnh. AssetLoader có cache theo đường dẫn và kích thước; chip/ảnh đại diện chỉ giải mã ở kích thước hiển thị để giảm bộ nhớ.

Mã tạo 37 asset UI ở `tools/ReferenceV2PackGenerator.java`; chạy từ thư mục `D:\Poker\LTM` bằng `java tools/ReferenceV2PackGenerator.java`. Không cần chạy khi mở game vì toàn bộ PNG đã được lưu sẵn.

## Prompt tạo bàn mới

Bàn `table/poker-table-v2.png` được tạo bằng công cụ imagegen tích hợp với `screens/05_poker_game_room.png` làm ảnh tham chiếu/chỉnh sửa. Prompt cuối:

> Isolate and faithfully reconstruct ONLY the large oval poker table seen in the middle-left of the screenshot: deep emerald green felt, thick oxblood/red burgundy leather padded outer rail, warm polished gold piping and cup-holder rings, dark carved wood outer edge, same slight elevated top-down perspective and proportions. Reconstruct portions hidden by players, chips and labels into a continuous clean table. Keep the felt completely empty for dynamic cards, pot, timer and player UI. Remove all people, avatars, chips, cards, text, buttons, labels, HUD, room background and side panels. Truly transparent outside the table; clean crisp alpha cutout, no opaque rectangular backdrop. Wide landscape composition, table centered and fully visible, no cropped edges. Preserve the recognizable classic casino aesthetic of the reference, no logos or watermark.

## Prompt nhân vật chia bài

Asset `dealer/female-dealer.png` được tạo bằng imagegen tích hợp (ảnh bàn người dùng gửi là tham chiếu phong cách):

> Create exactly one friendly adult female casino dealer standing, mid-thigh upward, facing the player, centered symmetrical pose, hands calmly holding a neatly squared deck of cards at waist level. Elegant vintage casino uniform in deep burgundy velvet, black waistcoat and subtle antique gold trim. Polished semi-realistic game illustration matching the warm mahogany, burgundy and gold classic poker interface. Clean readable silhouette at small UI sizes, detailed sharp face and hands, soft warm casino light. Genuinely transparent background with clean edges; no table, scenery, words, logos, watermark or other people.
