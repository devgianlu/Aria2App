package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.gianlu.aria2app.Options.Option;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.SuperTextView;

import java.util.List;

// TODO: Search
public class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.ViewHolder> {
    private final List<Option> options;
    private final LayoutInflater inflater;
    private final Context context;
    private IAdapter handler;

    public OptionsAdapter(Context context, List<Option> options) {
        this.context = context;
        this.options = options;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    public void setHandler(IAdapter handler) {
        this.handler = handler;
    }

    public void notifyItemChanged(Option option) {
        int pos = options.indexOf(option);
        if (pos != -1) {
            options.set(pos, option);
            super.notifyItemChanged(pos);
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Option option = options.get(position);

        holder.name.setText(option.name);

        if (option.isValueChanged()) {
            holder.value.setText(option.newValue);
            holder.value.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
        } else {
            holder.value.setText(option.value);
            holder.value.setTextColor(ContextCompat.getColor(context, android.R.color.secondary_text_light));
        }

        if (option.isQuick(context))
            holder.toggleFavourite.setImageResource(R.drawable.ic_favorite_black_48dp);
        else
            holder.toggleFavourite.setImageResource(R.drawable.ic_favorite_border_black_48dp);

        holder.edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null) handler.onEditOption(option);
            }
        });

        holder.toggleFavourite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                option.setQuick(context, !option.isQuick(context));
                notifyItemChanged(holder.getAdapterPosition());
            }
        });

        holder.info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://aria2.github.io/manual/en/html/aria2c.html#cmdoption--" + option.name)));
            }
        });
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    public List<Option> getOptions() {
        return options;
    }

    public interface IAdapter {
        void onEditOption(Option option);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView name;
        final SuperTextView value;
        final ImageButton edit;
        final ImageButton toggleFavourite;
        final ImageButton info;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.option_item, parent, false));

            name = (SuperTextView) itemView.findViewById(R.id.optionItem_name);
            value = (SuperTextView) itemView.findViewById(R.id.optionItem_value);
            edit = (ImageButton) itemView.findViewById(R.id.optionItem_edit);
            toggleFavourite = (ImageButton) itemView.findViewById(R.id.optionItem_toggleFavourite);
            info = (ImageButton) itemView.findViewById(R.id.optionItem_info);
        }
    }
}
