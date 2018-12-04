package uk.ac.ed.inf.coinz;

import android.opengl.Visibility;
import android.support.annotation.NonNull;
import android.support.constraint.Group;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder> {
    private ArrayList<CardViewItem> mItemList;
    public OnItemClickListener mListener;
    // user (Firebase):
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String email;

    public interface OnItemClickListener{
        //void onItemClick(int position);
        void  onDeleteClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        mListener = listener;
    }

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder{
        public ImageView mImageview;
        public TextView mTextView1;
        public TextView mTextView2;
        public ImageView mTransactionImage;
        public TextView mFrom;
        public Group mGroup;

        public RecyclerViewHolder(View itemView, OnItemClickListener listener) {
            super(itemView);

            mImageview = itemView.findViewById(R.id.card_img);
            mTextView1 = itemView.findViewById(R.id.loan_currency);
            mTextView2 = itemView.findViewById(R.id.loan_value);
            mTransactionImage = itemView.findViewById(R.id.loan_img);
            mFrom = itemView.findViewById(R.id.from_email);
            mGroup = itemView.findViewById(R.id.from_group);
           /* itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION){
                            listener.onItemClick(position);
                        }
                    }
                }
            });*/

            mTransactionImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION){
                            listener.onDeleteClick(position);
                        }
                    }
                }
            });

        }
    }

    public RecyclerViewAdapter(ArrayList<CardViewItem> itemList){
        mItemList = itemList;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        setUpUser();
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_coins, parent,false);
        RecyclerViewHolder rvh = new RecyclerViewHolder(v,mListener);
        return rvh;
    }

    private void setUpUser() {
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        email = currentUser.getEmail();
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        CardViewItem currentItem = mItemList.get(position);
        holder.mImageview.setImageResource(currentItem.getImageResource());
        holder.mTextView1.setText(currentItem.getText1());
        holder.mTextView2.setText(currentItem.getText2());
        String from = currentItem.getFrom();
        if (from == email){
            //holder.mGroup.setVisibility(View.GONE);
            holder.mFrom.setText(from);
        }else holder.mFrom.setText(from);
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }




}
