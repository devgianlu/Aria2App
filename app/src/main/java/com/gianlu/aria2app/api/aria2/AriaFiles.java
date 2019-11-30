package com.gianlu.aria2app.api.aria2;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import androidx.annotation.Nullable;

public class AriaFiles extends ArrayList<AriaFile> {

    public AriaFiles(JSONArray array) throws JSONException {
        super(array.length());

        for (int i = 0; i < array.length(); i++)
            add(new AriaFile(array.getJSONObject(i)));

        if (!isEmpty()) AriaDirectory.guessSeparator(get(0));
    }

    @Nullable
    public AriaFile findFileByIndex(int index) {
        for (AriaFile file : this)
            if (file.index == index)
                return file;

        return null;
    }
}
