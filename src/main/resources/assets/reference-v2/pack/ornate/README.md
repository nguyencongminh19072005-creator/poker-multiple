# Ornate v2 UI artwork

These seven transparent PNGs are reusable, text-free reconstructions based on the flattened reference images in `../../screens/`. They are integrated through `com.poker.view.OrnateUi` and `AssetLoader`; names, money, actions, and player state remain live JavaFX nodes.

| File | Reference | Intended placement |
| --- | --- | --- |
| `title-banner.png` | `01_lobby.png` | Lobby/game heading |
| `room-card.png` | `01_lobby.png` | Live room rows |
| `wood-panel.png` | `01_lobby.png` | Lobby/create-room/list panels |
| `chat-panel.png` | `06_room_chat.png` | Game log/chat frame |
| `player-seat.png` | `05_poker_game_room.png` | Live player seat |
| `button-red.png` | `05_poker_game_room.png` | Primary/fold/raise actions |
| `button-blue.png` | `05_poker_game_room.png` | Join/check/call actions |

Generation method: OpenAI built-in image generation with each screenshot supplied as an image-edit reference. No screenshot was displayed as a static whole-screen UI.

Prompt set: reconstruct an individual blank old-world casino UI component from its reference; preserve carved mahogany, quilted ruby velvet, polished antique-gold bevels and subtly painted highlights; remove all text, icons and unrelated elements; use a transparent exterior and a front-on high-resolution composition. For buttons, use a deep ruby or sapphire enamel center. For the chat and wood panels, keep a dark empty interior suitable for live content.

The 52-card deck, six existing avatar images, six existing chip denominations and the existing v2 poker table remain separate assets; they are not embedded in these frames. To run: from `D:\Poker\LTM`, use `mvn javafx:run` with the project's usual Java/Maven setup.
