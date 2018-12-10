package com.gianlu.aria2app.Options;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.gianlu.aria2app.Adapters.OptionsAdapter;
import com.gianlu.aria2app.R;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class OptionsView extends FrameLayout {
    private final RecyclerView list;
    private OptionsAdapter adapter;

    public OptionsView(Context context) {
        this(context, null, 0);
    }

    public OptionsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OptionsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(getContext()).inflate(R.layout.view_options, this, true);

        list = findViewById(R.id.optionsView_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        list.setHasFixedSize(true);

        final EditText query = findViewById(R.id.optionsView_query);
        query.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (adapter != null) adapter.filter(s.toString());
            }
        });

        ImageButton search = findViewById(R.id.optionsView_search);
        search.setOnClickListener(v -> {
            if (adapter != null) adapter.filter(query.getText().toString());
        });
    }

    @Nullable
    public OptionsAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(OptionsAdapter adapter) {
        this.adapter = adapter;
        this.list.setAdapter(adapter);
    }
}
