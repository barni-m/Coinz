package uk.ac.ed.inf.coinz;

import com.google.firebase.Timestamp;

public class RepayCardItem {
    private String mEmail;
    private int mDays;
    private String mCurrency;
    private Timestamp mDateTaken;
    private Double mInterest;
    private Double mValue;

    public RepayCardItem(String currency, Timestamp dateTaken, String email, Double interest, int days, Double value){
        mEmail = email;
        mDays = days;
        mCurrency = currency;
        mDateTaken = dateTaken;
        mInterest = interest;
        mValue = value;
    }

    public String getEmail() {
        return mEmail;
    }

    public int getDays() {
        return mDays;
    }

    public String getCurrency() {
        return mCurrency;
    }

    public Double getInterest() {
        return mInterest;
    }

    public Double getValue() {
        return mValue;
    }

    public Timestamp getDateTaken() {
        return mDateTaken;
    }
}
