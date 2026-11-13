package com.orai.dahta.island;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;

/**
 * 섬의 한 "페이지". 오버레이는 서비스 안의 창이라 Fragment 를 쓸 수 없어서,
 * 페이지마다 레이아웃과 클래스를 따로 두고 서비스가 하나씩 갈아 끼운다.
 */
public abstract class IslandPage {
    private final View view;

    /**
     * @param parent 페이지가 들어갈 틀. 레이아웃의 layout_width/height 가 적용되도록
     *               attachToRoot 없이 이 틀을 부모로 하여 inflate 한다.
     */
    protected IslandPage(Context context, ViewGroup parent, @LayoutRes int layout) {
        view = LayoutInflater.from(context).inflate(layout, parent, false);
    }

    /** 이 페이지의 화면. */
    public final View view() {
        return view;
    }

    protected final <T extends View> T find(@IdRes int id) {
        return view.findViewById(id);
    }

    /** 단계에 맞는 문구로 내용을 갱신한다. */
    public abstract void bind(IslandPhase phase);
}
