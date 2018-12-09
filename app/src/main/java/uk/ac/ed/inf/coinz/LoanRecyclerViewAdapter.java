package uk.ac.ed.inf.coinz;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

// adapter for loan recycler (in bank)
public class LoanRecyclerViewAdapter extends RecyclerView.Adapter<LoanRecyclerViewAdapter.LoanRecyclerViewHolder>  {

    private ArrayList<LoanCardItem> mItemList;
    private LoanRecyclerViewAdapter.OnItemClickListener mListener;

    // interface for item click listener (implemented below)
    public interface OnItemClickListener{
        void  onDeleteClick(int position);
    }
    public void setOnItemClickListener(LoanRecyclerViewAdapter.OnItemClickListener listener){
        mListener = listener;
    }

    // view holder with click listener overriding
    static class LoanRecyclerViewHolder extends RecyclerView.ViewHolder{
        ImageView mCurrencyImageview;
        TextView mTextValue;
        TextView mTextCurrency;
        TextView mTextInterestRate;
        TextView mTextRepayPeriod;
        ImageView mLoanImage;

        LoanRecyclerViewHolder(View itemView, LoanRecyclerViewAdapter.OnItemClickListener listener) {
            super(itemView);

            mCurrencyImageview = itemView.findViewById(R.id.card_img);
            mTextCurrency = itemView.findViewById(R.id.loan_currency);
            mTextValue = itemView.findViewById(R.id.loan_value);
            mTextInterestRate = itemView.findViewById(R.id.interest_rate);
            mTextRepayPeriod = itemView.findViewById(R.id.repay_period);
            mLoanImage = itemView.findViewById(R.id.loan_img);

            // return clicked card position (overridden in BankFragment.kt)
            mLoanImage.setOnClickListener(view -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION){
                        listener.onDeleteClick(position);
                    }
                }
            });

        }
    }

    public LoanRecyclerViewAdapter(ArrayList<LoanCardItem> itemList){
        mItemList = itemList;
    }

    @NonNull
    @Override
    public LoanRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_coins_loan, parent,false);
        return new LoanRecyclerViewHolder(v,mListener);
    }

    // creating the cards (associating the card object elements with the View objects in the layout)
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull LoanRecyclerViewHolder holder, int position) {
        LoanCardItem currentItem = mItemList.get(position);
        holder.mCurrencyImageview.setImageResource(currentItem.getCurrencyImageResource());
        holder.mTextCurrency.setText(currentItem.getCurrency());
        holder.mTextValue.setText("Value: " + currentItem.getValue().toString());
        holder.mTextRepayPeriod.setText("Repay in: " + Integer.toString(currentItem.getRepayPeriod()));
        Double interestRate = (currentItem.getInterestRate()*100);
        holder.mTextInterestRate.setText("Interest rate: " + interestRate.toString() + "%");


    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }


}
