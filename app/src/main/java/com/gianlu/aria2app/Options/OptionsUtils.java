package com.gianlu.aria2app.Options;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.gianlu.aria2app.Adapters.OptionsAdapter;
import com.gianlu.aria2app.NetIO.Aria2.Option;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.SuperTextView;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

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

        return builder;
    }
}
