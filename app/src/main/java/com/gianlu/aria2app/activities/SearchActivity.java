package com.gianlu.aria2app.activities;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.aria2app.adapters.SearchResultsAdapter;
import com.gianlu.aria2app.api.AbstractClient;
import com.gianlu.aria2app.api.AriaRequests;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.search.MissingSearchEngine;
import com.gianlu.aria2app.api.search.SearchApi;
import com.gianlu.aria2app.api.search.SearchEngine;
import com.gianlu.aria2app.api.search.SearchResult;
import com.gianlu.aria2app.api.search.Torrent;
import com.gianlu.aria2app.main.MainActivity;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.ui.Toaster;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SearchActivity extends ActivityWithDialog implements SearchView.OnQueryTextListener, SearchView.OnCloseListener, SearchApi.OnSearch, SearchResultsAdapter.Listener, MenuItem.OnActionExpandListener {
    private static final String TAG = SearchActivity.class.getSimpleName();
    private RecyclerMessageView rmv;
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
        rmv = findViewById(R.id.search_rmv);
        rmv.stopLoading();
        rmv.disableSwipeRefresh();
        rmv.linearLayoutManager(RecyclerView.VERTICAL, false);
        rmv.dividerDecoration(RecyclerView.VERTICAL);

        final Button messageMore = findViewById(R.id.search_messageMore);
        messageMore.setOnClickListener(view -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://gianlu.xyz/projects/TorrentSearchEngine")));
            } catch (ActivityNotFoundException ex) {
                messageMore.setVisibility(View.GONE);
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

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.searchEngines)
                .setMultiChoiceItems(enginesNames, checkedEngines, (dialog, which, isChecked) -> checkedEngines[which] = isChecked)
                .setPositiveButton(R.string.apply, (dialog, which) -> {
                    Set<String> set = new HashSet<>();
                    for (int i = 0; i < checkedEngines.length; i++)
                        if (checkedEngines[i]) set.add(engines.get(i).id);

                    if (set.isEmpty()) {
                        Toaster.with(SearchActivity.this).message(R.string.noEnginesSelected).show();
                    } else {
                        Prefs.putSet(PK.A2_SEARCH_ENGINES, set);
                        if (query != null) onQueryTextSubmit(query);
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
        rmv.startLoading();
        this.query = query;

        searchApi.search(query.trim(), SearchApi.RESULTS_PER_REQUEST, Prefs.getSet(PK.A2_SEARCH_ENGINES, null), null, this);

        AnalyticsApplication.sendAnalytics(Utils.ACTION_SEARCH);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onClose() {
        message.setVisibility(View.VISIBLE);
        rmv.stopLoading();
        rmv.hideList();
        rmv.hideMessage();
        searchView.setQuery(null, false);
        this.query = null;
        return false;
    }

    @Override
    public void onResult(List<SearchResult> results, List<MissingSearchEngine> missingEngines, @Nullable String nextPageToken) {
        message.setVisibility(View.GONE);

        rmv.loadListData(new SearchResultsAdapter(this, results, nextPageToken, this));
        notifyMissingEngines(missingEngines);
    }

    @Override
    public void onException(@NonNull Exception ex) {
        message.setVisibility(View.GONE);
        rmv.showError(R.string.searchEngine_offline);
        Log.e(TAG, "Failed search.", ex);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.search_engines) {
            showProgress(R.string.gathering_information);
            searchApi.listSearchEngines(this, new SearchApi.OnResult<List<SearchEngine>>() {
                @Override
                public void onResult(@NonNull List<SearchEngine> result) {
                    dismissDialog();
                    showEnginesDialog(result);
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    dismissDialog();
                    Log.e(TAG, "Failed getting engines list.", ex);
                    Toaster.with(SearchActivity.this).message(R.string.failedLoading).show();
                }
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResultSelected(@NonNull SearchResult result) {
        showProgress(R.string.gathering_information);
        searchApi.getTorrent(result, this, new SearchApi.OnResult<Torrent>() {
            @Override
            public void onResult(@NonNull Torrent torrent) {
                dismissDialog();
                showTorrentDialog(torrent);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                dismissDialog();
                Log.e(TAG, "Failed getting torrent info.", ex);
                Toaster.with(SearchActivity.this).message(R.string.failedLoading).show();
            }
        });
    }

    @Override
    public void notifyMissingEngines(@NonNull final List<MissingSearchEngine> missingEngines) {
        if (missingEngines.isEmpty()) return;

        Snackbar.make(rmv, R.string.missingEngines_message, Snackbar.LENGTH_LONG)
                .setAction(R.string.show, view -> showMissingEnginesDialog(missingEngines)).show();
    }

    private void showMissingEnginesDialog(List<MissingSearchEngine> missingEngines) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
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

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(torrent.title)
                .setView(layout)
                .setNegativeButton(R.string.getMagnet, (dialogInterface, i) -> {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, torrent.magnet);
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);

                    AnalyticsApplication.sendAnalytics(Utils.ACTION_SEARCH_GET_MAGNET);
                })
                .setPositiveButton(R.string.download, null);

        if (torrent.torrentFileUrl != null) {
            builder.setNeutralButton(R.string.getTorrent, (dialogInterface, i) -> {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(torrent.torrentFileUrl)));

                AnalyticsApplication.sendAnalytics(Utils.ACTION_SEARCH_GET_TORRENT);
            });
        }

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                Aria2Helper.instantiate(SearchActivity.this)
                        .request(AriaRequests.addUri(Collections.singletonList(torrent.magnet), null, null), new AbstractClient.OnResult<String>() {
                            @Override
                            public void onResult(@NonNull String result) {
                                dialogInterface.dismiss();

                                Snackbar.make(rmv, R.string.downloadAdded, Snackbar.LENGTH_SHORT)
                                        .setAction(R.string.show, view1 -> startActivity(new Intent(SearchActivity.this, MainActivity.class))).show();
                            }

                            @Override
                            public void onException(@NonNull Exception ex) {
                                Log.e(TAG, "Failed adding URI.", ex);
                                Toaster.with(SearchActivity.this).message(R.string.failedAddingDownload).show();
                            }
                        });
            } catch (Aria2Helper.InitializingException | JSONException ex) {
                Log.e(TAG, "Failed initializing/parsing.", ex);
                Toaster.with(SearchActivity.this).message(R.string.failedAddingDownload).show();
                return;
            }

            AnalyticsApplication.sendAnalytics(Utils.ACTION_SEARCH_DOWNLOAD);
        }));

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
