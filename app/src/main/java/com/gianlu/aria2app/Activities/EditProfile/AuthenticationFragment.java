package com.gianlu.aria2app.Activities.EditProfile;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.aria2app.ProfilesManager.UserProfile;
import com.gianlu.aria2app.R;

public class AuthenticationFragment extends FieldErrorFragment {
    public static AuthenticationFragment getInstance(Context context, @Nullable UserProfile edit) {
        AuthenticationFragment fragment = new AuthenticationFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.authentication));
        if (edit != null) args.putSerializable("edit", edit);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onFieldError(@IdRes int fieldId, String reason) {

    }

    public Fields getFields() throws InvalidFieldException {
        // TODO
        throw new InvalidFieldException(getClass(), R.id.editProfile_profileName, "");
    }

    public class Fields {
        public Fields() {
        }
    }
}