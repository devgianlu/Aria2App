package com.gianlu.aria2app.Activities.EditProfile;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;

public class AuthenticationFragment extends FieldErrorFragment {
    private ScrollView layout;
    private RadioGroup authMethod;
    private TextInputLayout token;
    private LinearLayout userAndPasswd;
    private TextInputLayout username;
    private TextInputLayout password;

    public static AuthenticationFragment getInstance(Context context, @Nullable MultiProfile.UserProfile edit) {
        AuthenticationFragment fragment = new AuthenticationFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.authentication));
        if (edit != null) args.putSerializable("edit", edit);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (ScrollView) inflater.inflate(R.layout.fragment_edit_profile_authentication, container, false);
        authMethod = layout.findViewById(R.id.editProfile_authenticationMethod);
        authMethod.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int id) {
                switch (id) {
                    default:
                    case R.id.editProfile_authMethod_none:
                        token.setVisibility(View.GONE);
                        userAndPasswd.setVisibility(View.GONE);
                        break;
                    case R.id.editProfile_authMethod_token:
                        token.setVisibility(View.VISIBLE);
                        userAndPasswd.setVisibility(View.GONE);
                        break;
                    case R.id.editProfile_authMethod_http:
                        token.setVisibility(View.GONE);
                        userAndPasswd.setVisibility(View.VISIBLE);
                        break;
                }
            }
        });
        token = layout.findViewById(R.id.editProfile_token);
        CommonUtils.getEditText(token).addTextChangedListener(new TextWatcher() {
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
        userAndPasswd = layout.findViewById(R.id.editProfile_userAndPasswd);
        username = layout.findViewById(R.id.editProfile_username);
        CommonUtils.getEditText(username).addTextChangedListener(new TextWatcher() {
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
        password = layout.findViewById(R.id.editProfile_password);
        CommonUtils.getEditText(password).addTextChangedListener(new TextWatcher() {
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

        Bundle args = getArguments();
        MultiProfile.UserProfile edit;
        if (args != null && (edit = (MultiProfile.UserProfile) args.getSerializable("edit")) != null) {
            switch (edit.authMethod) {
                default:
                case NONE:
                    authMethod.check(R.id.editProfile_authMethod_none);
                    break;
                case HTTP:
                    authMethod.check(R.id.editProfile_authMethod_http);
                    CommonUtils.setText(username, edit.serverUsername);
                    CommonUtils.setText(password, edit.serverPassword);
                    break;
                case TOKEN:
                    authMethod.check(R.id.editProfile_authMethod_token);
                    CommonUtils.setText(token, edit.serverToken);
                    break;
            }
        }

        created = true;

        return layout;
    }

    @Override
    public void onFieldError(@IdRes int fieldId, String reason) {
        TextInputLayout inputLayout = layout.findViewById(fieldId);
        if (inputLayout != null) {
            inputLayout.setErrorEnabled(true);
            inputLayout.setError(reason);
        }
    }

    public Fields getFields(Context context) throws InvalidFieldException {
        if (!created) {
            Bundle args = getArguments();
            MultiProfile.UserProfile edit;
            if (args == null || (edit = (MultiProfile.UserProfile) getArguments().getSerializable("edit")) == null)
                return null;
            else
                return new Fields(edit.authMethod, edit.serverToken, edit.serverUsername, edit.serverPassword);
        }

        AbstractClient.AuthMethod authMethod;
        switch (this.authMethod.getCheckedRadioButtonId()) {
            default:
            case R.id.editProfile_authMethod_none:
                authMethod = AbstractClient.AuthMethod.NONE;
                break;
            case R.id.editProfile_authMethod_token:
                authMethod = AbstractClient.AuthMethod.TOKEN;
                break;
            case R.id.editProfile_authMethod_http:
                authMethod = AbstractClient.AuthMethod.HTTP;
                break;
        }

        String token = null;
        if (authMethod == AbstractClient.AuthMethod.TOKEN) {
            token = CommonUtils.getText(this.token).trim();
            if (token.isEmpty()) {
                throw new InvalidFieldException(getClass(), R.id.editProfile_token, context.getString(R.string.emptyToken));
            }
        }

        String username = null;
        String password = null;
        if (authMethod == AbstractClient.AuthMethod.HTTP) {
            username = CommonUtils.getText(this.username).trim();
            if (username.isEmpty())
                throw new InvalidFieldException(getClass(), R.id.editProfile_username, context.getString(R.string.emptyUsername));

            password = CommonUtils.getText(this.password).trim();
            if (password.isEmpty())
                throw new InvalidFieldException(getClass(), R.id.editProfile_password, context.getString(R.string.emptyPassword));
        }

        return new Fields(authMethod, token, username, password);
    }

    public class Fields {
        public final AbstractClient.AuthMethod authMethod;
        public final String token;
        public final String username;
        public final String password;

        Fields(AbstractClient.AuthMethod authMethod, @Nullable String token, @Nullable String username, @Nullable String password) {
            this.authMethod = authMethod;
            this.token = token;
            this.username = username;
            this.password = password;
        }
    }
}