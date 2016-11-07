package com.gianlu.aria2app.Terminal;

import android.app.Activity;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TerminalAdapter extends RecyclerView.Adapter<TerminalAdapter.ViewHolder> {
    private final Activity context;
    private final IAdapter handler;
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss.SSSS", Locale.getDefault());
    private final List<TerminalItem> objs;

    public TerminalAdapter(Activity context, IAdapter handler) {
        this.context = context;
        this.handler = handler;
        this.objs = new ArrayList<>();

        try {
            WebSocketRequester.getInstance(context, new WebSocketHandler());
        } catch (IOException | NoSuchAlgorithmException ex) {
            add(TerminalItem.createInfoItem(ex));
        }
    }

    private static boolean isFromServer(int type) {
        return (type & TerminalItem.FROM_SERVER) == TerminalItem.FROM_SERVER;
    }

    public void add(TerminalItem item) {
        objs.add(item);
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0)
            return null;

        ViewHolder holder = new ViewHolder(LayoutInflater.from(context).inflate(R.layout.terminal_cardview, parent, false));

        if ((viewType & TerminalItem.TYPE_CONVERSATION) == TerminalItem.TYPE_CONVERSATION) {
            holder.getRootView().addView(CommonUtils.fastHorizontalLinearLayoutWeightDummy(context, 1), isFromServer(viewType) ? 1 : 0);
        } else {
            holder.getRootView().addView(CommonUtils.fastHorizontalLinearLayoutWeightDummy(context, 1), 0);
            holder.getRootView().addView(CommonUtils.fastHorizontalLinearLayoutWeightDummy(context, 1), 2);
            holder.cardView.setAlpha(.3f);
        }

        return holder;
    }

    @Override
    public int getItemViewType(int position) {
        TerminalItem item = getItem(position);

        if (item.type == TerminalItem.TYPE_INFO) {
            return TerminalItem.TYPE_INFO;
        } else {
            return TerminalItem.TYPE_CONVERSATION | (item.fromServer ? TerminalItem.FROM_SERVER : TerminalItem.FROM_CLIENT);
        }
    }

    public void clear() {
        objs.clear();
        notifyDataSetChanged();

        if (handler != null)
            handler.onItemCountUpdated(0);
    }

    private TerminalItem getItem(int position) {
        return objs.get(position);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        if (holder == null)
            return;

        final TerminalItem item = getItem(position);
        holder.text.setText(item.text);
        holder.detailsAt.setText(timeFormatter.format(new Date(item.at)));

        if (item.type == TerminalItem.TYPE_INFO) {
            if (item.isException())
                holder.text.setTextColor(Color.RED);
            holder.detailsLength.setVisibility(View.GONE);
        } else {
            holder.detailsLength.setText(CommonUtils.dimensionFormatter(item.text.length()));
        }

        holder.expand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonUtils.animateCollapsingArrowBellows(holder.expand, CommonUtils.isExpanded(holder.details));

                if (CommonUtils.isExpanded(holder.details))
                    CommonUtils.collapse(holder.details);
                else
                    CommonUtils.expand(holder.details);
            }
        });
        holder.expand.setFocusable(false);

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (handler != null)
                    handler.onItemClick(item);
            }
        });
        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (handler != null)
                    handler.onItemLongClick(item);
                return true;
            }
        });
    }

    public void remove(TerminalItem item) {
        objs.remove(item);
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        if (handler != null)
            handler.onItemCountUpdated(objs.size());
        return objs.size();
    }

    public interface IAdapter {
        void onItemCountUpdated(int count);

        void onItemClick(TerminalItem item);

        void onItemLongClick(TerminalItem item);
    }

    private class WebSocketHandler extends WebSocketAdapter {
        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            add(TerminalItem.createInfoItem(context.getString(R.string.connected)));
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
            add(TerminalItem.createInfoItem(exception));
        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            add(TerminalItem.createConversationServerItem(text));
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            add(TerminalItem.createInfoItem(context.getString(R.string.disconnected)));
        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
            add(TerminalItem.createInfoItem(cause));
        }

        @Override
        public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {
            CommonUtils.logMe(context, cause);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView text;
        final CardView cardView;
        final ImageButton expand;
        final LinearLayout details;
        final TextView detailsAt;
        final TextView detailsLength;

        public ViewHolder(View itemView) {
            super(itemView);

            cardView = (CardView) itemView.findViewById(R.id.terminalItem_card);
            expand = (ImageButton) itemView.findViewById(R.id.terminalItem_expand);
            details = (LinearLayout) itemView.findViewById(R.id.terminalItem_details);
            detailsAt = (TextView) itemView.findViewById(R.id.terminalItem_detailsAt);
            detailsLength = (TextView) itemView.findViewById(R.id.terminalItem_detailsLength);
            text = (TextView) itemView.findViewById(R.id.terminalItem_text);
        }

        public LinearLayout getRootView() {
            return (LinearLayout) itemView;
        }
    }
}
