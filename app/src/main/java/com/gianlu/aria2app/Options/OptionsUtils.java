package com.gianlu.aria2app.Options;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.gianlu.aria2app.Adapters.OptionsAdapter;
import com.gianlu.aria2app.NetIO.Aria2.Option;
import com.gianlu.aria2app.NetIO.TrackersListFetch;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.misc.SuperTextView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class OptionsUtils {

    @NonNull
    public static AlertDialog.Builder getEditOptionDialog(@NonNull Context context, final Option option, final OptionsAdapter adapter) {
        LinearLayout layout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.dialog_edit_option, null, false);

        boolean multiple = Objects.equals(option.name, "header") || Objects.equals(option.name, "index-out");

        SuperTextView value = layout.findViewById(R.id.editOptionDialog_value);
        value.setHtml(R.string.currentValue, (option.value == null || option.value.isEmpty()) ? "<i>not set</i>" : option.value.strings("; "));

        EditText edit = layout.findViewById(R.id.editOptionDialog_edit);
        edit.setSingleLine(!multiple);
        if (multiple) edit.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        edit.setMaxLines(multiple ? Integer.MAX_VALUE : 1);

        if (option.isValueChanged())
            edit.setText(option.newValue == null ? null : option.newValue.strings("\n"));
        else
            edit.setText(option.value == null ? null : option.value.strings("\n"));

        layout.findViewById(R.id.editOptionDialog_multipleHelp)
                .setVisibility(multiple ? View.VISIBLE : View.GONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(layout)
                .setTitle(option.name)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.set, (dialog, which) -> {
                    String str = edit.getText().toString();
                    if (multiple) option.setNewValue(str.split("\\n"));
                    else option.setNewValue(str);

                    if (adapter != null) adapter.optionChanged(option);
                });

        if (option.name.equals("bt-tracker")) {
            builder.setNeutralButton(R.string.addBestTrackers, (dialog, which) ->
                    TrackersListFetch.get().getTrackers(TrackersListFetch.Type.BEST, null, new TrackersListFetch.Listener() {
                        @Override
                        public void onDone(@NonNull List<String> trackers) {
                            Set<String> set = new HashSet<>(trackers);
                            String oldStr = edit.getText().toString();
                            if (!oldStr.isEmpty()) {
                                String[] old = oldStr.split(",");
                                set.addAll(Arrays.asList(old));
                            }

                            option.setNewValue(CommonUtils.join(set, ","));
                            if (adapter != null) adapter.optionChanged(option);
                        }

                        @Override
                        public void onFailed(@NonNull Exception ex) {
                            Logging.log(ex);
                        }
                    }));
        }


        return builder;
    }
}
