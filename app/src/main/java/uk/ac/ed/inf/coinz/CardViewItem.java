package uk.ac.ed.inf.coinz;

public class CardViewItem {
    private int mImageResource;
    private String mText1;
    private String mText2;
    private String mFrom;

    public CardViewItem(int imageResource, String currency,String value, String from){
        mImageResource = imageResource;
        mText1 = currency;
        mText2 = value;
        mFrom = from;
    }

    public int getImageResource(){
        return mImageResource;
    }

    public String getText1(){
        return mText1;
    }

    public String getText2(){
        return mText2;
    }

    public String getFrom() {
        return mFrom;
    }
}
