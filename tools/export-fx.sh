#!/usr/bin/env bash
# Re-exports PixelFX effects (https://github.com/patch-gg/pixelfx, MIT) as sprite sheets for the app.
# Usage: tools/export-fx.sh /path/to/pixelfx-checkout
# Produces app/src/main/assets/fx/<category>_<name>.png plus a compact <category>_<name>.fx.json
# (fps, loop, frames, frameSize, columns) that SpriteSheet.java reads.
set -euo pipefail
PFX="${1:?path to pixelfx checkout}"
DEST="$(cd "$(dirname "$0")/.." && pwd)/app/src/main/assets/fx"
EFFECTS=(
  ui/combo-counter ui/confetti ui/coin-collect ui/countdown ui/achievement ui/quest-complete
  pickups/coin-spin pickups/gem-spin pickups/collect-burst pickups/chest-open pickups/heart-pickup pickups/power-star
  transitions/iris transitions/star-iris
  holy/sunburst
)
mkdir -p "$DEST"
( cd "$PFX" && npm ci --silent )
for e in "${EFFECTS[@]}"; do
  n="$(basename "$e")"; c="$(dirname "$e")"
  ( cd "$PFX" && node tools/render.mjs "$e" --sheet >/dev/null )
  cp "$PFX/records/$e/$n.png" "$DEST/${c}_${n}.png"
  python3 - "$PFX/records/$e/$n.json" "$DEST/${c}_${n}.fx.json" <<'PY'
import json,sys
m=json.load(open(sys.argv[1]))['meta']['pixelfx']
json.dump({k:m[k] for k in ('fps','loop','frames','frameSize','pivot','columns')},open(sys.argv[2],'w'),separators=(',',':'))
PY
  echo "exported $e"
done
