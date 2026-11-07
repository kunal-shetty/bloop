package com.kun.pixelwords;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.game.Album;
import com.kun.pixelwords.game.Reward;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.PanelDrawable;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The sticker album: a banner and a grid of nine words for each page. */
public class AlbumActivity extends PageActivity {

    private static final int STICKERS_PER_ROW = 3;

    private interface PageRow {
        void refresh();
    }

    private final List<PageRow> rows = new ArrayList<>();

    @Override
    protected String pageTitle() {
        return "STICKERS";
    }

    @Override
    protected AmbientView.Theme pageTheme() {
        return AmbientView.Theme.PETALS;
    }

    @Override
    protected void fillPage(LinearLayout list) {
        list.addView(PixelTheme.body(this, "Find these words in any puzzle to collect them. Finish a page for a prize.",
                16, PixelTheme.TEXT_DIM), matchWrap(8));
        for (Album.Page page : Album.Page.values()) addPage(list, page);
    }

    private void addPage(LinearLayout list, Album.Page page) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(new PanelDrawable(0xE03A2E6E, Math.max(2f, dp(1))));
        panel.setPadding(dp(10), dp(10), dp(10), dp(12));

        ImageView banner = banner(page);
        if (banner != null) {
            int width = getResources().getDisplayMetrics().widthPixels - dp(48);
            panel.addView(banner, new LinearLayout.LayoutParams(width, width * 28 / 96));
        }
        TextView title = PixelTheme.display(this, "", PixelTheme.SIZE_SMALL, PixelTheme.CREAM);
        title.setPadding(0, dp(8), 0, dp(4));
        panel.addView(title);

        List<TextView> stickers = new ArrayList<>();
        for (int i = 0; i < page.words.length; i += STICKERS_PER_ROW) {
            LinearLayout row = new LinearLayout(this);
            for (int c = 0; c < STICKERS_PER_ROW; c++) {
                TextView sticker = PixelTheme.body(this, "", PixelTheme.SIZE_BODY, PixelTheme.TEXT);
                sticker.setGravity(Gravity.CENTER);
                sticker.setPadding(0, dp(6), 0, dp(8));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                if (c > 0) lp.leftMargin = dp(6);
                stickers.add(sticker);
                row.addView(sticker, lp);
            }
            panel.addView(row, matchWrap(6));
        }

        Reward prize = Album.pagePrize(page);
        PixelButton claim = new PixelButton(this, "CLAIM PRIZE", PixelTheme.GOLD, PixelTheme.INK);
        claim.setLabelSize(PixelTheme.SIZE_SMALL);
        claim.setIcon(prize.icon());
        claim.setSubtitle("150 coins and " + prize.label());
        claim.setShine(true);
        claim.setOnClickListener(v -> claim(page));
        panel.addView(claim, matchWrap(8));
        list.addView(panel, matchWrap(10));

        rows.add(() -> {
            int have = Album.collected(progress, page);
            title.setText(page.title.toUpperCase(Locale.ROOT) + "  " + have + "/" + Album.WORDS_PER_PAGE
                    + (Album.isClaimed(progress, page) ? "  DONE" : ""));
            for (int i = 0; i < stickers.size(); i++) {
                boolean found = Album.has(progress, page, i);
                TextView s = stickers.get(i);
                s.setText(found ? page.words[i].toUpperCase(Locale.ROOT) : "?");
                s.setTextColor(found ? PixelTheme.INK : PixelTheme.TEXT_DIM);
                s.setBackground(new PanelDrawable(found ? PixelTheme.CREAM : 0xE0251D4A, Math.max(2f, dp(1))));
            }
            claim.setVisibility(Album.isClaimable(progress, page) ? View.VISIBLE : View.GONE);
        });
    }

    private ImageView banner(Album.Page page) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        Bitmap art;
        try (InputStream in = getAssets().open("album/" + page.name().toLowerCase(Locale.ROOT) + ".png")) {
            art = BitmapFactory.decodeStream(in, null, o);
        } catch (IOException e) {
            return null;
        }
        BitmapDrawable drawable = new BitmapDrawable(getResources(), art);
        drawable.setFilterBitmap(false); // keeps the pixels square when it's scaled up
        ImageView view = new ImageView(this);
        view.setImageDrawable(drawable);
        view.setScaleType(ImageView.ScaleType.FIT_XY);
        return view;
    }

    @Override
    protected void refreshPage() {
        for (PageRow r : rows) r.refresh();
    }

    private void claim(Album.Page page) {
        Reward paid = Album.claim(progress, page);
        if (paid == null) {
            Sfx.play(Sfx.Id.UI_TAP);
            return;
        }
        save();
        coins.animateTo(progress.coins);
        celebrate();
        refreshPage();
        new PixelModal(this).title("PAGE COMPLETE!").icon("sticker").accent(PixelTheme.GOLD)
                .stat("coin", "Coins", "+150", PixelTheme.GOLD)
                .stat(paid.icon(), paid.label(), "", PixelTheme.MINT)
                .primary("NICE", null)
                .show();
    }
}
