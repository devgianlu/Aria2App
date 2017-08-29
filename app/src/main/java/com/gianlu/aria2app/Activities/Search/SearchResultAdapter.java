package com.gianlu.aria2app.Activities.Search;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.aria2app.R;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    private final List<SearchResult> results;
    private final LayoutInflater inflater;
    private final IAdapter handler;

    public SearchResultAdapter(Context context, List<SearchResult> results, IAdapter handler) {
        this.results = results;
        this.inflater = LayoutInflater.from(context);
        this.handler = handler;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.search_result_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final SearchResult item = results.get(position);

        holder.name.setText(item.name);
        holder.seeders.setText(String.valueOf(item.seeders));
        holder.leeches.setText(String.valueOf(item.leeches));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null)
                    handler.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public interface IAdapter {
        void onItemClick(SearchResult which);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView name;
        public final TextView seeders;
        public final TextView leeches;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.searchResult_name);
            seeders = itemView.findViewById(R.id.searchResult_seeders);
            leeches = itemView.findViewById(R.id.searchResult_leeches);
        }
    }
}
