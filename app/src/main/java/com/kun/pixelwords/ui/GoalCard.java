package com.kun.pixelwords.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;

import com.kun.pixelwords.game.Achievement;
import com.kun.pixelwords.game.Progress;

/** One achievement: a medal in its tier's colour, then progress, a shining claim prompt, or a tick. */
public class AchievementCard extends PressableView {

    private static final int BRONZE = 0xFFD08A4E;
    private static final int SILVER = 0xFFCFD8EA;
    private static final int LOCKED_DIM = 0xA0251D4A;
    private static final int BAR_SEGMENTS = 12;
    private static final long SHINE_EVERY_MS = 2200;
    private static final long SHINE_SWEEP_MS = 1100;

    public final Achievement achievement;
    private final Paint paint = new Paint();
    private final Paint title = new Paint();
    private final Paint count = new Paint();
    private final Paint label = new Paint();
    private int value;
    private boolean unlocked, claimed;

    public AchievementCard(Context context, Achievement achievement) {
        super(context);
        this.achievement = achievement;
        paint.setFilterBitmap(false);
        PixelTheme.styleBody(title, PixelTheme.bodyPx(context, 19));
        PixelTheme.styleBody(count, PixelTheme.bodyPx(context, 16));
        count.setTextAlign(Paint.Align.RIGHT);
        PixelTheme.styleDisplay(label, PixelTheme.displayPx(context, 9));
    }

    public void update(Progress p) {
        value = achievement.progress(p);
        unlocked = achievement.isUnlocked(p);
        claimed = achievement.isClaimed(p);
        invalidate();
    }

    private float dp(float v) {
        return PixelTheme.dp(getContext(), v);
    }

    private int medalColor() {
        switch (achievement.tier) {
            case BRONZE: return BRONZE;
            case SILVER: return SILVER;
            default: return PixelTheme.GOLD;
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(resolveSize(0, widthSpec), (int) dp(88));
    }

    @Override
    protected void onDraw(Canvas c) {
        boolean claimable = unlocked && !claimed;
        float w = getWidth(), h = getHeight(), s = sink(), pad = dp(10);
        int fill = claimable ? PixelTheme.GOLD : claimed ? 0xFF2F2660 : PixelTheme.PANEL;
        PixelTheme.drawTile(c, paint, 0, s, w, h, fill, px, depth - s);

        float size = dp(62), top = s + (h - depth - size) / 2f;
        drawMedal(c, pad, top, size);

        float textX = pad + size + dp(10), right = w - pad;
        float titleY = s + dp(32), rowY = s + dp(62);
        title.setColor(claimable ? PixelTheme.INK : claimed ? PixelTheme.TEXT_DIM : PixelTheme.TEXT);
        c.drawText(achievement.title, textX, titleY, title);

        if (claimed) {
            label.setColor(PixelTheme.MINT);
            c.drawText("CLAIMED", textX, rowY, label);
        } else {
            float rewardX = drawReward(c, right, rowY, claimable);
            if (claimable) {
                label.setColor(PixelTheme.INK);
                c.drawText("TAP TO CLAIM", textX, rowY, label);
            } else {
                count.setColor(PixelTheme.TEXT_DIM);
                c.drawText(value + " / " + achievement.goal, right, titleY, count);
                drawBar(c, textX, rowY - dp(13), rewardX - dp(10), rowY + dp(3));
            }
        }
        if (claimable) {
            drawShine(c, w, s + px, h - depth - px);
            postInvalidateOnAnimation();
        }
    }

    private void drawMedal(Canvas c, float x, float y, float size) {
        boolean lit = unlocked;
        int medal = medalColor();
        PixelTheme.drawTile(c, paint, x, y, x + size, y + size, lit ? medal : PixelTheme.shade(medal, 0.55f), px, px * 2);
        float inset = px * 3;
        PixelTheme.drawSlot(c, paint, x + inset, y + inset, x + size - inset, y + size - inset - px * 2, PixelTheme.BG_MID, px);
        float icon = size * 0.56f;
        float ix = x + (size - icon) / 2f, iy = y + (size - icon - px * 2) / 2f;
        PixelIcons.draw(c, paint, achievement.icon, ix, iy, icon);
        if (!lit) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(LOCKED_DIM);
            c.drawRect(ix, iy, ix + icon, iy + icon, paint);
        }
        if (claimed) {
            float tick = dp(24);
            PixelIcons.draw(c, paint, "check", x + size - tick * 0.8f, y + size - tick * 0.9f, tick);
        }
    }

    /** Draws the coin and amount against the right edge, and returns where they start. */
    private float drawReward(Canvas c, float right, float baseline, boolean onGold) {
        String amount = "+" + achievement.tier.coins;
        float icon = dp(18), gap = dp(4);
        float x = right - label.measureText(amount);
        label.setColor(onGold ? PixelTheme.INK : PixelTheme.GOLD);
        c.drawText(amount, x, baseline, label);
        x -= icon + gap;
        PixelIcons.draw(c, paint, "coin", x, baseline - icon * 0.82f, icon);
        return x;
    }

    private void drawBar(Canvas c, float l, float t, float r, float b) {
        PixelTheme.drawSlot(c, paint, l, t, r, b, PixelTheme.BG_DEEP, px);
        float inner = r - l - px * 4;
        float seg = (inner - px * (BAR_SEGMENTS - 1)) / BAR_SEGMENTS;
        float share = value / (float) achievement.goal;
        // Any progress at all lights one segment, so a started goal never looks untouched.
        int lit = value == 0 ? 0 : Math.max(1, (int) (share * BAR_SEGMENTS));
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < BAR_SEGMENTS; i++) {
            float x = l + px * 2 + i * (seg + px);
            paint.setColor(i < lit ? PixelTheme.MINT : 0xFF1D1740);
            c.drawRect(x, t + px * 2, x + seg, b - px * 2, paint);
        }
    }

    private void drawShine(Canvas c, float w, float top, float bottom) {
        long t = SystemClock.uptimeMillis() % SHINE_EVERY_MS;
        if (t >= SHINE_SWEEP_MS) return;
        float band = w * 0.14f;
        float x = -band + (w + band) * (t / (float) SHINE_SWEEP_MS);
        c.save();
        c.clipRect(px, top, w - px, bottom);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x55FFFFFF);
        c.drawRect(x, top, x + band, bottom, paint);
        paint.setColor(0x33FFFFFF);
        c.drawRect(x + band + px * 2, top, x + band + px * 5, bottom, paint);
        c.restore();
    }
}
