package com.kun.pixelwords.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

/** Menu tile for one game mode: icon, name, one line of live status. */
public class ModeCard extends PressableView {

    private static final float HEIGHT_DP = 104;
    private static final float PAD_DP = 10;
    private static final float ICON_DP = 36;

    private final Paint paint = new Paint();
    private final Paint title = new Paint();
    private final Paint sub = new Paint();
    private final String icon;
    private final String name;
    private String status = "";
    private String badge = "";
    private int fill;

    public ModeCard(Context context, String icon, String name, int fill) {
        super(context);
        this.icon = icon;
        this.name = name;
        this.fill = fill;
        PixelTheme.styleDisplay(title, PixelTheme.displayPx(context, PixelTheme.SIZE_LABEL));
        PixelTheme.styleBody(sub, PixelTheme.bodyPx(context, PixelTheme.SIZE_BODY));
    }

    public void setStatus(String s) {
        status = s;
        invalidate();
    }

    public void setBadge(String s) {
        badge = s;
        invalidate();
    }

    public void setFill(int color) {
        fill = color;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        // Taller than the usual height only when larger text needs the room to clear the icon.
        Context ctx = getContext();
        float icon = PixelTheme.dp(ctx, ICON_DP) + px * 4;
        float text = title.getTextSize() + sub.getTextSize() + PixelTheme.dp(ctx, 4);
        float frame = (px + PixelTheme.dp(ctx, PAD_DP)) * 2 + depth;
        int h = (int) Math.max(PixelTheme.dp(ctx, HEIGHT_DP), icon + text + frame);
        setMeasuredDimension(resolveSize(h, widthSpec), resolveSize(h, heightSpec));
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), s = sink();
        PixelTheme.drawTile(c, paint, 0, s, w, h, fill, px, depth - s);

        float pad = PixelTheme.dp(getContext(), PAD_DP);
        float size = PixelTheme.dp(getContext(), ICON_DP);
        float top = px + s + pad;
        // A dark medallion keeps every icon readable on any card colour.
        PixelTheme.drawSlot(c, paint, pad, top, pad + size + px * 4, top + size + px * 4, PixelTheme.shade(fill, 0.42f), px);
        PixelIcons.draw(c, paint, icon, pad + px * 2, top + px * 2, size);

        float textY = h - depth - px - pad;
        sub.setColor(PixelTheme.INK);
        c.drawText(status, pad, textY, sub);
        title.setColor(PixelTheme.INK);
        c.drawText(name, pad, textY - sub.getTextSize() - PixelTheme.dp(getContext(), 1), title);

        if (!badge.isEmpty()) {
            float bw = sub.measureText(badge) + pad;
            float bh = sub.getTextSize() + px * 3;
            float l = w - bw - pad * 0.6f, t = top;
            PixelTheme.drawTile(c, paint, l, t, l + bw, t + bh, PixelTheme.INK, px, 0);
            sub.setColor(PixelTheme.GOLD);
            c.drawText(badge, l + pad / 2f, t + bh * 0.76f, sub);
        }
    }
}
