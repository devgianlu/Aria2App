package com.gianlu.aria2app.Activities.MoreAboutDownload.InfoFragment;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.aria2app.Utils;

import java.util.List;

class BitfieldAdapter extends RecyclerView.Adapter<BitfieldAdapter.ViewHolder> {
    private final Context context;
    private final List<Integer> pieces;

    BitfieldAdapter(Context context, List<Integer> pieces) {
        this.context = context;
        this.pieces = pieces;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(new View(context));
    }

    void update(List<Integer> pieces) {
        this.pieces.clear();
        this.pieces.addAll(pieces);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.itemView.setBackgroundColor(Color.argb(Utils.mapAlpha(pieces.get(position)), 255, 87, 34));
    }

    @Override
    public int getItemCount() {
        return pieces.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);

            GridLayoutManager.LayoutParams params = new GridLayoutManager.LayoutParams(30, 30);
            params.setMargins(5, 5, 5, 5);
            itemView.setLayoutParams(params);
        }
    }
}
