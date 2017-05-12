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
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.ProfilesManager.UserProfile;
import com.gianlu.aria2app.R;

public class AuthenticationFragment extends FieldErrorFragment {
    private LinearLayout layout;
    private RadioGroup authMethod;
    private TextInputLayout token;
    private LinearLayout userAndPasswd;
    private TextInputLayout username;
    private TextInputLayout password;

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
        layout = (LinearLayout) inflater.inflate(R.layout.edit_profile_authentication_fragment, container, false);
        authMethod = (RadioGroup) layout.findViewById(R.id.editProfile_authenticationMethod);
        authMethod.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int id) {
                // TODO
            }
        });
        token = (TextInputLayout) layout.findViewById(R.id.editProfile_token);
        token.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                token.setErrorEnabled(false);
            }
        });
        userAndPasswd = (LinearLayout) layout.findViewById(R.id.editProfile_userAndPasswd);
        username = (TextInputLayout) layout.findViewById(R.id.editProfile_username);
        username.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                username.setErrorEnabled(false);
            }
        });
        password = (TextInputLayout) layout.findViewById(R.id.editProfile_password);
        password.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                password.setErrorEnabled(false);
            }
        });
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

    public Fields getFields() throws InvalidFieldException {
        JTA2.AuthMethod authMethod;
        switch (this.authMethod.getCheckedRadioButtonId()) {
            default:
            case R.id.editProfile_authMethod_none:
                authMethod = JTA2.AuthMethod.NONE;
                break;
            case R.id.editProfile_authMethod_token:
                authMethod = JTA2.AuthMethod.TOKEN;
                break;
            case R.id.editProfile_authMethod_http:
                authMethod = JTA2.AuthMethod.HTTP;
                break;
        }

        String token = null;
        if (authMethod == JTA2.AuthMethod.TOKEN) {
            token = this.token.getEditText().getText().toString().trim();
            if (token.isEmpty()) {
                throw new InvalidFieldException(getClass(), R.id.editProfile_token, getString(R.string.emptyToken));
            }
        }

        String username = null;
        String password = null;
        if (authMethod == JTA2.AuthMethod.HTTP) {
            username = this.username.getEditText().getText().toString().trim();
            password = this.password.getEditText().getText().toString().trim();
            if (username.isEmpty()) {
                throw new InvalidFieldException(getClass(), R.id.editProfile_token, getString(R.string.emptyUsername));
            }
            if (password.isEmpty()) {
                throw new InvalidFieldException(getClass(), R.id.editProfile_token, getString(R.string.emptyPassword));
            }
        }

        return new Fields(authMethod, token, username, password);
    }

    public class Fields {
        public final JTA2.AuthMethod authMethod;
        public final String token;
        public final String username;
        public final String password;

        public Fields(JTA2.AuthMethod authMethod, @Nullable String token, @Nullable String username, @Nullable String password) {
            this.authMethod = authMethod;
            this.token = token;
            this.username = username;
            this.password = password;
        }
    }
}