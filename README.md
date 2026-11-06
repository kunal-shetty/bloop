# Bloop

A Words-of-Wonder-style word-wheel crossword for Android, in pure Java, with a pixel-art world, a pixel cat you can dress up in the coin shop, daily streaks, word duels, timed mode, leaderboards and cloud save.

## What's in the box

```
core/   Pure-Java module (no Android). Fully tested.
  generator/  Dictionary + profanity filter, seeded crossword layout, level generator, 3 hand-made levels
  game/       GameSession rules, scoring, streak logic (with freezes), save format + merge, duel bot, the accessory shop
app/    Android app (Activity/View based, no XML layouts, no AndroidX)
  ui/         WheelView (swipe wheel), CrosswordView, AmbientView (pixel scenery and weather), SplashView,
              CatView, PixelModal (every dialog), PixelButton/ModeCard, PixelIcons (12x12 text-art icons)
  audio/      Sfx (chiptune pack), Haptics
  fx/         SpriteSheet + FxLayer: plays PixelFX effects exported as sprite sheets
  services/   PlayGamesService (sign-in, leaderboards, cloud save), AdsService (AdMob videos, ad breaks and consent), BillingService (Play purchases)
  assets/     words.txt, common.txt, fx/ (25 PixelFX effects), sfx/ (31 sounds), cat/ (layered art), fonts/ (Press Start 2P, VT323)
tools/export-fx.sh   Re-export PixelFX effects as sprite sheets
tools/gen_sfx.py     Regenerate the sound pack (needs numpy)
tools/gen_music.py   Regenerate the background music (needs numpy and soundfile)
tools/gen_cat.py     Regenerate the cat and its accessories (needs Pillow)
tools/gen_icon.py    Regenerate the launcher icon (needs Pillow)
tools/gen_home.py    Regenerate the cat home art (needs Pillow)
```

## Running

```
./gradlew :core:selfTest          # 35k checks: dictionary, generator, game rules, saves, and real races over local sockets
./gradlew :app:assembleDebug      # needs the Android SDK (open the folder in Android Studio)
```

## How the important parts work

- **Levels are deterministic.** `forProgression(n)`, `forDaily(epochDay)` and `forDuel(seed)` always return the same level on every device. So cloud save stores only a level *number*, the daily puzzle needs no server, and two duel players get an identical board from one shared seed. If you ever change the generator, bump `LevelGenerator.ALGORITHM_VERSION`.
- **Grid words are common words, and bonus words are rare.** Only words from a 10k common-words list can appear in the grid. A level also picks a few common words as bonus words (about one per three grid words, five at most); any other real word is shown as not being in the puzzle, with no reward and no penalty. Offensive words are removed from all of these (exact-match filter, so "class" is safe).
- **Difficulty** climbs slowly by level: 4 letters on a 6x6 grid (levels 4-15), 5 on 7x7, 6 on 8x8, 7 on 9x9, then 8 letters on 10x10 from level 171, with more words at each step. Daily puzzles get harder towards the weekend.
- **Journey:** `Journey` holds 20-level chapters, picks two or three hard levels per chapter from the level number, and pays chapter rewards. Hard levels come from `LevelGenerator.forHard`, so normal levels never change. Replays can raise a level's stars but pay no coins and add nothing to lifetime stats.
- **Streaks:** consecutive daily completions grow the streak; one missed day is forgiven if you have a freeze (earned every 7 days, max 3).
- **Shop:** `Shop` holds the catalogue and rules. Items are stored by id in the save, so add new ones with a new id, add their art to `tools/gen_cat.py`, and never renumber.
- **Achievements:** `Achievement` works out unlocks from stats already in the save, and only stores which rewards were claimed. Add new ones with a new id, and never renumber.
- **Accessibility:** `PixelTheme.applySettings` swaps the right-and-wrong colours and scales text. Keep `MINT` and `ROSE` non-final, and read them at draw time.
- **Quests:** `Quest` picks three goals from the week number. Progress is lifetime stats minus a snapshot from the start of the week, so replays can't complete one.
- **Cat home:** `Home` holds the furniture catalogue and rules. Items start in a slot and can be dragged. Art comes from `tools/gen_home.py`. Add new items with a new id, and never renumber.
- **Purchases:** `Purchases` holds the coin packs. Every paid grant goes in a ledger in the save, keyed by a hash of its purchase token, so a retry can't pay twice and neither a cloud sync nor a stale screen can lose it. `ProgressStore.save` adds back anything a screen's older copy is missing.
- **Ads:** `AdPolicy` holds the limits: no ad breaks until level 8 is finished, then one at most every 3 levels and 3 minutes, on campaign levels only. `remove_ads` stops ad breaks, and coin videos are capped at 5 a day.
- **Saves:** `Progress` serialises to `key=value` lines (stars are packed, 30 levels to a line); `Progress.merge` resolves local-vs-cloud conflicts without losing progress.

