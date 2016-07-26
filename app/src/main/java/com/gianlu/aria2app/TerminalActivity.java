package com.gianlu.aria2app;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.ArrayMap;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gianlu.aria2app.Google.Analytics;
import com.gianlu.jtitan.Aria2Helper.IMethod;
import com.gianlu.jtitan.Aria2Helper.JTA2;
import com.google.android.gms.analytics.HitBuilders;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class TerminalActivity extends AppCompatActivity {
    private ListView history;
    private List<TerminalLine> lines = new ArrayList<>();
    private Map<String, Long> latencyIDs = new ArrayMap<>();
    private Menu menu;

    private RelativeLayout container;
    private RelativeLayout advancedContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);
        setTitle(R.string.title_activity_terminal);

        container = (RelativeLayout) findViewById(R.id.terminal_container);
        assert container != null;
        advancedContainer = (RelativeLayout) findViewById(R.id.terminal_advancedContainer);
        assert advancedContainer != null;
        history = (ListView) findViewById(R.id.terminal_history);
        assert history != null;
        final AutoCompleteTextView method = (AutoCompleteTextView) findViewById(R.id.terminal_method);
        assert method != null;
        final EditText id = (EditText) findViewById(R.id.terminal_id);
        assert id != null;
        EditText jsonrpc = (EditText) findViewById(R.id.terminal_jsonrpc);
        assert jsonrpc != null;
        EditText params = (EditText) findViewById(R.id.terminal_params);
        assert params != null;
        final EditText advJson = (EditText) findViewById(R.id.terminal_advJSON);
        assert advJson != null;
        final TextView json = (TextView) findViewById(R.id.terminal_json);
        assert json != null;
        Button send = (Button) findViewById(R.id.terminal_send);
        assert send != null;
        OnTextChanged listener = new OnTextChanged(json, method, id, jsonrpc, params);

        final WebSocket conn;
        try {
            conn = Utils.readyWebSocket(this);
            conn.addListener(new WSHandler())
                    .connectAsynchronously();
        } catch (IOException | NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            return;
        }

        history.setTag(true);
        history.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                ClipData data = ClipData.newPlainText("json", ((TerminalLine) adapterView.getItemAtPosition(i)).getMessage());
                ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(data);
                Utils.UIToast(TerminalActivity.this, getString(R.string.copiedClipboard));
                return true;
            }
        });

        JTA2 jta2 = Utils.readyJTA2(this);
        final ProgressDialog pd = Utils.fastProgressDialog(this, R.string.gathering_information, true, false);
        pd.show();
        jta2.listMethods(new IMethod() {
            @Override
            public void onMethods(final List<String> methods) {
                pd.dismiss();

                TerminalActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        method.setThreshold(1);
                        method.setAdapter(new ArrayAdapter<>(TerminalActivity.this, android.R.layout.simple_dropdown_item_1line, methods));
                    }
                });
            }

            @Override
            public void onException(Exception ex) {
                pd.dismiss();
                Utils.UIToast(TerminalActivity.this, Utils.TOAST_MESSAGES.FAILED_LOADING_AUTOCOMPLETION);
            }
        });

        method.addTextChangedListener(listener);
        jsonrpc.addTextChangedListener(listener);
        id.addTextChangedListener(listener);
        params.addTextChangedListener(listener);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HitBuilders.EventBuilder event = new HitBuilders.EventBuilder().setCategory(Analytics.CATEGORY_USER_INPUT);

                if (((boolean) history.getTag())) {
                    conn.sendText(json.getText().toString());
                    latencyIDs.put(id.getText().toString(), System.currentTimeMillis());

                    lines.add(new TerminalLine(false, json.getText().toString()));
                    history.setAdapter(new TerminalAdapter(lines));

                    event.setAction(Analytics.ACTION_TERMINAL_BASIC);
                } else {
                    conn.sendText(advJson.getText().toString());

                    lines.add(new TerminalLine(false, advJson.getText().toString()));
                    history.setAdapter(new TerminalAdapter(lines));

                    event.setAction(Analytics.ACTION_TERMINAL_ADV);
                }

                if (Analytics.isTrackingAllowed(TerminalActivity.this))
                    Analytics.getDefaultTracker(TerminalActivity.this.getApplication()).send(event.build());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.terminal, menu);
        this.menu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.terminalMenu_clearList:
                lines.clear();
                latencyIDs.clear();
                history.setAdapter(null);
                break;
            case R.id.terminalMenu_advanced:
                container.setVisibility(View.INVISIBLE);
                container.invalidate();
                advancedContainer.setVisibility(View.VISIBLE);
                item.setVisible(false);
                menu.getItem(2).setVisible(true);
                history.setTag(false);
                break;
            case R.id.terminalMenu_basic:
                container.setVisibility(View.VISIBLE);
                advancedContainer.setVisibility(View.INVISIBLE);
                advancedContainer.invalidate();
                item.setVisible(false);
                menu.getItem(1).setVisible(true);
                history.setTag(true);
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, MainActivity.class));
    }

    private class WSHandler extends WebSocketAdapter {
        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            Utils.UIToast(TerminalActivity.this, Utils.TOAST_MESSAGES.WS_OPENED);
        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
            Utils.UIToast(TerminalActivity.this, Utils.TOAST_MESSAGES.WS_CLOSED, cause);
        }

        @Override
        public void onTextMessage(WebSocket websocket, String payload) throws Exception {
            try {
                String id = new JSONObject(payload).getString("id");
                if (latencyIDs.containsKey(id)) {
                    lines.add(new TerminalLine(true, payload, System.currentTimeMillis() - latencyIDs.remove(id)));
                } else {
                    lines.add(new TerminalLine(true, payload, -1));
                }
            } catch (JSONException ex) {
                lines.add(new TerminalLine(true, payload, -1));
            } finally {
                history.setAdapter(new TerminalAdapter(lines));
            }
        }
    }

    private class OnTextChanged implements TextWatcher {
        private TextView json;
        private AutoCompleteTextView method;
        private EditText id;
        private EditText jsonrpc;
        private EditText params;

        OnTextChanged(TextView json, AutoCompleteTextView method, EditText id, EditText jsonrpc, EditText params) {
            this.json = json;
            this.method = method;
            this.id = id;
            this.jsonrpc = jsonrpc;
            this.params = params;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (id.getText().toString().isEmpty()) {
                id.setText(String.valueOf(new Random().nextInt(1000)));
            }
            json.setText(String.format("{\"id\": \"%s\", \"jsonrpc\": \"%s\", \"method\": \"%s\", \"params\": [%s]}", id.getText().toString(), jsonrpc.getText().toString(), method.getText().toString(), params.getText().toString()));
        }
    }

    private class TerminalLine {
        private boolean fromServer;
        private String message;
        private long latency;

        public TerminalLine(boolean fromServer, String message) {
            this.fromServer = fromServer;
            this.message = message;
            this.latency = -1;
        }

        public TerminalLine(boolean fromServer, String message, long latency) {
            this.fromServer = fromServer;
            this.message = message;
            this.latency = latency;
        }

        public boolean isFromServer() {
            return fromServer;
        }

        public String getMessage() {
            return message;
        }

        public long getLatency() {
            return latency;
        }
    }

    private class TerminalAdapter extends BaseAdapter {
        private List<TerminalLine> objs;

        public TerminalAdapter(List<TerminalLine> objs) {
            this.objs = objs;
        }

        @Override
        public int getCount() {
            return objs.size();
        }

        @Override
        public TerminalLine getItem(int i) {
            return objs.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @SuppressLint({"ViewHolder", "InflateParams"})
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            TerminalLine item = getItem(i);
            view = getLayoutInflater().inflate(R.layout.terminal_custom_item, null);

            TextView response = (TextView) view.findViewById(R.id.terminalItem_response);
            response.setTextAlignment(item.isFromServer() ? View.TEXT_ALIGNMENT_VIEW_END : View.TEXT_ALIGNMENT_VIEW_START);
            response.setTypeface(null, item.isFromServer() ? Typeface.BOLD : Typeface.NORMAL);
            response.setText(item.getMessage());
            ((TextView) view.findViewById(R.id.terminalItem_latency)).setText(item.getLatency() == -1 ? "" : String.format(Locale.getDefault(), "%d ms", item.getLatency()));

            return view;
        }
    }
}
