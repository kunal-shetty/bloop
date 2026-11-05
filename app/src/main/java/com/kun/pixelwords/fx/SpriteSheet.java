package com.kun.pixelwords.fx;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A PixelFX effect exported as a sprite sheet (see tools/export-fx.sh): a PNG with all frames in a
 * uniform grid, plus a tiny "&lt;name&gt;.fx.json" with fps, frame count, frame size and column count.
 * Name format: "category_effect", e.g. "ui_confetti".
 */
public final class SpriteSheet {

    public final Bitmap bitmap;
    public final int frames;
    public final int frameW;
    public final int frameH;
    public final int columns;
    public final float fps;
    public final boolean loop;

    private SpriteSheet(Bitmap bitmap, int frames, int frameW, int frameH, int columns, float fps, boolean loop) {
        this.bitmap = bitmap;
        this.frames = frames;
        this.frameW = frameW;
        this.frameH = frameH;
        this.columns = columns;
        this.fps = fps;
        this.loop = loop;
    }

    public static SpriteSheet load(Context context, String name) throws IOException {
        try {
            JSONObject meta = new JSONObject(readAll(context.getAssets().open("fx/" + name + ".fx.json")));
            JSONArray size = meta.getJSONArray("frameSize");

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false; // never resample pixel art
            Bitmap bmp;
            try (InputStream in = context.getAssets().open("fx/" + name + ".png")) {
                bmp = BitmapFactory.decodeStream(in, null, opts);
            }
            if (bmp == null) throw new IOException("Could not decode sprite sheet " + name);

            return new SpriteSheet(bmp, meta.getInt("frames"), size.getInt(0), size.getInt(1),
                    meta.getInt("columns"), (float) meta.getDouble("fps"), meta.getBoolean("loop"));
        } catch (JSONException e) {
            throw new IOException("Bad sprite metadata for " + name, e);
        }
    }

    public long durationMs() {
        return (long) (frames * 1000f / fps);
    }

    /** Source rectangle of frame {@code i} inside the sheet bitmap. */
    public void frameRect(int i, Rect out) {
        int col = i % columns;
        int row = i / columns;
        out.set(col * frameW, row * frameH, (col + 1) * frameW, (row + 1) * frameH);
    }

    private static String readAll(InputStream in) throws IOException {
        try (InputStream is = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }
}
