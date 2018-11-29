package uk.ac.ed.inf.coinz;

public class LoanCardItem {
    private int mImageResource;
    private String mVal;
    private String mInterest;
    private String mRepayPeriod;

    public LoanCardItem(int currencyImage, String val,String interest, String repayPeriod){
        mImageResource = currencyImage;
        mVal = val;
        mInterest = interest;
        mRepayPeriod =  repayPeriod;
    }

    public int getCurrencyImageResource(){
        return mImageResource;
    }

    public String getValue(){
        return mVal;
    }

    public String getInterestRate(){
        return mInterest;
    }

    public String getRepayPeriod(){
        return mRepayPeriod;
    }
}
