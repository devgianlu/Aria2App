package com.gianlu.aria2app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.Terminal.AutoCompletionAdapter;
import com.gianlu.aria2app.Terminal.TerminalAdapter;
import com.gianlu.aria2app.Terminal.TerminalItem;
import com.gianlu.aria2app.Terminal.WebSocketRequester;
import com.gianlu.commonutils.CommonUtils;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// TODO: Ping feature
public class TerminalActivity extends AppCompatActivity {
    private final List<String> methods = new ArrayList<>();
    private TerminalAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);
        setTitle(R.string.title_activity_terminal);

        final RecyclerView list = (RecyclerView) findViewById(R.id.terminal_recyclerView);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        adapter = new TerminalAdapter(this, new TerminalAdapter.IAdapter() {
            @Override
            public void onItemCountUpdated(final int count) {
                TerminalActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (count > 0) {
                            list.setVisibility(View.VISIBLE);
                            findViewById(R.id.terminal_noItems).setVisibility(View.GONE);
                        } else {
                            list.setVisibility(View.GONE);
                            findViewById(R.id.terminal_noItems).setVisibility(View.VISIBLE);
                        }
                    }
                });
            }

            @Override
            public void onItemClick(TerminalItem item) {
                ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("json", item.text));
                CommonUtils.UIToast(TerminalActivity.this, CommonUtils.ToastMessage.COPIED_TO_CLIPBOARD, item.text);
            }

            @Override
            public void onItemLongClick(final TerminalItem item) {
                if (item.type == TerminalItem.TYPE_INFO)
                    return;

                AlertDialog.Builder builder = new AlertDialog.Builder(TerminalActivity.this);
                builder.setTitle(item.text)
                        .setAdapter(new ArrayAdapter<>(TerminalActivity.this, android.R.layout.simple_list_item_1, Arrays.asList("Copy to clipboard", "Modify and resend", "Delete")), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                switch (i) {
                                    case 0:
                                        onItemClick(item);
                                        break;
                                    case 1:
                                        CommonUtils.showDialog(TerminalActivity.this, createNewRequestDialog(item.text));
                                        break;
                                    case 2:
                                        adapter.remove(item);
                                        break;
                                }
                            }
                        });

                CommonUtils.showDialog(TerminalActivity.this, builder);
            }
        });
        list.setAdapter(adapter);

        try {
            WebSocketRequester.getInstance(this);
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException ex) {
            CommonUtils.UIToast(TerminalActivity.this, Utils.ToastMessages.WS_EXCEPTION, ex);
            return;
        }

        try {
            JTA2.newInstance(this).listMethods(new JTA2.IMethod() {
                @Override
                public void onMethods(List<String> methods) {
                    TerminalActivity.this.methods.addAll(methods);
                }

                @Override
                public void onException(Exception ex) {
                    CommonUtils.UIToast(TerminalActivity.this, Utils.ToastMessages.FAILED_LOADING_AUTOCOMPLETION, ex);
                }
            });
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException ex) {
            CommonUtils.UIToast(TerminalActivity.this, Utils.ToastMessages.WS_EXCEPTION, ex);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.terminal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.terminalMenu_clearList:
                adapter.clear();
                break;
            case R.id.terminalMenu_newRequest:
                CommonUtils.showDialog(this, createNewRequestDialog(null));
                break;
            case R.id.terminalMenu_newAdvancedRequest:
                CommonUtils.showDialog(this, createNewAdvancedRequestDialog());
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return true;
    }

    private AlertDialog.Builder createNewRequestDialog(@Nullable String obj) {
        if (adapter == null)
            return null;

        LinearLayout view = (LinearLayout) getLayoutInflater().inflate(R.layout.new_request_dialog, null, false);
        final EditText id = (EditText) view.findViewById(R.id.createRequestDialog_id);
        final EditText jsonrpc = (EditText) view.findViewById(R.id.createRequestDialog_jsonrpc);
        final AutoCompleteTextView method = (AutoCompleteTextView) view.findViewById(R.id.createRequestDialog_method);
        method.setAdapter(new AutoCompletionAdapter(this, methods));
        final EditText params = (EditText) view.findViewById(R.id.createRequestDialog_params);
        final TextView json = (TextView) view.findViewById(R.id.createRequestDialog_json);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    json.setTextColor(Color.BLACK);
                    json.setText(WebSocketRequester.formatRequest(id.getText().toString(),
                            jsonrpc.getText().toString(),
                            method.getText().toString(),
                            params.getText().toString()).toString());
                } catch (JSONException ex) {
                    json.setTextColor(Color.RED);
                    json.setText(getString(R.string.invalidJSON, ex.getMessage()));
                }
            }
        };
        id.addTextChangedListener(watcher);
        jsonrpc.addTextChangedListener(watcher);
        method.addTextChangedListener(watcher);
        params.addTextChangedListener(watcher);

        if (obj != null) {
            try {
                JSONObject jObj = new JSONObject(obj);

                id.setText(jObj.optString("id"));
                jsonrpc.setText(jObj.optString("jsonrpc"));
                method.setText(jObj.optString("method"));
                JSONArray arr = jObj.optJSONArray("params");
                if (arr != null) {
                    String str = arr.toString();
                    params.setText(str.substring(1, str.length() - 1));
                }
            } catch (JSONException ex) {
                CommonUtils.UIToast(this, Utils.ToastMessages.FAILED_EDIT_CONVERSATION_ITEM, ex);
                return null;
            }
        }

        return new AlertDialog.Builder(this)
                .setTitle(R.string.create_request)
                .setView(view)
                .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            String req = WebSocketRequester.getInstance(TerminalActivity.this)
                                    .request(id.getText().toString(),
                                            jsonrpc.getText().toString(),
                                            method.getText().toString(),
                                            params.getText().toString());

                            adapter.add(TerminalItem.createConversationClientItem(req));
                        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException ex) {
                            CommonUtils.UIToast(TerminalActivity.this, Utils.ToastMessages.WS_EXCEPTION, ex);
                        } catch (JSONException ex) {
                            CommonUtils.UIToast(TerminalActivity.this, Utils.ToastMessages.INVALID_REQUEST, ex);
                        }

                        ThisApplication.sendAnalytics(TerminalActivity.this, new HitBuilders.EventBuilder()
                                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                                .setAction(ThisApplication.ACTION_TERMINAL_BASIC)
                                    .build());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);
    }

    private AlertDialog.Builder createNewAdvancedRequestDialog() {
        if (adapter == null)
            return null;

        final EditText text = new EditText(this);
        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
        text.setPadding(pad, pad, pad, pad);
        text.setMinLines(5);
        text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return new AlertDialog.Builder(this)
                .setTitle(R.string.create_request)
                .setView(text)
                .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            String req = WebSocketRequester.getInstance(TerminalActivity.this)
                                    .request(text.getText().toString());

                            adapter.add(TerminalItem.createConversationClientItem(req));
                        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException ex) {
                            CommonUtils.UIToast(TerminalActivity.this, Utils.ToastMessages.WS_EXCEPTION, ex);
                        }

                        ThisApplication.sendAnalytics(TerminalActivity.this, new HitBuilders.EventBuilder()
                                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                                .setAction(ThisApplication.ACTION_TERMINAL_ADV)
                                    .build());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        WebSocketRequester.destroy();
    }
}
