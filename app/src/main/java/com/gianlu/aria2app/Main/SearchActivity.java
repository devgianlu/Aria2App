package com.gianlu.aria2app.Main;

import android.app.SearchManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.SearchView;

import com.gianlu.aria2app.Main.Search.SearchUtils;
import com.gianlu.aria2app.R;

public class SearchActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        final ImageView logoView = (ImageView) findViewById(R.id.search_logo);
        SearchUtils.retrieveWebsiteLogo(new SearchUtils.ILogo() {
            @Override
            public void onLogo(final Drawable logo) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logoView.setImageDrawable(logo);
                    }
                });
            }

            @Override
            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        });
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
        System.out.println("SUBMIT: " + query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        System.out.println("QUERY: " + newText);
        return true;
    }
}
