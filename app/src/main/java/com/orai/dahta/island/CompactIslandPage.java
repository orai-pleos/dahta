package com.orai.dahta.island;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orai.dahta.res.R;

/** 평소 알약. 대기/도착은 제목만, 그 외에는 "제목 · 보조 문구". */
public final class CompactIslandPage extends IslandPage {
    private final TextView title = find(R.id.island_compact_title);
    private final TextView separator = find(R.id.island_compact_separator);
    private final TextView sub = find(R.id.island_compact_sub);

    public CompactIslandPage(Context context, ViewGroup parent) {
        super(context, parent, R.layout.island_page_compact);
    }

    @Override
    public void bind(IslandPhase phase) {
        Context context = view().getContext();
        String subText = null;
        switch (phase) {
            case WAITING:
                title.setText(R.string.island_waiting_title);
                break;
            case CALL:
                title.setText(R.string.island_call_compact_title);
                subText = context.getString(R.string.island_call_compact_sub);
                break;
            case DRIVING:
                title.setText(R.string.island_compact_title);
                subText = context.getString(R.string.island_eta, 12);
                break;
            case GUIDING:
                title.setText(R.string.island_compact_title);
                subText = context.getString(R.string.island_eta, 3);
                break;
            case ARRIVED:
                title.setText(R.string.island_arrived_compact_title);
                break;
        }
        int visibility = subText != null ? View.VISIBLE : View.GONE;
        sub.setText(subText);
        sub.setVisibility(visibility);
        separator.setVisibility(visibility);
    }
}
