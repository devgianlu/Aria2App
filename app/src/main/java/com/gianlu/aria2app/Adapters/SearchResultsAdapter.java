package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.NetIO.Search.MissingSearchEngine;
import com.gianlu.aria2app.NetIO.Search.SearchApi;
import com.gianlu.aria2app.NetIO.Search.SearchEngine;
import com.gianlu.aria2app.NetIO.Search.SearchResult;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CasualViews.InfiniteRecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SearchResultsAdapter extends InfiniteRecyclerView.InfiniteAdapter<SearchResultsAdapter.ViewHolder, SearchResult> {
    private final LayoutInflater inflater;
    private final Listener listener;
    private final SearchApi searchApi;
    private String token;

    public SearchResultsAdapter(Context context, List<SearchResult> results, @Nullable String token, Listener listener) {
        super(context, new Config<SearchResult>().noSeparators().undeterminedPages().items(results));
        this.inflater = LayoutInflater.from(context);
        this.searchApi = SearchApi.get();
        this.token = token;
        this.listener = listener;
    }

    @Override
    protected void userBindViewHolder(@NonNull ViewHolder holder, @NonNull ItemEnclosure<SearchResult> item, int position) {
        final SearchResult result = item.getItem();
        SearchEngine engine = searchApi.findEngine(result.engineId);

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

        holder.itemView.setOnClickListener(view -> {
            if (listener != null) listener.onResultSelected(result);
        });
    }

    @NonNull
    @Override
    protected RecyclerView.ViewHolder createViewHolder(@NonNull ViewGroup parent) {
        return new ViewHolder(parent);
    }

    @Override
    protected void moreContent(int page, @NonNull ContentProvider<SearchResult> provider) {
        if (token == null) {
            provider.onMoreContent(new ArrayList<>());
        } else {
            searchApi.search(token, SearchApi.RESULTS_PER_REQUEST, null, new SearchApi.OnSearch() {
                @Override
                public void onResult(List<SearchResult> results, List<MissingSearchEngine> missingEngines, @Nullable String nextPageToken) {
                    token = nextPageToken;
                    provider.onMoreContent(results);

                    if (listener != null) listener.notifyMissingEngines(missingEngines);
                }

                @Override
                public void onException(@NonNull Exception ex) {
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

    public interface Listener {
        void onResultSelected(@NonNull SearchResult result);

        void notifyMissingEngines(@NonNull List<MissingSearchEngine> missingEngines);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView engine;
        final TextView seeders;
        final TextView leeches;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_search_result, parent, false));

            name = itemView.findViewById(R.id.searchResult_name);
            engine = itemView.findViewById(R.id.searchResult_engine);
            seeders = itemView.findViewById(R.id.searchResult_seeders);
            leeches = itemView.findViewById(R.id.searchResult_leeches);
        }
    }
}
