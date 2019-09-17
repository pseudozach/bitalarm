package com.pseudozach.bitalarm;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.pseudozach.bitalarm.alarms.TransactionHolder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;

public class FreshWalletFragment extends Fragment {

    @BindView(R.id.fab)
    FloatingActionButton mFab;

    @BindView(R.id.list)
    RecyclerView mList;

    private FirebaseAuth mAuth;
    FirebaseRecyclerAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recycler_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

/*        mFab = getActivity().findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //implement add sats to wallet
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("How much do you want to fund the wallet? (In Satoshis)");

// Set up the input
                final EditText input = new EditText(getContext());
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text | InputType.TYPE_TEXT_VARIATION_PASSWORD
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setHint("1000");
                builder.setView(input);

// Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String m_Text = input.getText().toString();
                        Log.e("WF", "got input now go to invoice activity: " + m_Text);
                        Intent i = new Intent(getActivity(), InvoiceActivity.class);
                        i.putExtra("amount", m_Text);
                        startActivity(i);

                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });*/

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser() != null){

            Query query = FirebaseDatabase.getInstance()
                    .getReference()
                    .child("bitalarm")
                    .child("transactions")
                    .child(mAuth.getCurrentUser().getUid())
                    .limitToLast(50);

            FirebaseRecyclerOptions<Transaction> options =
                    new FirebaseRecyclerOptions.Builder<Transaction>()
                            .setQuery(query, Transaction.class)
                            .build();

            adapter = new FirebaseRecyclerAdapter<Transaction, TransactionHolder>(options) {
                @Override
                public TransactionHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    // Create a new instance of the ViewHolder, in this case we are using a custom
                    // layout called R.layout.message for each item
                    View view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.message, parent, false);
                    return new TransactionHolder(view);
                }

                @Override
                protected void onBindViewHolder(TransactionHolder holder, int position, Transaction transaction) {
                    //Log.e("FWF", "got a tx:: " + transaction.getPrice());
                    //Log.e("FWF", "got a tx:: " + transaction.getDirection() + ", " + transaction.getTo());
                    // Bind the Chat object to the ChatHolder
                    // ...
                    holder.bind(transaction);
                }

                @Override
                public void onDataChanged() {
                    // If there are no chat messages, show a view that invites the user to add a message.
                    //mEmptyListMessage.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
                    //Log.e("FWF", "datachanged somehow!!");
                }
            };

            mList = view.findViewById(R.id.list);
            mList.setLayoutManager(new LinearLayoutManager(getContext()));
            mList.setAdapter(adapter);
            adapter.startListening();

        } else {
            Log.e("FWF", "user not logged in...");
        }



    }

    @Override
    public void onStart() {
        super.onStart();
        if(adapter != null){
            adapter.startListening();
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        if(adapter != null) {
            adapter.stopListening();
        }
    }
}
