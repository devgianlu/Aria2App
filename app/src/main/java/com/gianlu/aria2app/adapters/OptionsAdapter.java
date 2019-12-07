package com.gianlu.aria2app.adapters;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.api.aria2.Option;
import com.gianlu.aria2app.api.aria2.OptionsMap;
import com.gianlu.aria2app.options.OptionsManager;
import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.ui.Toaster;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.ViewHolder> {
    private final List<Option> options;
    private final List<Option> originalOptions;
    private final LayoutInflater inflater;
    private final Listener listener;

    private OptionsAdapter(@NonNull Context context, List<Option> options, Listener listener) {
        this.originalOptions = options;
        this.options = new ArrayList<>(options);
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @NonNull
    public static OptionsAdapter setup(@NonNull Context context, @NonNull OptionsMap map, boolean global, boolean quick, boolean quickOnTop, Listener listener) throws IOException, JSONException {
        List<String> all;
        if (global) all = OptionsManager.get(context).loadGlobalOptions();
        else all = OptionsManager.get(context).loadDownloadOptions();

        Set<String> filter;
        if (quick) filter = Prefs.getSet(PK.A2_QUICK_OPTIONS_MIXED, null);
        else filter = null;

        List<Option> options = Option.fromOptionsMap(map, all, filter);
        if (quickOnTop)
            Collections.sort(options, new OptionsManager.IsQuickComparator());

        return new OptionsAdapter(context, options, listener);
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    public void optionChanged(@NonNull Option option) {
        int pos = options.indexOf(option);
        if (pos != -1) {
            options.set(pos, option);
            super.notifyItemChanged(pos);
        }

        int realPos = originalOptions.indexOf(option);
        if (realPos != -1) originalOptions.set(realPos, option);
    }

    public void filter(@Nullable String query) {
        options.clear();
        if (query == null || query.isEmpty()) {
            options.addAll(originalOptions);
            notifyDataSetChanged();
            return;
        }

        for (Option option : originalOptions)
            if (option.name.startsWith(query) || option.name.contains(query))
                options.add(option);

        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Option option = options.get(position);

        holder.name.setText(option.name);

        if (option.isValueChanged()) {
            holder.value.setText(option.newValue.strings("; "));
            CommonUtils.setTextColorFromAttr(holder.value, R.attr.colorSecondary);
        } else {
            holder.value.setText(option.value.strings("; "));
            CommonUtils.setTextColorFromAttr(holder.value, android.R.attr.textColorPrimary);
        }

        if (option.isQuick())
            holder.toggleFavourite.setImageResource(R.drawable.baseline_favorite_24);
        else
            holder.toggleFavourite.setImageResource(R.drawable.baseline_favorite_border_24);

        holder.edit.setOnClickListener(v -> {
            if (listener != null) listener.onEditOption(option);
        });

        holder.toggleFavourite.setOnClickListener(v -> {
            boolean isQuick = !option.isQuick();
            option.setQuick(isQuick);

            int oldIndex = holder.getAdapterPosition();
            notifyItemChanged(oldIndex);

            if (!isQuick) Collections.sort(options);  // Order alphabetically
            Collections.sort(options, new OptionsManager.IsQuickComparator());

            int newIndex = options.indexOf(option);
            if (newIndex != -1) notifyItemMoved(oldIndex, newIndex);
        });

        holder.info.setOnClickListener(v -> {
            try {
                v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://aria2.github.io/manual/en/html/aria2c.html#cmdoption-" + option.name)));
            } catch (ActivityNotFoundException ex) {
                DialogUtils.showToast(v.getContext(), Toaster.build().message(R.string.missingWebBrowser).ex(ex));
            }
       });
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    public List<Option> getOptions() {
        return originalOptions;
    }

    public interface Listener {
        void onEditOption(@NonNull Option option);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView name;
        final SuperTextView value;
        final ImageButton edit;
        final ImageButton toggleFavourite;
        final ImageButton info;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_option, parent, false));

            name = itemView.findViewById(R.id.optionItem_name);
            value = itemView.findViewById(R.id.optionItem_value);
            edit = itemView.findViewById(R.id.optionItem_edit);
            toggleFavourite = itemView.findViewById(R.id.optionItem_toggleFavourite);
            info = itemView.findViewById(R.id.optionItem_info);
        }
    }
}
