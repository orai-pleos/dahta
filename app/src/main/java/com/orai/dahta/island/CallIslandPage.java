package com.orai.dahta.island;

import android.content.Context;
import android.view.ViewGroup;

import com.orai.dahta.res.R;

/** 호출 도착 카드. [모시러 가기] / [나중에] 버튼을 가진다. */
public final class CallIslandPage extends IslandPage {

    public CallIslandPage(Context context, ViewGroup parent, Runnable onAccept, Runnable onLater) {
        super(context, parent, R.layout.island_page_call);
        find(R.id.island_call_accept).setOnClickListener(button -> onAccept.run());
        find(R.id.island_call_later).setOnClickListener(button -> onLater.run());
    }

    @Override
    public void bind(IslandPhase phase) {
        // 문구가 고정이라 단계에 따라 바꿀 내용이 없다. (호출 정보가 실제 데이터가 되면 여기서 채운다.)
    }
}
