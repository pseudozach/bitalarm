package com.pseudozach.bitalarm;

public class Transaction {
    private String mPrice;
    private String mDirection;
    private String mTo;
    private String mFrom;
    private long mTimestamp;

    public Transaction() {}  // Needed for Firebase

    public Transaction(String price, String direction, String to, String from, long timestamp) {
        mPrice = price;
        mDirection = direction;
        mTo = to;
        mFrom = from;
        mTimestamp = timestamp;
    }

    public String getPrice() { return mPrice; }

    public void setPrice(String price) { mPrice = price; }

    public String getDirection() { return mDirection; }

    public void setDirection(String direction) { mDirection = direction; }

    public String getTo() { return mTo; }

    public void setTo(String to) { mTo = to; }

    public String getFrom() { return mFrom; }

    public void setFrom(String from) { mFrom = from; }

    public long getTimestamp() { return mTimestamp; }

    public void setTimestamp(long timestamp) { mTimestamp =  timestamp; }
}
