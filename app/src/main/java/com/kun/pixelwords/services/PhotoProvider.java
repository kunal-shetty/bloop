package com.kun.pixelwords.services;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Hands a photo of the cat's home to the app the player shares it with. The picture lives in the cache
 * and is replaced by the next one, so nothing is left behind in the gallery and no storage permission is
 * needed. Only that one file can be read, and only by apps the share grants it to.
 */
public class PhotoProvider extends ContentProvider {

    private static final String DIR = "photos";
    private static final String NAME = "cat_home.png";

    /** Saves the photo and returns the address to share it by. */
    public static Uri save(Context c, Bitmap photo) throws IOException {
        File dir = new File(c.getCacheDir(), DIR);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("no room for the photo");
        try (FileOutputStream out = new FileOutputStream(new File(dir, NAME))) {
            photo.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        return Uri.parse("content://" + c.getPackageName() + ".photos/" + NAME);
    }

    private File file(Uri uri) throws FileNotFoundException {
        if (!NAME.equals(uri.getLastPathSegment())) throw new FileNotFoundException(uri.toString());
        File f = new File(new File(getContext().getCacheDir(), DIR), NAME);
        if (!f.isFile()) throw new FileNotFoundException(uri.toString());
        return f;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("read only");
        return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    /** Some apps ask for the name and size before they read the picture. */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        MatrixCursor cursor = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        try {
            cursor.addRow(new Object[]{NAME, file(uri).length()});
        } catch (FileNotFoundException e) {
            // an empty answer: the photo has gone
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "image/png";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        throw new UnsupportedOperationException();
    }
}
