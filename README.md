# LunaManhunt

PaperMC plugin scaffold.

## PlaceholderAPI placeholders

Identifier: `%lunamanhunt_*%`

| Placeholder | Ý nghĩa | Ví dụ giá trị |
| --- | --- | --- |
| `%lunamanhunt_match_id%` | ID trận hiện tại của người chơi | `ABCDXYZ` hoặc `-` |
| `%lunamanhunt_match_exists%` | Người chơi có đang ở trong một trận hay không | `true`, `false` |
| `%lunamanhunt_match_players%` | Số người chơi hiện tại trong phòng | `1`, `4`, ... |
| `%lunamanhunt_match_hunters%` | Số thợ săn hiện tại trong trận | `0`, `2`, ... |
| `%lunamanhunt_match_owner%` | Tên chủ phòng (fallback UUID rút gọn nếu offline) | `Belikhun`, `f84c6a79` |
| `%lunamanhunt_match_speedrunner%` | Tên speedrunner hiện tại | `Belikhun`, `-` |
| `%lunamanhunt_match_timer%` | Đồng hồ trận hiện tại dạng `mm:ss.SSS` | `03:42.517` |
| `%lunamanhunt_role%` | Vai trò kỹ thuật của người chơi trong trận | `speedrunner`, `hunter`, `lobby`, `none` |
| `%lunamanhunt_role_display%` | Tên vai trò để hiển thị | `Speedrunner`, `Thợ săn`, `Sảnh`, `Không có` |
| `%lunamanhunt_role_color%` | Màu vai trò dạng cặp gradient `start:end` | `#38F8FF:#8B5CFF`, `#FFB347:#FF4D6D`, `#FFFFFF` |
| `%lunamanhunt_match_state%` | Trạng thái trận hiện tại (lowercase enum) | `running`, `run_pending`, `lobby`, `none` |
| `%lunamanhunt_match_state_display%` | Trạng thái trận hiển thị tiếng Việt | `Phòng chờ`, `Đang chạy`, `Chưa tham gia` |
| `%lunamanhunt_profile_total_wins%` | Tổng số trận thắng trong hồ sơ | `12` |
| `%lunamanhunt_profile_total_losses%` | Tổng số trận thua trong hồ sơ | `8` |
| `%lunamanhunt_profile_total_matches%` | Tổng số trận trong lịch sử | `20` |
| `%lunamanhunt_profile_win_rate%` | Tỉ lệ thắng theo % (1 chữ số thập phân) | `60.0` |
| `%lunamanhunt_profile_best_time%` | Kỷ lục speedrun cá nhân dạng `mm:ss.SSS` | `18:24.903` hoặc `--:--.---` |
| `%lunamanhunt_profile_kills%` | Tổng số hunter bị hạ trong lịch sử | `37` |

## Cấu hình hiển thị tên người chơi

- `ui.player-name-format`: định dạng tên người chơi dùng chung trong thông báo/UI, hỗ trợ MiniMessage và PlaceholderAPI.
- Mặc định: `<gradient:#7fd36b:#8fd3ff>%player_name%</gradient>`.
- Placeholder nội bộ luôn hỗ trợ: `%player_name%`, `%player_display_name%`, `%player_uuid%`.
