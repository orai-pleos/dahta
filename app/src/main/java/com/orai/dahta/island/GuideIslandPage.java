package com.orai.dahta.island;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orai.dahta.res.R;

/** 안내 순간과 도착의 확장 페이지. 둘은 문구만 다르다. */
public final class GuideIslandPage extends IslandPage {
    private final TextView title = find(R.id.island_title);
    private final TextView desc = find(R.id.island_desc);
    private final TextView caption = find(R.id.island_caption);

    public GuideIslandPage(Context context, ViewGroup parent) {
        super(context, parent, R.layout.island_page_guide);
    }

    @Override
    public void bind(IslandPhase phase) {
        boolean arrived = phase == IslandPhase.ARRIVED;
        title.setText(arrived ? R.string.island_arrived_title : R.string.island_guide_title);
        desc.setText(arrived ? R.string.island_arrived_desc : R.string.island_guide_desc);
        caption.setText(arrived ? R.string.island_arrived_caption : R.string.island_guide_caption);
    }
}
