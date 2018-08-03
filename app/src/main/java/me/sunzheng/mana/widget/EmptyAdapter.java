package me.sunzheng.mana.widget;

import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

/**
 * see https://stackoverflow.com/questions/37462766/swiperefreshlayout-doesnt-refresh-on-swipe-when-list-size-is-zero
 * Created by Sun on 2017/12/1.
 */

public class EmptyAdapter extends RecyclerView.Adapter {

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
