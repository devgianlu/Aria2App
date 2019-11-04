package com.gianlu.aria2app.Activities.EditProfile;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;

import static com.gianlu.aria2app.Activities.EditProfile.InvalidFieldException.Where;

public class AuthenticationFragment extends FieldErrorFragmentWithState {
    private ScrollView layout;
    private MaterialButtonToggleGroup authMethod;
    private TextInputLayout token;
    private LinearLayout userAndPasswd;
    private TextInputLayout username;
    private TextInputLayout password;

    @NonNull
    public static AuthenticationFragment getInstance(@NonNull Context context) {
        AuthenticationFragment fragment = new AuthenticationFragment();
        fragment.setRetainInstance(true);
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.authentication));
        fragment.setArguments(args);
        return fragment;
    }

    private static int radioIdFromAuthMethod(@NonNull AbstractClient.AuthMethod method) {
        switch (method) {
            default:
            case NONE:
                return R.id.editProfile_authMethod_none;
            case HTTP:
                return R.id.editProfile_authMethod_http;
            case TOKEN:
                return R.id.editProfile_authMethod_token;
        }
    }

    @NonNull
    public static Bundle stateFromProfile(@NonNull MultiProfile.UserProfile profile) {
        Bundle bundle = new Bundle();
        bundle.putInt("authMethod", radioIdFromAuthMethod(profile.authMethod));
        bundle.putString("token", profile.serverToken);
        bundle.putString("username", profile.serverUsername);
        bundle.putString("password", profile.serverPassword);
        return bundle;
    }

    @NonNull
    public static Fields validateStateAndCreateFields(@NonNull Bundle bundle) throws InvalidFieldException {
        AbstractClient.AuthMethod authMethod;
        switch (bundle.getInt("authMethod", R.id.editProfile_authMethod_none)) {
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
            token = bundle.getString("token", null);
            if (token == null || (token = token.trim()).isEmpty())
                throw new InvalidFieldException(Where.AUTHENTICATION, R.id.editProfile_token, R.string.emptyToken);
        }

        String username = null;
        String password = null;
        if (authMethod == AbstractClient.AuthMethod.HTTP) {
            username = bundle.getString("username", null);
            if (username == null || (username = username.trim()).isEmpty())
                throw new InvalidFieldException(Where.AUTHENTICATION, R.id.editProfile_username, R.string.emptyUsername);

            password = bundle.getString("password", null);
            if (password == null || (password = password.trim()).isEmpty())
                throw new InvalidFieldException(Where.AUTHENTICATION, R.id.editProfile_password, R.string.emptyPassword);
        }

        return new Fields(authMethod, token, username, password);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("authMethod", authMethod.getCheckedButtonId());
        outState.putString("token", CommonUtils.getText(token));
        outState.putString("username", CommonUtils.getText(username));
        outState.putString("password", CommonUtils.getText(password));
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle bundle) {
        authMethod.check(bundle.getInt("authMethod", R.id.editProfile_authMethod_none));
        CommonUtils.setText(token, bundle.getString("token"));
        CommonUtils.setText(username, bundle.getString("username"));
        CommonUtils.setText(password, bundle.getString("password"));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (ScrollView) inflater.inflate(R.layout.fragment_edit_profile_authentication, container, false);
        authMethod = layout.findViewById(R.id.editProfile_authenticationMethod);
        authMethod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            switch (checkedId) {
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
        });
        token = layout.findViewById(R.id.editProfile_token);
        CommonUtils.clearErrorOnEdit(token);
        userAndPasswd = layout.findViewById(R.id.editProfile_userAndPasswd);
        username = layout.findViewById(R.id.editProfile_username);
        CommonUtils.clearErrorOnEdit(username);
        password = layout.findViewById(R.id.editProfile_password);
        CommonUtils.clearErrorOnEdit(password);

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

    public static class Fields {
        public final AbstractClient.AuthMethod authMethod;
        public final String token;
        public final String username;
        public final String password;

        public Fields(AbstractClient.AuthMethod authMethod, @Nullable String token, @Nullable String username, @Nullable String password) {
            this.authMethod = authMethod;
            this.token = token;
            this.username = username;
            this.password = password;
        }
    }
}