package com.example.studentapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FeesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fees, container, false);
        
        // Sync with Firebase for real-time semester status
        android.content.SharedPreferences pref = getActivity().getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE);
        String enrollmentId = pref.getString("enrollmentId", "");

        TextView tvDueAmount = view.findViewById(R.id.tv_due_amount);
        TextView tvDuePeriod = view.findViewById(R.id.tv_due_period);
        Button btnPayNow = view.findViewById(R.id.btn_pay_now);

        // Transaction History Setup
        RecyclerView rv = view.findViewById(R.id.rv_transactions);
        List<Transaction> transactions = new ArrayList<>();
        TransactionAdapter adapter = new TransactionAdapter(transactions);
        rv.setAdapter(adapter);

        if (!enrollmentId.isEmpty()) {
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("students").child(enrollmentId)
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        
                        String feeSem1 = snapshot.child("feeSem1").getValue(String.class);
                        String feeSem2 = snapshot.child("feeSem2").getValue(String.class);
                        
                        boolean s1Paid = "Paid".equalsIgnoreCase(feeSem1);
                        boolean s2Paid = "Paid".equalsIgnoreCase(feeSem2);
                        
                        transactions.clear();
                        if (s1Paid) transactions.add(new Transaction("Semester 1 Fee", "Term 1", "₹7,500", "SUCCESS", "UTR-VERIFIED"));
                        if (s2Paid) transactions.add(new Transaction("Semester 2 Fee", "Term 2", "₹7,500", "SUCCESS", "UTR-VERIFIED"));
                        adapter.notifyDataSetChanged();

                        if (s1Paid && s2Paid) {
                            tvDueAmount.setText("₹0");
                            tvDuePeriod.setText("Full Year Paid");
                            btnPayNow.setVisibility(View.GONE);
                        } else if (s1Paid) {
                            tvDueAmount.setText("₹7,500");
                            tvDuePeriod.setText("Semester 2 Due");
                            btnPayNow.setVisibility(View.VISIBLE);
                        } else {
                            tvDueAmount.setText("₹7,500");
                            tvDuePeriod.setText("Semester 1 Due");
                            btnPayNow.setVisibility(View.VISIBLE);
                        }
                    }
                    @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                });
        }

        btnPayNow.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), PaymentActivity.class));
            if (getActivity() != null) getActivity().overridePendingTransition(R.anim.anim_slide_up_scale, R.anim.anim_fade_out_scale);
        });

        return view;
    }

    static class Transaction {
        String title, date, amount, status, inv;
        Transaction(String t, String d, String a, String s, String i) {
            title = t; date = d; amount = a; status = s; inv = i;
        }
    }

    class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {
        List<Transaction> list;
        TransactionAdapter(List<Transaction> l) { list = l; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(getContext()).inflate(R.layout.item_transaction, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int p) {
            Transaction t = list.get(p);
            h.t.setText(t.title);
            h.d.setText(t.date + " • " + t.inv);
            h.a.setText("+" + t.amount);
        }
        @Override public int getItemCount() { return list.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView t, d, a;
            VH(View v) { super(v); t = v.findViewById(R.id.tv_transaction_title); d = v.findViewById(R.id.tv_transaction_date); a = v.findViewById(R.id.tv_transaction_amount); }
        }
    }
}
