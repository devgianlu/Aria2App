package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.aria2app.Activities.Search.SearchEngine;
import com.gianlu.aria2app.Activities.Search.SearchResult;
import com.gianlu.aria2app.Activities.Search.SearchUtils;
import com.gianlu.aria2app.R;

import java.util.List;

// TODO: Should become an InfiniteAdapter
public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {
    private final List<SearchResult> results;
    private final LayoutInflater inflater;
    private final SearchUtils searchUtils;
    private final IAdapter listener;

    public SearchResultsAdapter(Context context, List<SearchResult> results, IAdapter listener) {
        this.results = results;
        this.inflater = LayoutInflater.from(context);
        this.searchUtils = SearchUtils.get(context);
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final SearchResult result = results.get(position);
        SearchEngine engine = searchUtils.findEngine(result.engineId);

        holder.name.setText(result.title);
        if (engine != null) holder.engine.setText(engine.name);

        if (result.seeders != null) {
            holder.seeders.setVisibility(View.VISIBLE);
            holder.seeders.setText(String.valueOf(result.seeders));
        } else {
            holder.seeders.setVisibility(View.GONE);
        }

        if (result.leeches != null) {
            holder.leeches.setVisibility(View.VISIBLE);
            holder.leeches.setText(String.valueOf(result.leeches));
        } else {
            holder.leeches.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onResultSelected(result);
            }
        });
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public interface IAdapter {
        void onResultSelected(SearchResult result);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView engine;
        final TextView seeders;
        final TextView leeches;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.search_result_item, parent, false));

            name = itemView.findViewById(R.id.searchResult_name);
            engine = itemView.findViewById(R.id.searchResult_engine);
            seeders = itemView.findViewById(R.id.searchResult_seeders);
            leeches = itemView.findViewById(R.id.searchResult_leeches);
        }
    }
}
