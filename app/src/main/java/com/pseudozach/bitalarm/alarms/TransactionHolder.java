package com.pseudozach.bitalarm.alarms;

import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RotateDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pseudozach.bitalarm.R;
import com.pseudozach.bitalarm.Transaction;
import com.pseudozach.bitalarm.list.BaseViewHolder;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class TransactionHolder extends RecyclerView.ViewHolder {
    private final TextView mNameField;
    private final TextView mTextField;
    private final TextView mTimestamp;
    private final FrameLayout mLeftArrow;
    private final FrameLayout mRightArrow;
    private final RelativeLayout mMessageContainer;
    private final LinearLayout mMessage;
    private final int mGreen300;
    private final int mGray300;

    public TransactionHolder(@NonNull View itemView) {
        super(itemView);
        mNameField = itemView.findViewById(R.id.name_text);
        mTextField = itemView.findViewById(R.id.message_text);
        mTimestamp = itemView.findViewById(R.id.timestamp);
        mLeftArrow = itemView.findViewById(R.id.left_arrow);
        mRightArrow = itemView.findViewById(R.id.right_arrow);
        mMessageContainer = itemView.findViewById(R.id.message_container);
        mMessage = itemView.findViewById(R.id.message);
        mGreen300 = ContextCompat.getColor(itemView.getContext(), R.color.bsp_dark_gray);
        mGray300 = ContextCompat.getColor(itemView.getContext(), R.color.light_gray);
    }

    public void bind(@NonNull Transaction transaction) {
        if(transaction.getDirection().equals("out")){
            mLeftArrow.setBackgroundResource(R.drawable.ic_remove_white_24dp);
        }
        /*else {
            mLeftArrow.setBackgroundResource(R.drawable.ic_add_24dp);
        }*/

        setName(transaction.getPrice() + " Sats");
        //transaction.getFrom()
        setMessage("To " + transaction.getTo());

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.getDefault());
        long enough = (transaction.getTimestamp());
        Date netDate = (new Date(enough));
        //Log.e("TH", "enough, netdate, transaction.getTimestamp(): " + String.valueOf(enough) + ", " + netDate + ", " + transaction.getTimestamp());
        mTimestamp.setText(sdf.format(netDate));

        //1567151225802
        //1350574775

        //FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        //setIsSender(currentUser != null && transaction.().equals(currentUser.getUid()));
    }

    private void setName(@Nullable String name) {
        mNameField.setText(name);
    }

    private void setMessage(@Nullable String text) {
        mTextField.setText(text);
    }

    private void setIsSender(boolean isSender) {
        final int color;
        if (isSender) {
            color = mGreen300;
            mLeftArrow.setVisibility(View.GONE);
            mRightArrow.setVisibility(View.VISIBLE);
            mMessageContainer.setGravity(Gravity.END);
        } else {
            color = mGray300;
            mLeftArrow.setVisibility(View.VISIBLE);
            mRightArrow.setVisibility(View.GONE);
            mMessageContainer.setGravity(Gravity.START);
        }

        ((GradientDrawable) mMessage.getBackground()).setColor(color);
        ((RotateDrawable) mLeftArrow.getBackground()).getDrawable()
                .setColorFilter(color, PorterDuff.Mode.SRC);
        ((RotateDrawable) mRightArrow.getBackground()).getDrawable()
                .setColorFilter(color, PorterDuff.Mode.SRC);
    }
}