package com.kun.pixelwords;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.PanelDrawable;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelTheme;

import java.util.List;

/**
 * One long page for the credits, privacy policy, terms or refund policy: a table of contents that jumps
 * to each section, the sections, then links to the other pages and the Discord server.
 */
public class LegalActivity extends BaseActivity {

    static final String EXTRA_KIND = "kind";

    private ScrollView scroll;
    private LegalText.Kind kind;

    static Intent open(android.content.Context context, LegalText.Kind kind) {
        return new Intent(context, LegalActivity.class).putExtra(EXTRA_KIND, kind.name());
    }

    @Override
    protected View createBackground() {
        AmbientView ambient = new AmbientView(this);
        ambient.setTheme(AmbientView.Theme.FIREFLIES);
        return ambient;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String name = getIntent().getStringExtra(EXTRA_KIND);
        kind = name == null ? LegalText.Kind.CREDITS : LegalText.Kind.valueOf(name);
        buildUi();
    }

    private int dp(float v) {
        return (int) PixelTheme.dp(this, v);
    }

    private void buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(12), dp(14), 0);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        PixelButton back = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        back.setIcon("back");
        back.setSpokenName("Back");
        back.setIconSize(24);
        back.setClickSound(Sfx.Id.UI_BACK);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = PixelTheme.display(this, kind.title, PixelTheme.SIZE_TITLE, PixelTheme.GOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(new View(this), new LinearLayout.LayoutParams(dp(46), dp(46))); // balances the back button
        page.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        scroll = new ScrollView(this);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(8), 0, dp(24));

        List<LegalText.Section> sections = LegalText.sections(kind);
        View[] anchors = new View[sections.size()];

        list.addView(contents(sections, anchors), matchWrap(0));
        for (int i = 0; i < sections.size(); i++) {
            LegalText.Section section = sections.get(i);
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setBackground(new PanelDrawable(0xE0251D4A, Math.max(2f, dp(1))));
            box.setPadding(dp(12), dp(10), dp(12), dp(12));
            box.addView(PixelTheme.display(this, (i + 1) + ". " + section.heading, PixelTheme.SIZE_SMALL, PixelTheme.GOLD));
            for (String paragraph : section.paragraphs) {
                TextView text = PixelTheme.body(this, paragraph, PixelTheme.SIZE_BODY, PixelTheme.TEXT);
                text.setPadding(0, dp(8), 0, 0);
                text.setLineSpacing(dp(2), 1f);
                box.addView(text);
            }
            PixelButton top = new PixelButton(this, "BACK TO CONTENTS", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
            top.setLabelSize(PixelTheme.SIZE_CAPTION);
            top.setOnClickListener(v -> scroll.smoothScrollTo(0, 0));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tp.topMargin = dp(10);
            box.addView(top, tp);
            anchors[i] = box;
            list.addView(box, matchWrap(10));
        }

        list.addView(footer(), matchWrap(14));
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        contentLayer.addView(page, fill());
    }

    private View contents(List<LegalText.Section> sections, View[] anchors) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(new PanelDrawable(0xE03A2E6E, Math.max(2f, dp(1))));
        box.setPadding(dp(12), dp(10), dp(12), dp(12));
        box.addView(PixelTheme.display(this, "CONTENTS", PixelTheme.SIZE_SMALL, PixelTheme.CREAM));
        TextView note = PixelTheme.body(this, LegalText.APP + "  -  tap a line to jump to it.", PixelTheme.SIZE_BODY, PixelTheme.TEXT_DIM);
        note.setPadding(0, dp(4), 0, dp(4));
        box.addView(note);
        for (int i = 0; i < sections.size(); i++) {
            final int index = i;
            TextView line = PixelTheme.body(this, (i + 1) + ".  " + sections.get(i).heading, PixelTheme.SIZE_BODY, PixelTheme.SKY);
            line.setPadding(0, dp(7), 0, dp(7));
            line.setOnClickListener(v -> {
                Sfx.play(Sfx.Id.UI_TAP);
                scroll.smoothScrollTo(0, anchors[index].getTop() - dp(6));
            });
            box.addView(line);
        }
        return box;
    }

    /** The other pages, and the Discord server once there is one. */
    private View footer() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(new PanelDrawable(0xE03A2E6E, Math.max(2f, dp(1))));
        box.setPadding(dp(12), dp(10), dp(12), dp(12));
        box.addView(PixelTheme.display(this, "MORE", PixelTheme.SIZE_SMALL, PixelTheme.CREAM));
        for (LegalText.Kind other : LegalText.Kind.values()) {
            if (other == kind) continue;
            PixelButton b = new PixelButton(this, other.title, PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
            b.setLabelSize(PixelTheme.SIZE_LABEL);
            b.setOnClickListener(v -> {
                startActivity(open(this, other));
                finish();
            });
            box.addView(b, matchWrap(8));
        }
        if (Links.hasDiscord()) {
            PixelButton discord = new PixelButton(this, "JOIN OUR DISCORD", PixelTheme.SKY, PixelTheme.INK);
            discord.setLabelSize(PixelTheme.SIZE_LABEL);
            discord.setOnClickListener(v -> openDiscord(this));
            box.addView(discord, matchWrap(8));
        }
        TextView version = PixelTheme.body(this, LegalText.APP + "  v" + MainActivity.versionName(this), PixelTheme.SIZE_BODY, PixelTheme.TEXT_DIM);
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, dp(12), 0, 0);
        box.addView(version);
        return box;
    }

    static void openDiscord(android.content.Context context) {
        if (!Links.hasDiscord()) return;
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Links.DISCORD_URL)));
        } catch (android.content.ActivityNotFoundException ignored) {
            // no browser to open it with
        }
    }

    private LinearLayout.LayoutParams matchWrap(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        return p;
    }
}
