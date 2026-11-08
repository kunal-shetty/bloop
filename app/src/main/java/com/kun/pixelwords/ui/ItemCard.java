package com.kun.pixelwords.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.game.Shop;

/** One shop tile: the accessory, its name, and its price or whether you own or wear it. */
public class ItemCard extends PressableView {

    public final Shop.Item item;
    private final Paint paint = new Paint();
    private final Paint name = new Paint();
    private final Paint price = new Paint();
    private final Rect art = new Rect();
    private final Rect dst = new Rect();
    private final Bitmap bitmap;
    private boolean owned, worn, affordable;

    public ItemCard(Context context, Shop.Item item) {
        super(context);
        this.item = item;
        paint.setFilterBitmap(false);
        PixelTheme.styleBody(name, PixelTheme.bodyPx(context, PixelTheme.SIZE_BODY));
        name.setTextAlign(Paint.Align.CENTER);
        PixelTheme.styleDisplay(price, PixelTheme.displayPx(context, PixelTheme.SIZE_CAPTION));
        bitmap = art(context, item);
        if (bitmap != null) findArt();
    }

    /** The accessory's front layer, with its back layer underneath if it has one. */
    private static Bitmap art(Context context, Shop.Item item) {
        Bitmap front = CatView.layer(context, "acc_" + item.key);
        Bitmap behind = CatView.layer(context, "acc_" + item.key + "_back");
        if (front == null || behind == null) return front;
        Bitmap both = Bitmap.createBitmap(front.getWidth(), front.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(both);
        c.drawBitmap(behind, 0, 0, null);
        c.drawBitmap(front, 0, 0, null);
        return both;
    }

    /** The layer is a full 32x32 canvas; this finds the part that has the accessory on it. */
    private void findArt() {
        int minX = 32, minY = 32, maxX = 0, maxY = 0;
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                if ((bitmap.getPixel(x, y) >>> 24) != 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x + 1);
                    maxY = Math.max(maxY, y + 1);
                }
            }
        }
        art.set(minX, minY, maxX, maxY);
    }

    public void update(Progress p) {
        owned = Shop.owns(p, item);
        worn = Shop.isWearing(p, item);
        affordable = Shop.canAfford(p, item);
        setContentDescription(item.title + (worn ? ", wearing" : owned ? ", owned" : ""));
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(resolveSize(0, widthSpec), (int) PixelTheme.dp(getContext(), 136));
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), s = sink();
        int fill = worn ? 0xFF2FA873 : owned ? PixelTheme.PANEL_LIGHT : PixelTheme.PANEL;
        PixelTheme.drawTile(c, paint, 0, s, w, h, fill, px, depth - s);

        float pad = PixelTheme.dp(getContext(), 8);
        float slotTop = s + px + pad * 0.7f, slotH = PixelTheme.dp(getContext(), 66);
        PixelTheme.drawSlot(c, paint, pad, slotTop, w - pad, slotTop + slotH, 0xFF8E7FD0, px);
        if (bitmap != null && !art.isEmpty()) {
            int scale = (int) Math.max(1, Math.min((w - pad * 3) / art.width(), (slotH - pad) / art.height()));
            int dw = art.width() * scale, dh = art.height() * scale;
            int l = (int) (w - dw) / 2, t = (int) (slotTop + (slotH - dh) / 2f);
            dst.set(l, t, l + dw, t + dh);
            c.drawBitmap(bitmap, art, dst, paint);
        }

        float textY = slotTop + slotH + name.getTextSize() * 0.95f;
        name.setColor(PixelTheme.TEXT);
        c.drawText(item.title, w / 2f, textY, name);

        float rowY = h - depth - px - pad * 0.8f;
        if (worn || owned) {
            String label = worn ? "WEARING" : "OWNED";
            price.setColor(worn ? PixelTheme.CREAM : PixelTheme.TEXT_DIM);
            c.drawText(label, (w - price.measureText(label)) / 2f, rowY, price);
        } else if (item.source != Shop.Source.COINS) {
            String label = "SPECIAL";
            price.setColor(PixelTheme.SKY);
            c.drawText(label, (w - price.measureText(label)) / 2f, rowY, price);
        } else {
            String label = String.valueOf(item.price);
            float icon = PixelTheme.dp(getContext(), 18), gap = PixelTheme.dp(getContext(), 5);
            float x = (w - icon - gap - price.measureText(label)) / 2f;
            PixelIcons.draw(c, paint, "coin", x, rowY - icon * 0.82f, icon);
            price.setColor(affordable ? PixelTheme.GOLD : PixelTheme.ROSE);
            c.drawText(label, x + icon + gap, rowY, price);
        }
    }
}
