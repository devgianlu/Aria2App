package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.gianlu.aria2app.Options.Option;
import com.gianlu.aria2app.R;

import java.util.List;

// TODO: OptionsAdapter
public class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.ViewHolder> {
    private final List<Option> options;
    private final LayoutInflater inflater;

    public OptionsAdapter(Context context, List<Option> options) {
        this.options = options;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.option_item, parent, false));
        }
    }
}
