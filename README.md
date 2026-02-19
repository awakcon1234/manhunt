# LunaManhunt

PaperMC plugin scaffold.

## PlaceholderAPI placeholders

Identifier: `%lunamanhunt_*%`

| Placeholder | Ý nghĩa | Ví dụ giá trị |
| --- | --- | --- |
| `%lunamanhunt_match_id%` | ID trận hiện tại của người chơi | `ABCDXYZ` hoặc `-` |
| `%lunamanhunt_role%` | Vai trò kỹ thuật của người chơi trong trận | `speedrunner`, `hunter`, `lobby`, `none` |
| `%lunamanhunt_role_display%` | Tên vai trò để hiển thị | `Speedrunner`, `Thợ săn`, `Sảnh`, `Không có` |
| `%lunamanhunt_role_color%` | Màu vai trò dạng cặp gradient `start:end` | `#38F8FF:#8B5CFF`, `#FFB347:#FF4D6D`, `#FFFFFF` |
| `%lunamanhunt_match_state%` | Trạng thái trận hiện tại (lowercase enum) | `running`, `run_pending`, `lobby`, `none` |
