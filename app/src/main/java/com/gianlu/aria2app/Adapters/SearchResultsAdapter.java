package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.Search.MissingSearchEngine;
import com.gianlu.aria2app.NetIO.Search.SearchEngine;
import com.gianlu.aria2app.NetIO.Search.SearchResult;
import com.gianlu.aria2app.NetIO.Search.SearchUtils;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.InfiniteRecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SearchResultsAdapter extends InfiniteRecyclerView.InfiniteAdapter<SearchResultsAdapter.ViewHolder, SearchResult> {
    private final LayoutInflater inflater;
    private final SearchUtils searchUtils;
    private final IAdapter listener;
    private final SearchUtils utils;
    private String token;

    public SearchResultsAdapter(Context context, List<SearchResult> results, @Nullable String token, IAdapter listener) {
        super(context, results, -1, -1, false);
        this.inflater = LayoutInflater.from(context);
        this.searchUtils = SearchUtils.get();
        this.token = token;
        this.utils = SearchUtils.get();
        this.listener = listener;
    }

    @Override
    protected void userBindViewHolder(ViewHolder holder, int position) {
        final SearchResult result = items.get(position).getItem();
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
    protected RecyclerView.ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(parent);
    }

    @Override
    protected void moreContent(int page, final IContentProvider<SearchResult> provider) {
        if (token == null) {
            provider.onMoreContent(new ArrayList<SearchResult>());
        } else {
            utils.search(token, SearchUtils.RESULTS_PER_REQUEST, new SearchUtils.ISearch() {
                @Override
                public void onResult(List<SearchResult> results, List<MissingSearchEngine> missingEngines, @Nullable String nextPageToken) {
                    token = nextPageToken;
                    provider.onMoreContent(results);

                    if (listener != null) listener.notifyMissingEngines(missingEngines);
                }

                @Override
                public void onException(Exception ex) {
                    provider.onFailed(ex);
                }
            });
        }
    }

    @Nullable
    @Override
    protected Date getDateFromItem(SearchResult item) {
        return null;
    }

    public interface IAdapter {
        void onResultSelected(SearchResult result);

        void notifyMissingEngines(List<MissingSearchEngine> missingEngines);
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
