package com.gianlu.aria2app.Terminal;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TerminalAdapter extends RecyclerView.Adapter<TerminalAdapter.ViewHolder> {
    private Context context;
    private IAdapter handler;
    private List<TerminalItem> objs;

    public TerminalAdapter(Context context, IAdapter handler) {
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

    private static boolean isExpanded(View v) {
        return v.getVisibility() == View.VISIBLE;
    }

    private static void expand(final View v) {
        v.measure(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        v.getLayoutParams().height = 0;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? RelativeLayout.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    private static void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    public void add(TerminalItem item) {
        objs.add(item);
        notifyDataSetChanged();
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
            holder.cardView.setAlpha(.5f);
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
    }

    public TerminalItem getItem(int position) {
        return objs.get(position);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        if (holder == null)
            return;

        TerminalItem item = getItem(position);
        holder.text.setText(item.text);

        if (item.type == TerminalItem.TYPE_INFO) {
            if (item.isException())
                holder.text.setTextColor(Color.RED);
        } else {

        }

        holder.expand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonUtils.animateCollapsingArrowBellows(holder.expand, isExpanded(holder.more));

                if (isExpanded(holder.more))
                    collapse(holder.more);
                else
                    expand(holder.more);
            }
        });
    }

    @Override
    public int getItemCount() {
        handler.onItemCountUpdated(objs.size());
        return objs.size();
    }

    public interface IAdapter {
        void onItemCountUpdated(int count);
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
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public CardView cardView;
        public ImageButton expand;
        public LinearLayout more;
        public TextView text;

        public ViewHolder(View itemView) {
            super(itemView);

            cardView = (CardView) itemView.findViewById(R.id.terminalItem_card);
            expand = (ImageButton) itemView.findViewById(R.id.terminalItem_expand);
            more = (LinearLayout) itemView.findViewById(R.id.terminalItem_more);
            text = (TextView) itemView.findViewById(R.id.terminalItem_text);
        }

        public LinearLayout getRootView() {
            return (LinearLayout) itemView;
        }
    }
}
