package uk.ac.ed.inf.coinz;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.Timestamp;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.ArrayList;

// adapter for recycler showing the pending loan repayments
public class RepayRecyclerAdapter extends RecyclerView.Adapter<RepayRecyclerAdapter.RepayRecyclerHolder> {

    private ArrayList<RepayCardItem> mItemList;
    private RepayRecyclerAdapter.OnItemClickListener mListener;


    public interface OnItemClickListener{
        void onItemClick(int position);
    }

    public void setOnItemClickListener(RepayRecyclerAdapter.OnItemClickListener listener){
        mListener = listener;
    }

    static class RepayRecyclerHolder extends RecyclerView.ViewHolder{
        TextView mEmailTextView;
        TextView mDaysTextView;
        TextView mRepayVal;
        TextView mInterestRate;
        ImageView mCoinImg;
        TextView mRepayCurrency;
        Button mRepayButton;


        RepayRecyclerHolder(View itemView, RepayRecyclerAdapter.OnItemClickListener listener){
            super(itemView);

            mEmailTextView = itemView.findViewById(R.id.user_email);
            mDaysTextView =  itemView.findViewById(R.id.repay_period);
            mRepayVal = itemView.findViewById(R.id.repay_value);
            mInterestRate = itemView.findViewById(R.id.interest_rate);
            mCoinImg = itemView.findViewById(R.id.coin_img);
            mRepayCurrency = itemView.findViewById(R.id.repay_currency);
            mRepayButton = itemView.findViewById(R.id.repay_button);



            mRepayButton.setOnClickListener(view -> {
                if (listener != null){
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION){
                        listener.onItemClick(position);
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

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public void onBindViewHolder(@NonNull RepayRecyclerHolder holder, int position) {
        RepayCardItem currentItem = mItemList.get(position);
        holder.mEmailTextView.setText(currentItem.getEmail());

        int totalNoOFDays = currentItem.getDays();
        int noOfDaysLeft = numberOfDaysLeft(totalNoOFDays,currentItem.getDateTaken());
        holder.mDaysTextView.setText(Integer.toString(noOfDaysLeft) + " day(s)");

        holder.mRepayCurrency.setText(currentItem.getCurrency());

        holder.mInterestRate.setText(currentItem.getInterest().toString());

        holder.mCoinImg.setImageResource(currentItem.getCoinImg());

        Double repayAmount = repayAmount(noOfDaysLeft,totalNoOFDays,currentItem.getValue()
                , currentItem.getInterest());
        holder.mRepayVal.setText(String.format("%1$,.2f", repayAmount));
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    // returns number of days left till the repayment is due
    private int numberOfDaysLeft(int totalNoOfDays, Timestamp dateTaken){
        LocalDate dateTakenJoda = new LocalDate(new DateTime(dateTaken.toDate()));
        int numberOfDaysSince = Days.daysBetween(dateTakenJoda, new LocalDate()).getDays();

        return totalNoOfDays - numberOfDaysSince;
    }

    // returns the amount to be repayed
    @NonNull
    private Double repayAmount(int noOfDaysLeft, int totalNoOfDays, Double originalVal, Double interestRate){
        return originalVal* Math.pow((interestRate + 1),totalNoOfDays-noOfDaysLeft);
    }

}
