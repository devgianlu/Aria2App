package com.gianlu.aria2app;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.Terminal.TerminalAdapter;
import com.gianlu.aria2app.Terminal.TerminalItem;
import com.gianlu.aria2app.Terminal.WebSocketRequester;
import com.gianlu.commonutils.CommonUtils;

import org.json.JSONException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

// TODO: Advanced mode
// TODO: Modify and resend
// TODO: Method autocompletion
public class TerminalActivity extends AppCompatActivity {
    private TerminalAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        RecyclerView list = (RecyclerView) findViewById(R.id.terminal_recyclerView);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        final TextView noItems = (TextView) findViewById(R.id.terminal_noItems);

        adapter = new TerminalAdapter(this, new TerminalAdapter.IAdapter() {
            @Override
            public void onItemCountUpdated(final int count) {
                TerminalActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (count > 0)
                            noItems.setVisibility(View.GONE);
                        else
                            noItems.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
        list.setAdapter(adapter);

        try {
            WebSocketRequester.getInstance(this).sendPing();
        } catch (IOException | NoSuchAlgorithmException ex) {
            CommonUtils.UIToast(TerminalActivity.this, Utils.ToastMessages.WS_EXCEPTION, ex);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.terminal, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.terminalMenu_clearList:
                adapter.clear();
                break;
            case R.id.terminalMenu_newRequest:
                CommonUtils.showDialog(this, createNewRequestDialog());
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public AlertDialog.Builder createNewRequestDialog() {
        if (adapter == null)
            return null;

        LinearLayout view = (LinearLayout) getLayoutInflater().inflate(R.layout.new_request_dialog, null, false);
        final EditText id = (EditText) view.findViewById(R.id.createRequestDialog_id);
        final EditText jsonrpc = (EditText) view.findViewById(R.id.createRequestDialog_jsonrpc);
        final EditText method = (EditText) view.findViewById(R.id.createRequestDialog_method);
        final EditText params = (EditText) view.findViewById(R.id.createRequestDialog_params);

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
                        } catch (IOException | NoSuchAlgorithmException ex) {
                            CommonUtils.UIToast(TerminalActivity.this, Utils.ToastMessages.WS_EXCEPTION, ex);
                        } catch (JSONException ex) {
                            CommonUtils.UIToast(TerminalActivity.this, Utils.ToastMessages.INVALID_REQUEST, ex);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        WebSocketRequester.destroy();
    }
}
