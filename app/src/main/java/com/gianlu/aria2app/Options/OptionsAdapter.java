package com.gianlu.aria2app.Options;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.ViewHolder> implements Filterable {
    private final Context context;
    private final View.OnClickListener helpClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://aria2.github.io/manual/en/html/aria2c.html#cmdoption--" + view.getTag())));
        }
    };
    private final List<Option> originalObjs;
    private final boolean quickOptionsFilter;
    private final boolean hideHearts;
    private final boolean hideUseMe;
    private final OptionFilter filter;
    private List<Option> objs;

    public OptionsAdapter(Context context, List<Option> objs, boolean quickOptionsFilter, boolean hideHearts, boolean hideUseMe) {
        this.context = context;
        this.originalObjs = objs;
        this.objs = objs;
        this.quickOptionsFilter = quickOptionsFilter;
        this.hideHearts = hideHearts;
        this.hideUseMe = hideUseMe;
        this.filter = new OptionFilter();
    }

    public List<Option> getOptions() {
        return originalObjs;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.option_item, parent, false));
    }

    @Nullable
    private Option getRealItem(String name) {
        for (Option option : originalObjs) {
            if (Objects.equals(option.longName, name))
                return option;
        }

        return null;
    }

    private Option getItem(int pos) {
        return objs.get(pos);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Option item = getRealItem(getItem(position).longName);
        if (item == null)
            return;

        holder.help.setTag(item.longName);
        holder.help.setOnClickListener(helpClick);

        holder.longName.setText(item.longName);
        holder.currValue.setText(item.value);

        holder.newValue.setText(item.value);
        holder.newValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                item.newValue = editable.toString();
            }
        });

        holder.expand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonUtils.animateCollapsingArrowBellows(holder.expand, CommonUtils.isExpanded(holder.edit));

                if (CommonUtils.isExpanded(holder.edit))
                    CommonUtils.collapse(holder.edit);
                else
                    CommonUtils.expand(holder.edit);
            }
        });

        if (quickOptionsFilter || hideHearts) {
            holder.toggleQuick.setVisibility(View.GONE);
        } else {
            holder.toggleQuick.setVisibility(View.VISIBLE);
            holder.toggleQuick.setFocusable(false);

            holder.toggleQuick.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                    Set<String> quickOptions = preferences.getStringSet("a2_quickOptions", new HashSet<String>());

                    item.isQuick = !item.isQuick;
                    if (item.isQuick) {
                        holder.toggleQuick.setImageResource(R.drawable.ic_favorite_black_48dp);
                        quickOptions.add(item.longName);
                    } else {
                        holder.toggleQuick.setImageResource(R.drawable.ic_favorite_border_black_48dp);
                        quickOptions.remove(item.longName);
                    }

                    preferences.edit().putStringSet("a2_quickOptions", quickOptions).apply();
                }
            });

            if (item.isQuick)
                holder.toggleQuick.setImageResource(R.drawable.ic_favorite_black_48dp);
            else
                holder.toggleQuick.setImageResource(R.drawable.ic_favorite_border_black_48dp);
        }

        if (hideUseMe) {
            holder.toggleUse.setVisibility(View.GONE);
        } else {
            holder.toggleUse.setVisibility(View.VISIBLE);
            holder.toggleUse.setFocusable(false);

            holder.toggleUse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    item.useMe = !item.useMe;
                    if (item.useMe) {
                        holder.toggleUse.setImageResource(R.drawable.ic_label_black_48dp);
                    } else {
                        holder.toggleUse.setImageResource(R.drawable.ic_label_outline_black_48dp);
                    }
                }
            });

            if (item.useMe)
                holder.toggleUse.setImageResource(R.drawable.ic_label_black_48dp);
            else
                holder.toggleUse.setImageResource(R.drawable.ic_label_outline_black_48dp);
        }
    }

    @Override
    public int getItemCount() {
        return objs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView longName;
        final TextView currValue;
        final ImageButton expand;
        final EditText newValue;
        final LinearLayout edit;
        final ImageButton toggleUse;
        final ImageButton toggleQuick;
        final ImageButton help;

        ViewHolder(View rootView) {
            super(rootView);

            longName = (TextView) rootView.findViewById(R.id.optionItem_longName);
            currValue = (TextView) rootView.findViewById(R.id.optionItem_value);
            expand = (ImageButton) rootView.findViewById(R.id.optionItem_expand);
            newValue = (EditText) rootView.findViewById(R.id.optionItem_newValue);
            edit = (LinearLayout) rootView.findViewById(R.id.optionItem_edit);
            toggleQuick = (ImageButton) rootView.findViewById(R.id.optionItem_toggleQuick);
            toggleUse = (ImageButton) rootView.findViewById(R.id.optionItem_toggleUse);
            help = (ImageButton) rootView.findViewById(R.id.optionItem_help);
        }
    }

    private class OptionFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null || constraint.length() == 0) {
                results.values = originalObjs;
                results.count = originalObjs.size();
            } else {
                List<Option> filtered = new ArrayList<>();

                for (Option option : originalObjs) {
                    if (option.longName.toLowerCase().startsWith(constraint.toString().toLowerCase())) {
                        filtered.add(option);
                    }
                }

                if (constraint.length() >= 1) {
                    for (Option option : originalObjs) {
                        if (!filtered.contains(option) && option.longName.toLowerCase().contains(constraint.toString().toLowerCase())) {
                            filtered.add(option);
                        }
                    }
                }

                results.values = filtered;
                results.count = filtered.size();
            }
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            if (filterResults.count == 0) {
                objs.clear();
                notifyDataSetChanged();
            } else {
                objs = (List<Option>) filterResults.values;
                notifyDataSetChanged();
            }
        }
    }
}
