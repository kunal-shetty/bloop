package com.kun.pixelwords.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.kun.pixelwords.game.Home;
import com.kun.pixelwords.game.Progress;

/** One piece of furniture for sale: the item, its name, and its price or whether it's in the room. */
public class HomeCard extends PressableView {

    private static final float HEIGHT_DP = 128, ART_DP = 58, COIN_DP = 18;

    public final Home.Item item;
    private final Paint paint = new Paint();
    private final Paint name = new Paint();
    private final Paint price = new Paint();
    private final Rect dst = new Rect();
    private final Bitmap art;
    private boolean owned, inRoom, affordable;

    public HomeCard(Context context, Home.Item item) {
        super(context);
        this.item = item;
        paint.setFilterBitmap(false);
        PixelTheme.styleBody(name, PixelTheme.bodyPx(context, PixelTheme.SIZE_BODY));
        name.setTextAlign(Paint.Align.CENTER);
        PixelTheme.styleDisplay(price, PixelTheme.displayPx(context, PixelTheme.SIZE_CAPTION));
        art = RoomView.art(context, item.key);
    }

    public void update(Progress p) {
        owned = Home.owns(p, item);
        inRoom = Home.placed(p, item.slot) == item;
        affordable = Home.canAfford(p, item);
        invalidate();
    }

    private float dp(float v) {
        return PixelTheme.dp(getContext(), v);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(resolveSize(0, widthSpec), (int) dp(HEIGHT_DP));
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), s = sink(), pad = dp(8);
        int fill = inRoom ? 0xFF2FA873 : owned ? PixelTheme.PANEL_LIGHT : PixelTheme.PANEL;
        PixelTheme.drawTile(c, paint, 0, s, w, h, fill, px, depth - s);

        float slotTop = s + px + pad * 0.7f, slotH = dp(ART_DP);
        PixelTheme.drawSlot(c, paint, pad, slotTop, w - pad, slotTop + slotH, 0xFF8E7FD0, px);
        if (art != null) {
            int scale = (int) Math.max(1, Math.min((w - pad * 3) / art.getWidth(), (slotH - pad) / art.getHeight()));
            int dw = art.getWidth() * scale, dh = art.getHeight() * scale;
            int l = (int) (w - dw) / 2, t = (int) (slotTop + (slotH - dh) / 2f);
            dst.set(l, t, l + dw, t + dh);
            c.drawBitmap(art, null, dst, paint);
        }

        name.setColor(PixelTheme.TEXT);
        c.drawText(item.title, w / 2f, slotTop + slotH + name.getTextSize() * 0.95f, name);

        float rowY = h - depth - px - pad * 0.7f;
        if (owned) {
            String label = inRoom ? "IN ROOM" : "OWNED";
            price.setColor(inRoom ? PixelTheme.CREAM : PixelTheme.TEXT_DIM);
            c.drawText(label, (w - price.measureText(label)) / 2f, rowY, price);
        } else if (!Home.forSale(item)) {
            String label = "SPECIAL";
            price.setColor(PixelTheme.SKY);
            c.drawText(label, (w - price.measureText(label)) / 2f, rowY, price);
        } else {
            String label = String.valueOf(item.price);
            float icon = dp(COIN_DP), gap = dp(4);
            float x = (w - icon - gap - price.measureText(label)) / 2f;
            PixelIcons.draw(c, paint, "coin", x, rowY - icon * 0.82f, icon);
            price.setColor(affordable ? PixelTheme.GOLD : PixelTheme.ROSE);
            c.drawText(label, x + icon + gap, rowY, price);
        }
    }
}
