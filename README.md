# LunaManhunt

PaperMC plugin scaffold.

## PlaceholderAPI placeholders

Identifier: `%mh_*%`

| Placeholder | Ý nghĩa | Ví dụ giá trị |
| --- | --- | --- |
| `%mh_match_id%` | ID trận hiện tại của người chơi | `ABCDXYZ` hoặc `-` |
| `%mh_match_id_hash%` | ID trận kèm `#` | `#ABCDXYZ`, `#-` |
| `%mh_match_id_colored%` | ID trận có màu (MiniMessage) | `<gradient...>#ABCDXYZ</gradient>` |
| `%mh_match_id_chip%` | Chip ID trận có biểu tượng | `<white>⌂</white> ...` |
| `%mh_match_exists%` | Người chơi có đang ở trong một trận hay không | `true`, `false` |
| `%mh_match_players%` | Số người chơi hiện tại trong phòng | `1`, `4`, ... |
| `%mh_match_hunters%` | Số thợ săn hiện tại trong trận | `0`, `2`, ... |
| `%mh_match_owner%` | Tên chủ phòng (fallback UUID rút gọn nếu offline) | `Belikhun`, `f84c6a79` |
| `%mh_match_speedrunner%` | Tên speedrunner hiện tại | `Belikhun`, `-` |
| `%mh_match_timer%` | Đồng hồ trận hiện tại dạng `mm:ss.SSS` | `03:42.517` |
| `%mh_role%` | Vai trò kỹ thuật của người chơi trong trận | `speedrunner`, `hunter`, `lobby`, `none` |
| `%mh_role_display%` | Tên vai trò để hiển thị | `Speedrunner`, `Thợ săn`, `Sảnh`, `Không có` |
| `%mh_role_color%` | Màu vai trò dạng cặp gradient `start:end` | `#38F8FF:#8B5CFF`, `#FFB347:#FF4D6D`, `#FFFFFF` |
| `%mh_match_state%` | Trạng thái trận hiện tại (lowercase enum) | `running`, `run_pending`, `lobby`, `none` |
| `%mh_match_state_display%` | Trạng thái trận hiển thị tiếng Việt | `Phòng chờ`, `Đang chạy`, `Chưa tham gia` |
| `%mh_profile_total_wins%` | Tổng số trận thắng trong hồ sơ | `12` |
| `%mh_profile_total_losses%` | Tổng số trận thua trong hồ sơ | `8` |
| `%mh_profile_total_matches%` | Tổng số trận trong lịch sử | `20` |
| `%mh_profile_win_rate%` | Tỉ lệ thắng theo % (1 chữ số thập phân) | `60.0` |
| `%mh_profile_best_time%` | Kỷ lục speedrun cá nhân dạng `mm:ss.SSS` | `18:24.903` hoặc `--:--.---` |
| `%mh_profile_kills%` | Tổng số hunter bị hạ trong lịch sử | `37` |
| `%mh_profile_hunter_kills%` | Alias của `%mh_profile_kills%` | `37` |

## Command aliases

- Lệnh quản trị chính: `/lm`
- Alias tương đương: `/manhunt`, `/hunt`, `/mh`

## Cấu hình hiển thị tên người chơi

- `ui.player-name-format`: định dạng tên người chơi dùng chung trong thông báo/UI, hỗ trợ MiniMessage và PlaceholderAPI.
- Mặc định: `<gradient:#7fd36b:#8fd3ff>%player_name%</gradient>`.
- Placeholder nội bộ luôn hỗ trợ: `%player_name%`, `%player_display_name%`, `%player_uuid%`.