## Before you can run it on a phone (setup checklist)

1. Open the project in Android Studio and let it sync. Accept any AGP upgrade suggestion, and bump `compileSdk`/`targetSdk` to whatever Google Play currently requires. Check the three Google library versions in `app/build.gradle`.
2. **Play Games:** create the game in Play Console → Play Games Services, put the project ID in `strings.xml` (`play_games_app_id`) and create three leaderboards (daily, timed, duel) whose IDs go in `strings.xml`. Enable saved games.
3. **AdMob:** `strings.xml` ships with Google's *test* IDs. Replace `admob_app_id`, `admob_rewarded_unit` and `admob_interstitial_unit` with yours before release, and create a GDPR message under Privacy & messaging so the consent form appears. Debug builds keep using test ads through `app/src/debug/res/values/admob.xml`.
4. **Signing:** create an upload key (`keytool -genkeypair -v -keystore upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`), keep a backup, and add a `keystore.properties` next to `settings.gradle` with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`. Git ignores both files. Then run `./gradlew :app:bundleRelease`.
5. **Purchases:** upload that bundle to the internal testing track first, or Play won't let you create products. Then add `coins_small`, `coins_medium`, `coins_large` and `remove_ads` under Monetize > One-time products, and add yourself as a licence tester. Purchases only work in a copy installed from Play, not a sideloaded debug build.

## Honest status

- **Tested:** everything in `core/` (run `selfTest`).
- **Run on a real phone (Vivo V2143, Android 13):** the splash, menu, modals and core gameplay. Play Games sign-in, leaderboards, ads and purchases have not been exercised, because they need the console setup above.
- **Not built yet:** online multiplayer (Wi-Fi races work, see below).

## Multiplayer: Wi-Fi now, online later

**Wi-Fi races** need no server. One phone hosts (`core/lan`, plain TCP sockets), `LanDiscovery` announces the room with Android network service discovery, and up to three others join. Everyone plays one board from a shared seed, with live scores. Once a race starts, a player who leaves stays in the results, and the host has a hard deadline so a stalled phone cannot hang a race. Hints are off in a race, because a hint stops the clock.

The duel button still plays a deterministic **bot** when nobody is around. Opponents over the internet need a server. Play Games Services no longer provides multiplayer, so the plan is Firebase (Firestore or Realtime Database): a matchmaking doc holds the seed, each player writes their score timeline, and the client compares them. The board already comes from a shared seed, so the server only has to exchange seeds and scores.

## PixelFX art

Effects come from [patch-gg/pixelfx](https://github.com/patch-gg/pixelfx) (MIT). `tools/export-fx.sh <pixelfx checkout>` re-renders the chosen effects into `app/src/main/assets/fx/`. Full-screen weather loops were deliberately **not** shipped as sprite sheets (hundreds of 320×180 frames, tens of MB); `AmbientView` draws snow/petals/fireflies/leaves natively instead.

See `NOTICE.md` for licences and attributions of bundled data.
