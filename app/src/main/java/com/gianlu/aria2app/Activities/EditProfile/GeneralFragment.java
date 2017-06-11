package com.gianlu.aria2app.Activities.EditProfile;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.ProfilesManager.UserProfile;
import com.gianlu.aria2app.R;

public class GeneralFragment extends FieldErrorFragment {
    private TextInputLayout profileName;
    private CheckBox enableNotifs;
    private LinearLayout layout;

    public static GeneralFragment getInstance(Context context, @Nullable UserProfile edit) {
        GeneralFragment fragment = new GeneralFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.general));
        if (edit != null) args.putSerializable("edit", edit);
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (LinearLayout) inflater.inflate(R.layout.edit_profile_general_fragment, container, false);
        profileName = (TextInputLayout) layout.findViewById(R.id.editProfile_profileName);
        profileName.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                profileName.setErrorEnabled(false);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        enableNotifs = (CheckBox) layout.findViewById(R.id.editProfile_enableNotifs);

        UserProfile edit = (UserProfile) getArguments().getSerializable("edit");
        if (edit != null) {
            profileName.getEditText().setText(edit.getProfileName(getContext()));
            enableNotifs.setChecked(edit.notificationsEnabled);
        }

        return layout;
    }

    @Override
    public void onFieldError(@IdRes int fieldId, String reason) {
        TextInputLayout inputLayout = (TextInputLayout) layout.findViewById(fieldId);
        if (inputLayout != null) {
            inputLayout.setErrorEnabled(true);
            inputLayout.setError(reason);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public Fields getFields() throws InvalidFieldException {
        String profileName = this.profileName.getEditText().getText().toString().trim();
        if (profileName.isEmpty() ||
                (ProfilesManager.get(getContext()).profileExists(ProfilesManager.getId(profileName))
                        && getArguments().getSerializable("edit") == null)) {
            throw new InvalidFieldException(getClass(), R.id.editProfile_profileName, getString(R.string.invalidProfileName));
        }

        return new Fields(profileName, enableNotifs.isChecked());
    }

    public class Fields {
        public final String profileName;
        public final boolean enableNotifs;

        public Fields(String profileName, boolean enableNotifs) {
            this.profileName = profileName;
            this.enableNotifs = enableNotifs;
        }
    }
}
