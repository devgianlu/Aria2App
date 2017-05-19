package com.gianlu.aria2app.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.TreeNode;
import com.gianlu.aria2app.R;

@SuppressLint("ViewConstructor")
public class BreadcrumbSegment extends LinearLayout {

    public BreadcrumbSegment(Context context, final TreeNode node, final IBreadcrumb handler) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.breadcrumb_segment, this, true);

        Button name = (Button) findViewById(R.id.breadcrumbSegment_name);
        name.setText(node.name);
        name.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null) handler.onDirSelected(node);
            }
        });

        ImageView arrow = (ImageView) findViewById(R.id.breadcrumbSegment_arrow);
        arrow.setVisibility(node.isRoot() ? GONE : VISIBLE);
    }

    public interface IBreadcrumb {
        void onDirSelected(TreeNode node);
    }
}
