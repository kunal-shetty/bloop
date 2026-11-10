package com.kun.pixelwords.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.List;

/** A row of tabs that shows one section of a scrolling list at a time. */
public class TabBar extends LinearLayout {

    private static final float GAP_DP = 6, TOP_DP = 6, SECTION_TOP_DP = 10;

    private final List<PixelButton> tabs = new ArrayList<>();
    private final List<LinearLayout> sections = new ArrayList<>();
    private ScrollView scroll;
    private java.util.function.IntConsumer onShown;

    /** Called with the tab's index whenever one opens, including the first. */
    public void setOnShown(java.util.function.IntConsumer listener) {
        onShown = listener;
    }

    public TabBar(Context context) {
        super(context);
        setPadding(0, dp(TOP_DP), 0, 0);
    }

    private int dp(float v) {
        return (int) PixelTheme.dp(getContext(), v);
    }

    /** Adds a tab and its section to {@code list}, and returns the section to fill. */
    public LinearLayout add(String label, LinearLayout list) {
        LinearLayout section = new LinearLayout(getContext());
        section.setOrientation(VERTICAL);
        section.setPadding(0, dp(SECTION_TOP_DP), 0, 0);
        list.addView(section);
        sections.add(section);

        PixelButton tab = new PixelButton(getContext(), label, PixelTheme.PANEL, PixelTheme.TEXT);
        tab.setLabelSize(PixelTheme.SIZE_CAPTION);
        int index = tabs.size();
        tab.setOnClickListener(v -> show(index));
        LayoutParams lp = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (index > 0) lp.leftMargin = dp(GAP_DP);
        addView(tab, lp);
        tabs.add(tab);
        return section;
    }

    /** Call once every tab is added. Opens the first one. */
    public void attach(ScrollView scrollView) {
        scroll = scrollView;
        show(0);
    }

    private void show(int index) {
        for (int i = 0; i < tabs.size(); i++) {
            boolean on = i == index;
            sections.get(i).setVisibility(on ? View.VISIBLE : View.GONE);
            tabs.get(i).setFill(on ? PixelTheme.GOLD : PixelTheme.PANEL);
            tabs.get(i).setTextColor(on ? PixelTheme.INK : PixelTheme.TEXT);
        }
        scroll.scrollTo(0, 0);
        if (onShown != null) onShown.accept(index);
    }
}
