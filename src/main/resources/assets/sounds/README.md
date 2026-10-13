# Poker sound effects

The JavaFX client loads these 13 PCM WAV files through `SoundEffects`.

The deal, result, and button sounds are Mixkit WAV recordings. The seven
remaining Pixabay-style effects use public previews from the matching CC0
Freesound recordings, decoded from OGG to 16-bit PCM WAV. Longer recordings
were cut to one action. Poker card sounds play when each card reaches the table.

| Game event | Resource | Recording and source | Edit |
| --- | --- | --- | --- |
| Deal | `deal.wav` | [Poker card flick, Mixkit](https://mixkit.co/free-sound-effects/casino/) (`2002.wav`) | 0.15–0.70 s |
| Flop / Turn / River | `flip.wav` | [flipCard, Splashdust](https://freesound.org/people/Splashdust/sounds/84322/) | 0.26–0.79 s, reduced low frequencies |
| Check | `check.wav` | [Tap on Wooden Table, zembacraftworks](https://freesound.org/people/zembacraftworks/sounds/427393/) | full preview |
| Call | `call.wav` | [Poker Chip Dropping, deleted_user_7146007](https://freesound.org/people/deleted_user_7146007/sounds/383870/) | 1.05–1.70 s |
| Bet / Raise | `bet_raise.wav` | [poker_chips1, fartheststar](https://freesound.org/people/fartheststar/sounds/201807/) | full preview |
| Fold | `fold.wav` | [cards thrown, khiser83](https://freesound.org/people/khiser83/sounds/336353/) | 1.90–2.65 s |
| All-in | `all_in.wav` | [AllInPushChips, Joma86](https://freesound.org/people/Joma86/sounds/532861/) | full preview |
| Payout to winner | `payout.wav` | [Clinking coins, Mixkit](https://mixkit.co/free-sound-effects/casino/) (`1993.wav`) | original WAV |
| Win | `win.wav` | [Payout award, Mixkit](https://mixkit.co/free-sound-effects/casino/) (`1934.wav`) | original WAV |
| Lose | `lose.wav` | [Losing piano, Mixkit](https://mixkit.co/free-sound-effects/lose/) (`2024.wav`) | original WAV |
| Your turn | `your_turn.wav` | [Interface Sounds, Kenney](https://kenney.nl/assets/interface-sounds) (`bong_001.ogg`) | decoded to WAV |
| Countdown | `countdown.wav` | [Ticking Clock_1, Sonoticai](https://freesound.org/people/Sonoticai/sounds/523464/) | 0.50–0.90 s, amplified |
| Button click | `button_click.wav` | [Cool interface click tone, Mixkit](https://mixkit.co/free-sound-effects/click/) (`2568.wav`) | original WAV |

The Freesound recordings also appear in [Pixabay's sound effects catalog](https://pixabay.com/sound-effects/). The Kenney Interface Sounds pack is CC0. Mixkit effects are licensed for use in video games under its [Sound Effects Free License](https://mixkit.co/license/modal/sfxFree/); that license restricts redistribution as standalone sounds or with source files, so check it before publishing project source or an asset pack.
