package com.example.driverapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.example.driverapp.R;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private List<Student> studentList;

    public StudentAdapter(List<Student> studentList) {
        this.studentList = studentList;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = studentList.get(position);
        holder.tvName.setText(student.name);
        holder.tvEnrollment.setText("ID: " + student.enrollmentId);

        // Procedural Avatar
        if (student.name != null && !student.name.isEmpty()) {
            holder.ivAvatar.setText(String.valueOf(student.name.charAt(0)).toUpperCase());
            int[] colors = {0xFF3B82F6, 0xFF10B981, 0xFFF59E0B, 0xFFEF4444, 0xFF8B5CF6, 0xFFEC4899};
            int colorIndex = Math.abs(student.name.hashCode()) % colors.length;
            holder.ivAvatar.getBackground().setTint(colors[colorIndex]);
        }

        if ("Pending".equalsIgnoreCase(student.feeStatus)) {
            holder.badgeFee.setText("Pending Fees");
            holder.badgeFee.setTextColor(Color.parseColor("#991B1B"));
            holder.badgeFee.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
        } else {
            holder.badgeFee.setText("Fees Paid");
            holder.badgeFee.setTextColor(Color.parseColor("#065F46"));
            holder.badgeFee.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D1FAE5")));
        }

        if ("Valid".equalsIgnoreCase(student.passStatus)) {
            holder.badgePass.setText("Valid Pass");
            holder.badgePass.setTextColor(Color.parseColor("#1E40AF"));
            holder.badgePass.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DBEAFE")));
        } else {
            holder.badgePass.setText("Expired Pass");
            holder.badgePass.setTextColor(Color.parseColor("#92400E"));
            holder.badgePass.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
        }

        holder.itemView.setOnClickListener(v -> showStudentDetails(v.getContext(), student));
    }

    private void showStudentDetails(android.content.Context context, Student student) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(context).inflate(R.layout.layout_student_detail, null);

        TextView tvAvatar = view.findViewById(R.id.detail_avatar_text);
        if (tvAvatar != null && student.name != null && !student.name.isEmpty()) {
            tvAvatar.setText(String.valueOf(student.name.charAt(0)).toUpperCase());
            int[] colors = {0xFF3B82F6, 0xFF10B981, 0xFFF59E0B, 0xFFEF4444, 0xFF8B5CF6, 0xFFEC4899};
            int colorIndex = Math.abs(student.name.hashCode()) % colors.length;
            view.findViewById(R.id.detail_avatar_card).setBackgroundTintList(android.content.res.ColorStateList.valueOf(colors[colorIndex]));
        }

        ((TextView) view.findViewById(R.id.detail_student_name)).setText(student.name);
        ((TextView) view.findViewById(R.id.detail_enrollment)).setText("Enrollment ID: " + student.enrollmentId);
        ((TextView) view.findViewById(R.id.detail_email)).setText(student.email != null ? student.email : "Not Assigned");
        ((TextView) view.findViewById(R.id.detail_phone)).setText(student.phone != null ? student.phone : "Not Assigned");
        ((TextView) view.findViewById(R.id.detail_stop)).setText("Boarding: " + (student.pickupStop != null ? student.pickupStop : "N/A"));
        ((TextView) view.findViewById(R.id.detail_bus_no)).setText("Assigned Bus: #" + student.busNo);

        TextView tvSem1 = view.findViewById(R.id.tv_sem1_status);
        TextView tvSem2 = view.findViewById(R.id.tv_sem2_status);

        if ("Paid".equalsIgnoreCase(student.feeSem1)) {
            tvSem1.setText("PAID");
            tvSem1.setTextColor(Color.parseColor("#059669"));
        } else {
            tvSem1.setText("PENDING");
            tvSem1.setTextColor(Color.parseColor("#DC2626"));
        }

        if ("Paid".equalsIgnoreCase(student.feeSem2)) {
            tvSem2.setText("PAID");
            tvSem2.setTextColor(Color.parseColor("#059669"));
        } else {
            tvSem2.setText("PENDING");
            tvSem2.setTextColor(Color.parseColor("#DC2626"));
        }

        view.findViewById(R.id.btn_close_details).setOnClickListener(v -> bottomSheet.dismiss());

        bottomSheet.setContentView(view);
        bottomSheet.show();
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEnrollment, badgeFee, badgePass, ivAvatar;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_student_name);
            tvEnrollment = itemView.findViewById(R.id.tv_enrollment);
            badgeFee = itemView.findViewById(R.id.badge_fee_status);
            badgePass = itemView.findViewById(R.id.badge_pass_status);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
        }
    }
}
