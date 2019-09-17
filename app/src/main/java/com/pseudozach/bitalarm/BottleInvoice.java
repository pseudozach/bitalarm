package com.pseudozach.bitalarm;


/*{
        "claimed": false,
        "messageAuthHash": "DpOe6hXs47GtPOfzzgeq75kRkIUMAkQ0nSzwUaAWOa4=",
        "paymentRequest": "lnbc1pwkw7ulpp50psqrlkgfws2sky879tuuuf9epd65mjpwf2j9twvykm500mdhemqdpu2dhkx6tpdss8qcted4jkuapqw3hjqctpde6x7mn0wqsx7m3qw3mkjar5v4eqcqrpzvhm05xky2ap6rf5vydpjut97wuz2hg2qmypeflsg6eznjg596s0mxhm5sucgeep2m0caa9cmmgr9m76gqer3u3m0q2pt0gpmc3px880sqd8czqn",
        "pubsubAuth": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJwYXk6cHMiLCJleHAiOjE1NjcwNjMwNjMsImp0aSI6ImJjY2U3ZjBiNDNkYjJmMDgiLCJjIjpbInByaXZhdGUtaW52b2ljZS1zLTc4NjAwMWZlYzg0YmEwYTg1ODg3ZjE1N2NlNzEyNWM4NWJhYTZlNDE3MjU1MjJhZGNjMjViNzQ3YmY2ZGJlNzYiXX0.fdKPLcslSMMCy3dAN5mdlU4xLLi6q611h-bZfsa0CHs",
        "pubsubChannel": "private-invoice-s-786001fec84ba0a85887f157ce7125c85baa6e41725522adcc25b747bf6dbe76",
        "pubsubKey": "sub-c-6f445254-7734-11e9-90ac-5a6b801e0231"
        }
        */
public class BottleInvoice {
    boolean claimed;
    String messageAuthHash;
    String paymentRequest;
    String pubsubAuth;
    String pubsubChannel;
    String pubsubKey;

    public BottleInvoice(boolean claimed,String messageAuthHash,String paymentRequest,String pubsubAuth,String pubsubChannel,String pubsubKey){
        this.claimed = claimed;
        this.messageAuthHash = messageAuthHash;
        this.paymentRequest = paymentRequest;
        this.pubsubAuth = pubsubAuth;
        this.pubsubChannel = pubsubChannel;
        this.pubsubKey = pubsubKey;
    }

    public String getMessageAuthHash(){
        return messageAuthHash;
    }

    public boolean getClaimed() {
        return claimed;
    }

    public String getPaymentRequest() {
        return paymentRequest;
    }


}
