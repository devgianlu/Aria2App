package com.gianlu.aria2app.Activities;

import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SearchView;

import com.gianlu.aria2app.Adapters.SearchResultsAdapter;
import com.gianlu.aria2app.Main.MainActivity;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.NetIO.Search.MissingSearchEngine;
import com.gianlu.aria2app.NetIO.Search.SearchApi;
import com.gianlu.aria2app.NetIO.Search.SearchEngine;
import com.gianlu.aria2app.NetIO.Search.SearchResult;
import com.gianlu.aria2app.NetIO.Search.Torrent;
import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;

import org.json.JSONException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SearchActivity extends ActivityWithDialog implements SearchView.OnQueryTextListener, SearchView.OnCloseListener, SearchApi.OnSearch, SearchResultsAdapter.Listener, MenuItem.OnActionExpandListener {
    private RecyclerViewLayout recyclerViewLayout;
    private LinearLayout message;
    private String query = null;
    private SearchView searchView;
    private SearchApi searchApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        setTitle(R.string.searchTorrent);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        message = findViewById(R.id.search_message);
        recyclerViewLayout = findViewById(R.id.search_recyclerViewLayout);
        recyclerViewLayout.stopLoading();
        recyclerViewLayout.disableSwipeRefresh();
        recyclerViewLayout.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerViewLayout.getList().addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        Button messageMore = findViewById(R.id.search_messageMore);
        messageMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://gianlu.xyz/projects/TorrentSearchEngine")));
            }
        });

        searchApi = SearchApi.get();
    }

    private void showEnginesDialog(final List<SearchEngine> engines) {
        CharSequence[] enginesNames = new CharSequence[engines.size()];

        for (int i = 0; i < engines.size(); i++) {
            SearchEngine engine = engines.get(i);
            enginesNames[i] = engine.name + (engine.proxyed ? " (proxyed)" : "");
        }

        final boolean[] checkedEngines = new boolean[engines.size()];
        Set<String> checkedEnginesSet = Prefs.getSet(PK.A2_SEARCH_ENGINES, null);

        if (checkedEnginesSet == null) {
            for (int i = 0; i < checkedEngines.length; i++) checkedEngines[i] = true;
        } else {
            for (String checkedEngine : checkedEnginesSet)
                for (int i = 0; i < engines.size(); i++)
                    if (Objects.equals(engines.get(i).id, checkedEngine))
                        checkedEngines[i] = true;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.searchEngines)
                .setMultiChoiceItems(enginesNames, checkedEngines, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checkedEngines[which] = isChecked;
                    }
                })
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Set<String> set = new HashSet<>();
                        for (int i = 0; i < checkedEngines.length; i++)
                            if (checkedEngines[i]) set.add(engines.get(i).id);

                        if (set.isEmpty()) {
                            Toaster.with(SearchActivity.this).message(R.string.noEnginesSelected).show();
                        } else {
                            Prefs.putSet(SearchActivity.this, PK.A2_SEARCH_ENGINES, set);
                            if (query != null) onQueryTextSubmit(query);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        showDialog(builder);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.search_search);
        searchItem.setOnActionExpandListener(this);

        searchView = (SearchView) searchItem.getActionView();
        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);
            searchView.setOnCloseListener(this);
            searchView.setOnQueryTextListener(this);
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(final String query) {
        message.setVisibility(View.GONE);
        recyclerViewLayout.startLoading();
        this.query = query;

        searchApi.search(query.trim(), SearchApi.RESULTS_PER_REQUEST, Prefs.getSet(PK.A2_SEARCH_ENGINES, null), this);

        AnalyticsApplication.sendAnalytics(SearchActivity.this, Utils.ACTION_SEARCH);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onClose() {
        message.setVisibility(View.VISIBLE);
        recyclerViewLayout.stopLoading();
        recyclerViewLayout.hideList();
        recyclerViewLayout.hideMessage();
        searchView.setQuery(null, false);
        this.query = null;
        return false;
    }

    @Override
    public void onResult(List<SearchResult> results, List<MissingSearchEngine> missingEngines, @Nullable String nextPageToken) {
        message.setVisibility(View.GONE);

        recyclerViewLayout.loadListData(new SearchResultsAdapter(this, results, nextPageToken, this));
        notifyMissingEngines(missingEngines);
    }

    @Override
    public void onException(@NonNull Exception ex) {
        message.setVisibility(View.GONE);
        recyclerViewLayout.showError(R.string.searchEngine_offline);
        Logging.log(ex);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_engines:
                showDialog(DialogUtils.progressDialog(this, R.string.gathering_information));
                searchApi.listSearchEngines(new SearchApi.OnResult<List<SearchEngine>>() {
                    @Override
                    public void onResult(@NonNull List<SearchEngine> result) {
                        dismissDialog();
                        showEnginesDialog(result);
                    }

                    @Override
                    public void onException(@NonNull Exception ex) {
                        dismissDialog();
                        Toaster.with(SearchActivity.this).message(R.string.failedLoading).ex(ex).show();
                    }
                });
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResultSelected(@NonNull SearchResult result) {
        showDialog(DialogUtils.progressDialog(this, R.string.gathering_information));
        searchApi.getTorrent(result, new SearchApi.OnResult<Torrent>() {
            @Override
            public void onResult(@NonNull Torrent torrent) {
                dismissDialog();
                showTorrentDialog(torrent);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                dismissDialog();
                Toaster.with(SearchActivity.this).message(R.string.failedLoading).ex(ex).show();
            }
        });
    }

    @Override
    public void notifyMissingEngines(@NonNull final List<MissingSearchEngine> missingEngines) {
        if (missingEngines.isEmpty()) return;

        Snackbar.make(recyclerViewLayout, R.string.missingEngines_message, Snackbar.LENGTH_LONG)
                .setAction(R.string.show, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showMissingEnginesDialog(missingEngines);
                    }
                }).show();
    }

    private void showMissingEnginesDialog(List<MissingSearchEngine> missingEngines) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.missingEngines)
                .setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, missingEngines), null)
                .setPositiveButton(android.R.string.ok, null);

        showDialog(builder);
    }

    private void showTorrentDialog(final Torrent torrent) {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_torrent, null, false);
        SuperTextView engine = layout.findViewById(R.id.torrentDialog_engine);
        SuperTextView size = layout.findViewById(R.id.torrentDialog_size);
        SuperTextView seeders = layout.findViewById(R.id.torrentDialog_seeders);
        SuperTextView leeches = layout.findViewById(R.id.torrentDialog_leeches);

        SearchEngine searchEngine = searchApi.findEngine(torrent.engineId);
        if (searchEngine != null) engine.setHtml(R.string.searchEngine, searchEngine.name);
        else engine.setVisibility(View.GONE);
        size.setHtml(R.string.size, CommonUtils.dimensionFormatter(torrent.size, false));
        seeders.setHtml(R.string.numSeederShort, torrent.seeders);
        leeches.setHtml(R.string.numLeechesShort, torrent.leeches);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(torrent.title)
                .setView(layout)
                .setNegativeButton(R.string.getMagnet, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, torrent.magnet);
                        sendIntent.setType("text/plain");
                        startActivity(sendIntent);

                        AnalyticsApplication.sendAnalytics(SearchActivity.this, Utils.ACTION_SEARCH_GET_MAGNET);
                    }
                })
                .setPositiveButton(R.string.download, null);

        if (torrent.torrentFileUrl != null) {
            builder.setNeutralButton(R.string.getTorrent, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(torrent.torrentFileUrl)));

                    AnalyticsApplication.sendAnalytics(SearchActivity.this, Utils.ACTION_SEARCH_GET_TORRENT);
                }
            });
        }

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            Aria2Helper.instantiate(SearchActivity.this)
                                    .request(AriaRequests.addUri(Collections.singletonList(torrent.magnet), null, null), new AbstractClient.OnResult<String>() {
                                        @Override
                                        public void onResult(@NonNull String result) {
                                            dialogInterface.dismiss();

                                            Snackbar.make(recyclerViewLayout, R.string.downloadAdded, Snackbar.LENGTH_SHORT)
                                                    .setAction(R.string.show, new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            startActivity(new Intent(SearchActivity.this, MainActivity.class));
                                                        }
                                                    }).show();
                                        }

                                        @Override
                                        public void onException(Exception ex) {
                                            Toaster.with(SearchActivity.this).message(R.string.failedAddingDownload).ex(ex).show();
                                        }
                                    });
                        } catch (Aria2Helper.InitializingException | JSONException ex) {
                            Toaster.with(SearchActivity.this).message(R.string.failedAddingDownload).ex(ex).show();
                            return;
                        }

                        AnalyticsApplication.sendAnalytics(SearchActivity.this, Utils.ACTION_SEARCH_DOWNLOAD);
                    }
                });
            }
        });

        showDialog(dialog);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem menuItem) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem menuItem) {
        onClose();
        return true;
    }
}
