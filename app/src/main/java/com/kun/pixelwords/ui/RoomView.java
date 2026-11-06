package com.kun.pixelwords.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.kun.pixelwords.game.Home;
import com.kun.pixelwords.game.Progress;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** The cat's room, drawn at a whole-number scale. Drag a piece of furniture to move it. */
public class RoomView extends View {

    private static final Map<String, Bitmap> CACHE = new HashMap<>();

    private final Paint paint = new Paint();
    private final Rect dst = new Rect();
    private Progress progress = new Progress();
    private Runnable onMoved;
    private Home.Slot dragging;
    private int grabX, grabY;

    public RoomView(Context context) {
        super(context);
        paint.setFilterBitmap(false);
    }

    public void show(Progress p) {
        progress = p;
        invalidate();
    }

    /** Runs when the player lets go of a piece of furniture. */
    public void setOnMoved(Runnable r) {
        onMoved = r;
    }

    /** Art pixels to screen pixels. */
    public int scale() {
        return Math.max(1, Math.min(getWidth(), getHeight()) / Home.ROOM_SIZE);
    }

    private int originX() {
        return (getWidth() - Home.ROOM_SIZE * scale()) / 2;
    }

    private int originY() {
        return (getHeight() - Home.ROOM_SIZE * scale()) / 2;
    }

    /** Where a spot in the room lands on this view, for placing the cat. */
    public int screenX(int roomX) {
        return originX() + roomX * scale();
    }

    public int screenY(int roomY) {
        return originY() + roomY * scale();
    }

    static Bitmap art(Context c, String name) {
        Bitmap b = CACHE.get(name);
        if (b != null) return b;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        try (InputStream in = c.getAssets().open("home/" + name + ".png")) {
            b = BitmapFactory.decodeStream(in, null, o);
        } catch (IOException e) {
            Log.w("RoomView", "missing art " + name, e);
            return null;
        }
        CACHE.put(name, b);
        return b;
    }

    private void drawAt(Canvas c, String name, int roomX, int roomY) {
        Bitmap b = art(getContext(), name);
        if (b == null) return;
        int x = screenX(roomX), y = screenY(roomY), s = scale();
        dst.set(x, y, x + b.getWidth() * s, y + b.getHeight() * s);
        c.drawBitmap(b, null, dst, paint);
    }

    @Override
    protected void onDraw(Canvas c) {
        drawAt(c, Home.theme(progress).key, 0, 0);
        for (Home.Slot slot : Home.Slot.values()) {
            Home.Item item = Home.placed(progress, slot);
            if (item != null) drawAt(c, item.key, Home.x(progress, slot), Home.y(progress, slot));
        }
    }

    /** The frontmost piece of furniture at a spot, or null. */
    private Home.Slot slotAt(int roomX, int roomY) {
        Home.Slot[] slots = Home.Slot.values();
        for (int i = slots.length - 1; i >= 0; i--) {
            Home.Slot s = slots[i];
            if (Home.placed(progress, s) == null) continue;
            int x = Home.x(progress, s), y = Home.y(progress, s);
            if (roomX >= x && roomX < x + s.width && roomY >= y && roomY < y + s.height) return s;
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int roomX = (int) ((e.getX() - originX()) / scale()), roomY = (int) ((e.getY() - originY()) / scale());
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragging = slotAt(roomX, roomY);
                if (dragging == null) return false;
                grabX = roomX - Home.x(progress, dragging);
                grabY = roomY - Home.y(progress, dragging);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging != null) {
                    Home.move(progress, dragging, roomX - grabX, roomY - grabY);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging != null && onMoved != null) onMoved.run();
                dragging = null;
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }
}
