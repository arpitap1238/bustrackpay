package com.example.studentapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SupportFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_support, container, false);

        setupFaqToggle(view, R.id.faq_card_1, R.id.tv_answer_1, R.id.iv_arrow_1);
        setupFaqToggle(view, R.id.faq_card_2, R.id.tv_answer_2, R.id.iv_arrow_2);
        setupFaqToggle(view, R.id.faq_card_3, R.id.tv_answer_3, R.id.iv_arrow_3);

        return view;
    }

    private void setupFaqToggle(View parent, int cardId, int answerId, int arrowId) {
        View card = parent.findViewById(cardId);
        TextView answer = parent.findViewById(answerId);
        ImageView arrow = parent.findViewById(arrowId);

        if (card != null && answer != null && arrow != null) {
            card.setOnClickListener(v -> {
                boolean isExpanded = answer.getVisibility() == View.VISIBLE;
                answer.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                arrow.animate().rotation(isExpanded ? 0 : 90).setDuration(200).start();
            });
        }
    }
}
