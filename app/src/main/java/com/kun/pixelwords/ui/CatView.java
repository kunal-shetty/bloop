package com.kun.pixelwords.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;

import com.kun.pixelwords.audio.Haptics;
import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.game.Shop;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The cat: stacks the 32x32 art layers from assets/cat (see tools/gen_cat.py) at a whole-number scale.
 * It breathes, wags, blinks and looks around on its own; tap it for a hop, a mew and some hearts.
 */
public class CatView extends View {

    private static final int ART = 32;
    private static final long HOP_MS = 360;
    /** The cat keeps hopping if you spam it, but only mews this often. */
    private static final long MEW_GAP_MS = 1200;
    private static final Map<String, Bitmap> CACHE = new HashMap<>();

    private static final class Heart {
        float x, y;
        long born;
    }

    private final Paint paint = new Paint();
    private final Paint shadowPaint = new Paint();
    private final Rect src = new Rect(0, 0, ART, ART);
    private final Rect dst = new Rect();
    private final Random rnd = new Random();
    private final List<Heart> hearts = new ArrayList<>();
    private final float wantDp;

    private Shop.Item head, face, neck;
    private long nextBlink, blinkUntil, hopAt, happyUntil, lastMew;
    private boolean cheering;

    public CatView(Context context, float desiredSizeDp) {
        super(context);
        wantDp = desiredSizeDp;
        paint.setFilterBitmap(false);
        shadowPaint.setColor(0x66000000);
        nextBlink = SystemClock.uptimeMillis() + 1500;
        setSoundEffectsEnabled(false);
        setOnClickListener(v -> pet());
    }

    /** Dresses the cat in whatever the player is wearing. */
    public void dress(Progress p) {
        head = Shop.wearing(p, Shop.Slot.HEAD);
        face = Shop.wearing(p, Shop.Slot.FACE);
        neck = Shop.wearing(p, Shop.Slot.NECK);
        invalidate();
    }

    /** Puts an item on just for this view, as a try-on. */
    public void tryOn(Shop.Item item) {
        if (item.slot == Shop.Slot.HEAD) head = item;
        else if (item.slot == Shop.Slot.FACE) face = item;
        else neck = item;
        invalidate();
    }

    /** Keeps hopping happily; used when celebrating. */
    public void setCheering(boolean on) {
        cheering = on;
        invalidate();
    }

    public void hop() {
        hopAt = SystemClock.uptimeMillis();
        happyUntil = hopAt + 900;
        invalidate();
    }

    private void pet() {
        hop();
        if (hopAt - lastMew >= MEW_GAP_MS) {
            lastMew = hopAt;
            Sfx.play(Sfx.Id.MEOW, 0.92f + rnd.nextFloat() * 0.2f);
        }
        Haptics.click();
        Heart h = new Heart();
        h.x = getWidth() * (0.3f + rnd.nextFloat() * 0.4f);
        h.y = getHeight() * 0.3f;
        h.born = hopAt;
        hearts.add(h);
    }

    static Bitmap layer(Context c, String name) {
        Bitmap b = CACHE.get(name);
        if (b != null) return b;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        try (InputStream in = c.getAssets().open("cat/" + name + ".png")) {
            b = BitmapFactory.decodeStream(in, null, o);
        } catch (IOException e) {
            return null;
        }
        CACHE.put(name, b);
        return b;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int want = (int) PixelTheme.dp(getContext(), wantDp);
        setMeasuredDimension(resolveSize(want, widthSpec), resolveSize(want, heightSpec));
    }

    private void draw(Canvas c, String name) {
        Bitmap b = layer(getContext(), name);
        if (b != null) c.drawBitmap(b, src, dst, paint);
    }

    private void wear(Canvas c, Shop.Item item) {
        if (item != null) draw(c, "acc_" + item.key);
    }

    @Override
    protected void onDraw(Canvas c) {
        long now = SystemClock.uptimeMillis();
        int w = getWidth(), h = getHeight();
        // Never bigger than asked for, however much room the layout offers.
        int limit = Math.min(Math.min(w, h), (int) PixelTheme.dp(getContext(), wantDp));
        int scale = Math.max(1, limit / ART);
        int size = scale * ART;
        int left = (w - size) / 2, top = h - size;

        if (cheering && now - hopAt > HOP_MS + 180) hop();
        if (now >= nextBlink) {
            blinkUntil = now + 130;
            nextBlink = now + 1800 + rnd.nextInt(3200);
        }

        // Hop height comes from a parabola; the idle breath is a slow one-pixel bob.
        int lift = 0;
        long since = now - hopAt;
        if (since < HOP_MS) {
            float k = since / (float) HOP_MS;
            lift = Math.round(4f * k * (1f - k) * 7f) * scale;
        } else if ((now / 700) % 2 == 0) {
            lift = scale;
        }

        // Ground shadow stays put and shrinks as the cat leaves the floor.
        int shadow = size * 5 / 16 - lift / 2, floor = top + size - scale * 2;
        c.drawRect(left + size / 2 - shadow, floor, left + size / 2 + shadow, floor + scale * 2, shadowPaint);

        dst.set(left, top - lift, left + size, top + size - lift);
        boolean happy = now < happyUntil;
        draw(c, "tail_" + (happy ? (now / 90) % 3 : (now / 340) % 3));
        draw(c, "body");
        draw(c, happy ? "eyes_happy" : now < blinkUntil ? "eyes_closed" : "eyes_open");
        wear(c, neck);
        wear(c, face);
        wear(c, head);

        float heartSize = Math.max(24f, scale * 4f);
        for (int i = hearts.size() - 1; i >= 0; i--) {
            Heart b = hearts.get(i);
            float k = (now - b.born) / 900f;
            if (k >= 1f) {
                hearts.remove(i);
                continue;
            }
            PixelIcons.draw(c, paint, "heart", b.x, b.y - k * getHeight() * 0.3f, heartSize);
        }
        postInvalidateOnAnimation();
    }
}
