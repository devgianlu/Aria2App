package com.gianlu.aria2app.Options;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.gianlu.aria2app.Adapters.OptionsAdapter;
import com.gianlu.aria2app.R;

public class OptionsView extends FrameLayout {
    private RecyclerView list;
    private OptionsAdapter adapter;

    public OptionsView(Context context) {
        super(context);
        init();
    }

    public OptionsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OptionsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setAdapter(OptionsAdapter adapter) {
        this.adapter = adapter;
        list.setAdapter(adapter);
    }

    public void setIsDialog(boolean dialog) {
        if (dialog) {
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
            setPadding(padding, 0, padding, 0);
        } else {
            setPadding(0, 0, 0, 0);
        }
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.options_view, this, true);

        list = findViewById(R.id.optionsView_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        final EditText query = findViewById(R.id.optionsView_query);
        ImageButton search = findViewById(R.id.optionsView_search);

        query.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                adapter.filter(s.toString());
            }
        });

        search.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.filter(query.getText().toString());
            }
        });
    }
}
