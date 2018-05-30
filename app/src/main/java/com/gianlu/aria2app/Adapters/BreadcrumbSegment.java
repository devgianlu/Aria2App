package com.gianlu.aria2app.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.gianlu.aria2app.NetIO.Aria2.AriaDirectory;
import com.gianlu.aria2app.R;

@SuppressLint("ViewConstructor")
public class BreadcrumbSegment extends LinearLayout {

    public BreadcrumbSegment(Context context, final AriaDirectory dir, final Listener listener) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.item_breadcrumb, this, true);

        Button name = findViewById(R.id.breadcrumbSegment_name);
        name.setText(dir.name);
        name.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onDirSelected(dir);
            }
        });

        ImageView arrow = findViewById(R.id.breadcrumbSegment_arrow);
        arrow.setVisibility(dir.isRoot() ? GONE : VISIBLE);
    }

    public interface Listener {
        void onDirSelected(@NonNull AriaDirectory dir);
    }
}
