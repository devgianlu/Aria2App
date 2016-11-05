package com.gianlu.aria2app.Google.Billing;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.R;

import java.util.List;

public class ProductAdapter extends BaseAdapter {
    private List<Product> products;
    private Context context;
    private IAdapter handler;

    public ProductAdapter(Context context, List<Product> products, IAdapter handler) {
        this.products = products;
        this.context = context;
        this.handler = handler;
    }

    @Override
    public int getCount() {
        return products.size();
    }

    @Override
    public Product getItem(int i) {
        return products.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final Product item = getItem(i);
        ViewHolder holder = new ViewHolder(LayoutInflater.from(context).inflate(R.layout.product_item, viewGroup, false));

        switch (item.productId) {
            case "donation.lemonade":
                holder.icon.setImageResource(R.drawable.ic_lemonade_48dp);
                break;
            case "donation.coffee":
                holder.icon.setImageResource(R.drawable.ic_coffee_48dp);
                break;
            case "donation.hamburger":
                holder.icon.setImageResource(R.drawable.ic_cheese_burger_48dp);
                break;
            case "donation.pizza":
                holder.icon.setImageResource(R.drawable.ic_pizza_48dp);
                break;
            case "donation.sushi":
                holder.icon.setImageResource(R.drawable.ic_sushi_48dp);
                break;
            case "donation.champagne":
                holder.icon.setImageResource(R.drawable.ic_champagne_48dp);
                break;
        }

        holder.title.setText(item.title);
        holder.buy.setText(item.price);
        holder.buy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (handler != null)
                    handler.onItemSelected(item);
            }
        });

        return holder.rootView;
    }

    public interface IAdapter {
        void onItemSelected(Product product);
    }

    private class ViewHolder {
        public LinearLayout rootView;
        public ImageView icon;
        public TextView title;
        public Button buy;

        public ViewHolder(View rootView) {
            this.rootView = (LinearLayout) rootView;

            icon = (ImageView) rootView.findViewById(R.id.productItem_icon);
            title = (TextView) rootView.findViewById(R.id.productItem_title);
            buy = (Button) rootView.findViewById(R.id.productItem_buy);
            buy.setFocusable(false);
        }
    }
}
