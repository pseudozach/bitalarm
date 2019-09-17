package com.pseudozach.bitalarm;

public class invoice {
    String payreq;
    boolean paid;
    String amount;

    public invoice(String payreq, boolean paid, String amount){
        this.payreq = payreq;
        this.paid = paid;
        this.amount = amount;
    }

    public String getPayreq(){
        return payreq;
    }

    public boolean isPaid() {
        return paid;
    }

    public String getAmount() {
        return amount;
    }
}
