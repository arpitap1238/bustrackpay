package com.example.studentapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationFragment extends Fragment {

    private RecyclerView rvNotifications;
    private View layoutEmpty;
    private NotifAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        rvNotifications = view.findViewById(R.id.rv_notifications);
        layoutEmpty = view.findViewById(R.id.layout_empty_notifications);
        
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotifAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        mDatabase = FirebaseDatabase.getInstance().getReference("notifications");
        loadNotifications();

        return view;
    }

    private void loadNotifications() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Notification notification = postSnapshot.getValue(Notification.class);
                    if (notification != null && (notification.target.equals("all") || notification.target.equals("students"))) {
                        notificationList.add(notification);
                    }
                }
                
                Collections.sort(notificationList, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                
                if (notificationList.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvNotifications.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvNotifications.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Notifications", "Failed to load notifications", error.toException());
            }
        });
    }

    class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.VH> {
        List<Notification> list;
        NotifAdapter(List<Notification> l) { list = l; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(getContext()).inflate(R.layout.item_notification, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Notification n = list.get(position);
            holder.t.setText(n.title);
            holder.d.setText(n.message);
            
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.tm.setText(sdf.format(new Date(n.timestamp)));
            
            // Set icon and bg based on title/content keywords if wanted, 
            // but for now we'll stick to the admin message content
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView t, d, tm; ImageView i;
            VH(View v) {
                super(v);
                t = v.findViewById(R.id.tv_notif_title);
                d = v.findViewById(R.id.tv_notif_desc);
                tm = v.findViewById(R.id.tv_notif_time);
                i = v.findViewById(R.id.iv_notif_icon);
            }
        }
    }
}
