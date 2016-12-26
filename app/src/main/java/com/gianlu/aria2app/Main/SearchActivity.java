package com.gianlu.aria2app.Main;

import android.app.SearchManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.gianlu.aria2app.Main.Search.SearchResult;
import com.gianlu.aria2app.Main.Search.SearchResultAdapter;
import com.gianlu.aria2app.Main.Search.SearchUtils;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;

import java.util.List;

public class SearchActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private TextView noData;
    private RecyclerView list;
    private LinearLayout banner;
    private ProgressBar loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        setTitle(R.string.searchTorrent);

        TextView siteDescription = (TextView) findViewById(R.id.search_siteDescription);
        siteDescription.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf"));
        banner = (LinearLayout) findViewById(R.id.search_siteBanner);
        loading = (ProgressBar) findViewById(R.id.search_loading);
        list = (RecyclerView) findViewById(R.id.search_list);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        noData = (TextView) findViewById(R.id.search_noData);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.search_search).getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        loading.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
        noData.setVisibility(View.GONE);
        banner.setVisibility(View.GONE);

        SearchUtils.search(query, 1, new SearchUtils.ISearch() {
            @Override
            public void onResults(final List<SearchResult> results) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loading.setVisibility(View.GONE);

                        if (results.isEmpty()) {
                            noData.setVisibility(View.VISIBLE);
                            list.setVisibility(View.GONE);
                        } else {
                            noData.setVisibility(View.GONE);
                            list.setVisibility(View.VISIBLE);

                            list.setAdapter(new SearchResultAdapter(SearchActivity.this, results, new SearchResultAdapter.IAdapter() {
                                @Override
                                public void onItemClick(SearchResult which) {
                                    // TODO: Item selected
                                }
                            }));
                        }
                    }
                });
            }

            @Override
            public void onException(Exception ex) {
                CommonUtils.UIToast(SearchActivity.this, Utils.ToastMessages.SEARCH_FAILED, ex, new Runnable() {
                    @Override
                    public void run() {
                        loading.setVisibility(View.GONE);
                        noData.setVisibility(View.VISIBLE);
                        list.setVisibility(View.GONE);
                    }
                });
            }
        });
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return true;
    }
}
