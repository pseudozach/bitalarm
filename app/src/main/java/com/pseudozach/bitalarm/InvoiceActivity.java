package com.pseudozach.bitalarm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;

public class InvoiceActivity extends Activity {

    TextView invoicetv;
    ImageView qrcode;
    TextView fundheadertv;
    Button launchwalletbutton;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.invoice_activity);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        String amount = getIntent().getExtras().getString("amount");

        invoicetv = findViewById(R.id.invoicetv);
        qrcode = findViewById(R.id.qrcode);

        fundheadertv = findViewById(R.id.fundheadertv);
        launchwalletbutton = findViewById(R.id.launchwalletbutton);

        if(mAuth.getCurrentUser() != null){
            generateInvoice(amount);
        } else {
            Snackbar.make(invoicetv, "Error generating invoice. Try again later.", Snackbar.LENGTH_LONG).show();
        }


    }


    private void generateInvoice(String amount){

        Snackbar.make(invoicetv, "Generating invoice, please wait...", Snackbar.LENGTH_LONG).show();
        String userId = mAuth.getCurrentUser().getUid();
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://aqueous-fjord-19834.herokuapp.com/bitalarminvoice?gamename=bitalarm&userId="+userId+"&chargeamount="+amount;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.e("IA", response);
                        if(response.contains("OK")){
                            String invoiceId = response.split(",")[1];
                            Log.e("IA", "all good, invoice: " + invoiceId);

                            //populate invoicetv
                            mDatabase.child("lnorders").child(invoiceId).child("invoice").child("lightning_invoice").child("payreq").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.exists()){
                                        String payreq = dataSnapshot.getValue().toString();
                                        Log.e("IA", "got payreq: " + payreq);
                                        invoicetv.setText(payreq);
                                        fundheadertv.setText("Fund Alarm Wallet - " + amount + " Satoshis");

                                        launchwalletbutton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                String url = "lightning:" + payreq;
                                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                                startActivity(intent);
                                            }
                                        });

                                        Bitmap bitmap = null;
                                        try {
                                            bitmap = textToImage(payreq, qrcode.getWidth(), qrcode.getHeight());
                                        } catch (WriterException e) {
                                            e.printStackTrace();
                                        }
                                        qrcode.setImageBitmap(bitmap);
                                    } else {
                                        Log.e("IA", "no payreq from invoice");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Log.e("IA", "payreq cancelled!?!");
                                }
                            });

                            //listen to payment!
                            mDatabase.child("lnorders").child(invoiceId).child("payment_paid").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.exists() && (boolean) dataSnapshot.getValue()){
                                        Log.e("IA", "invoice paid!");
                                        //updateUI
                                        Snackbar.make(invoicetv, "Invoice Paid.", Snackbar.LENGTH_SHORT).show();
                                        //finish();

                                        Handler handler = new Handler();
                                        handler.postDelayed(new Runnable() {
                                            public void run() {
                                                finish();
                                            }
                                        }, 1000);

                                    } else {
                                        Log.e("IA", "not paid!");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                        }
                        // Display the first 500 characters of the response string.
                        //invoicetv.setText("Response is: "+ response.substring(0,500));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("IA", "nope: " + error.getMessage());
                //textView.setText("That didn't work!");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private Bitmap textToImage(String text, int width, int height) throws WriterException, NullPointerException {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.DATA_MATRIX.QR_CODE,
                    width, height, null);
        } catch (IllegalArgumentException Illegalargumentexception) {
            return null;
        }

        int bitMatrixWidth = bitMatrix.getWidth();
        int bitMatrixHeight = bitMatrix.getHeight();
        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

        int colorWhite = 0xFFFFFFFF;
        int colorBlack = 0xFF000000;

        for (int y = 0; y < bitMatrixHeight; y++) {
            int offset = y * bitMatrixWidth;
            for (int x = 0; x < bitMatrixWidth; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? colorBlack : colorWhite;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444);

        bitmap.setPixels(pixels, 0, width, 0, 0, bitMatrixWidth, bitMatrixHeight);
        return bitmap;
    }
}
