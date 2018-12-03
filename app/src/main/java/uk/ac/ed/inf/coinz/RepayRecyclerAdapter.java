package uk.ac.ed.inf.coinz;

import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class RepayRecyclerAdapter extends RecyclerView.Adapter<RepayRecyclerAdapter.RepayRecyclerHolder> {

    private ArrayList<RepayCardItem> mItemList;
    private RepayRecyclerAdapter.OnItemClickListener mListener;
    private int mExpandedPosition = -1;


    public interface OnItemClickListener{
        void onItemClick(int position);
    }

    public void setOnItemClickListener(RepayRecyclerAdapter.OnItemClickListener listener){
        mListener = listener;
    }

    public static class RepayRecyclerHolder extends RecyclerView.ViewHolder{
        public TextView mEmailTextView;
        public TextView mDaysTextView;
        public ConstraintLayout mDetailsLayout;

        public RepayRecyclerHolder(View itemView, RepayRecyclerAdapter.OnItemClickListener listener){
            super(itemView);

            mEmailTextView = itemView.findViewById(R.id.user_email);
            mDaysTextView =  itemView.findViewById(R.id.repay_period);
            mDetailsLayout = itemView.findViewById(R.id.deatils_layout);


            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null){
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION){
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }

    public  RepayRecyclerAdapter(ArrayList<RepayCardItem> itemList) {
        mItemList = itemList;
    }

    @NonNull
    @Override
    public RepayRecyclerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_repay, parent, false);
        return new RepayRecyclerHolder(v, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RepayRecyclerHolder holder, int position) {
        RepayCardItem currentItem = mItemList.get(position);
        holder.mEmailTextView.setText(currentItem.getEmail());
        holder.mDaysTextView.setText(Integer.toString(currentItem.getDays()) + " day(s)");
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }


}
