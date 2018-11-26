package uk.ac.ed.inf.coinz;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder> {
    private ArrayList<CardViewItem> mItemList;


    public static class RecyclerViewHolder extends RecyclerView.ViewHolder{
        public ImageView mImageview;
        public TextView mTextView1;
        public TextView mTextView2;

        public RecyclerViewHolder(View itemView) {
            super(itemView);

            mImageview = itemView.findViewById(R.id.card_img);
            mTextView1 = itemView.findViewById(R.id.main_text);
            mTextView2 = itemView.findViewById(R.id.description_text);

        }
    }

    public RecyclerViewAdapter(ArrayList<CardViewItem> itemList){
        mItemList = itemList;

    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_coins, parent,false);
        RecyclerViewHolder rvh = new RecyclerViewHolder(v);
        return rvh;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        CardViewItem currentItem = mItemList.get(position);
        holder.mImageview.setImageResource(currentItem.getImageResource());
        holder.mTextView1.setText(currentItem.getText1());
        holder
                .mTextView2
                .setText(currentItem
                        .getText2());
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }
}
