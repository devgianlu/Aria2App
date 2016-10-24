package com.gianlu.aria2app.Terminal;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.gianlu.aria2app.R;

import java.util.List;

// Must use ArrayAdapter because it implements Filterable
public class AutoCompletionAdapter extends ArrayAdapter<String> {
    private final View.OnClickListener helpClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://aria2.github.io/manual/en/html/aria2c.html#" + view.getTag())));
        }
    };

    public AutoCompletionAdapter(Context context, List<String> methods) {
        super(context, R.layout.method_autocompletion_item, methods);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        convertView = LayoutInflater.from(getContext()).inflate(R.layout.method_autocompletion_item, parent, false);
        ((TextView) convertView.findViewById(R.id.methodAutocompletionItem_text)).setText(getItem(position));
        View help = convertView.findViewById(R.id.methodAutocompletionItem_help);
        help.setOnClickListener(helpClick);
        help.setFocusable(false);
        help.setTag(getItem(position));

        return convertView;
    }
}
