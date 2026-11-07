# Third-party material

| What | Where | Source / licence |
|---|---|---|
| PixelFX sprite sheets | `app/src/main/assets/fx/` | [patch-gg/pixelfx](https://github.com/patch-gg/pixelfx), MIT. Parts adapted from Hugo Duprez's MIT effects (Sprite Fusion). Keep the MIT notice with any redistribution. |
| Press Start 2P, VT323 fonts | `app/src/main/assets/fonts/` | SIL Open Font License 1.1 (via google/fonts); licence text in `fonts/OFL.txt`. |
| ENABLE word list | `core/data/enable1.txt` (filtered into `app/.../words.txt`) | Public-domain Scrabble-style word list (via github.com/dolph/dictionary). |
| Common-words list | `core/data/common.txt` (filtered into `app/.../common.txt`) | google-10000-english by first20hours, derived from Google's Trillion Word Corpus. **Check the repository's licence terms before publishing.** |
| Word meanings | `app/src/main/assets/definitions.txt` | Built by `tools/gen_defs.py` from Princeton WordNet 3.1, under the WordNet licence, copied to `assets/WORDNET-LICENSE.txt`. Keep that file with any redistribution. |
| Offensive-word list | `core/data/badwords.txt` | LDNOOBW "List of Dirty, Naughty, Obscene, and Otherwise Bad Words", CC BY 4.0 as far as I know. **Verify and credit it in your store listing / in-app credits.** |

The Patch Pixel font in the PixelFX repo is *not* covered by its MIT licence and is not used here.

Sound effects in `assets/sfx/` are synthesised from scratch by `tools/gen_sfx.py`, and the cat art in `assets/cat/` is drawn by `tools/gen_cat.py`.
