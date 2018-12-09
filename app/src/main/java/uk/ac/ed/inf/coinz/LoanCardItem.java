package uk.ac.ed.inf.coinz;

// loan card object for loan recycler view
public class LoanCardItem {
    private int mImageResource;
    private Double mVal;
    private String mCurrency;
    private Double mInterest;
    private int mRepayPeriod;
    private String mId;
    private String mEmail;

    public LoanCardItem(int currencyImage,String currency, Double val,Double interest, int repayPeriod, String id, String email){
        mImageResource = currencyImage;
        mVal = val;
        mInterest = interest;
        mRepayPeriod =  repayPeriod;
        mCurrency = currency;
        mId = id;
        mEmail = email;
    }

    public int getCurrencyImageResource(){
        return mImageResource;
    }

    public Double getValue(){
        return mVal;
    }

    public Double getInterestRate(){
        return mInterest;
    }

    public int getRepayPeriod(){
        return mRepayPeriod;
    }

    public  String getCurrency(){ return mCurrency;}

    public String getId(){
        return mId;
    }

    public  String getEmail(){
        return mEmail;
    }
}
