package com.gianlu.aria2app.Main;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.gianlu.aria2app.Main.Search.SearchResult;
import com.gianlu.aria2app.Main.Search.SearchResultAdapter;
import com.gianlu.aria2app.Main.Search.SearchUtils;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;
import com.google.android.gms.analytics.HitBuilders;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SearchActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    private TextView noData;
    private RecyclerView list;
    private ProgressBar loading;
    private TextView label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        setTitle(R.string.searchTorrent);

        Toolbar toolbar = (Toolbar) findViewById(R.id.search_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        loading = (ProgressBar) findViewById(R.id.search_loading);
        list = (RecyclerView) findViewById(R.id.search_list);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        label = (TextView) findViewById(R.id.search_label);
        noData = (TextView) findViewById(R.id.search_noData);

        onQueryTextSubmit(SearchUtils.TRENDING_WEEK);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.search_search).getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setOnCloseListener(this);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(final String query) {
        loading.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
        noData.setVisibility(View.GONE);

        ThisApplication.sendAnalytics(SearchActivity.this, new HitBuilders.EventBuilder()
                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                .setAction(ThisApplication.ACTION_SEARCH)
                .build());

        SearchUtils.search(query, new SearchUtils.ISearch() {
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
                            if (Objects.equals(query, SearchUtils.TRENDING_WEEK))
                                label.setText(R.string.trendingWeek);
                            else
                                label.setText(getString(R.string.searchFor, query));

                            final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(SearchActivity.this, R.string.fetching_information);
                            final ProgressDialog pdd = CommonUtils.fastIndeterminateProgressDialog(SearchActivity.this, R.string.gathering_information);
                            list.setAdapter(new SearchResultAdapter(SearchActivity.this, results, new SearchResultAdapter.IAdapter() {
                                @Override
                                public void onItemClick(final SearchResult which) {
                                    CommonUtils.showDialog(SearchActivity.this, pd);
                                    SearchUtils.findMagnetLink(which, new SearchUtils.IMagnetLink() {
                                        @Override
                                        public void onMagnetLink(@Nullable final String magnet) {
                                            pd.dismiss();

                                            LinearLayout layout = new LinearLayout(SearchActivity.this);
                                            int padding = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
                                            layout.setPadding(padding, padding, padding, padding);
                                            layout.setOrientation(LinearLayout.VERTICAL);

                                            layout.addView(new SuperTextView(SearchActivity.this, R.string.size, which.size));
                                            layout.addView(new SuperTextView(SearchActivity.this, R.string.uploader, which.uploader));
                                            layout.addView(new SuperTextView(SearchActivity.this, R.string.uploaderType, which.uploaderType.toFormal(SearchActivity.this)));

                                            AlertDialog.Builder builder = new AlertDialog.Builder(SearchActivity.this);
                                            builder.setTitle(which.name)
                                                    .setView(layout)
                                                    .setNeutralButton(R.string.openInBrowser, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int i) {
                                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(which.href)));
                                                        }
                                                    })
                                                    .setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            CommonUtils.showDialog(SearchActivity.this, pdd);
                                                            try {
                                                                JTA2.newInstance(SearchActivity.this)
                                                                        .addUri(Collections.singletonList(magnet), null, null, new JTA2.IGID() {
                                                                            @Override
                                                                            public void onGID(String gid) {
                                                                                pdd.dismiss();
                                                                                CommonUtils.UIToast(SearchActivity.this, Utils.ToastMessages.DOWNLOAD_ADDED, gid);
                                                                            }

                                                                            @Override
                                                                            public void onException(Exception ex) {
                                                                                pdd.dismiss();
                                                                                CommonUtils.UIToast(SearchActivity.this, Utils.ToastMessages.FAILED_ADD_DOWNLOAD, ex);
                                                                            }
                                                                        });
                                                            } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException ex) {
                                                                pdd.dismiss();
                                                                CommonUtils.UIToast(SearchActivity.this, Utils.ToastMessages.WS_EXCEPTION, ex);
                                                            }

                                                            ThisApplication.sendAnalytics(SearchActivity.this, new HitBuilders.EventBuilder()
                                                                    .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                                                                    .setAction(ThisApplication.ACTION_NEW_URI_SEARCH)
                                                                    .build());
                                                        }
                                                    });

                                            CommonUtils.showDialog(SearchActivity.this, builder);
                                        }

                                        @Override
                                        public void onException(Exception ex) {
                                            pd.dismiss();
                                            CommonUtils.UIToast(SearchActivity.this, Utils.ToastMessages.SEARCH_FAILED, ex);
                                        }
                                    });
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
        if (newText.length() >= 3)
            onQueryTextSubmit(newText);
        return true;
    }

    @Override
    public boolean onClose() {
        onQueryTextSubmit(SearchUtils.TRENDING_WEEK);
        return false;
    }
}
