package io.github.vejei.ratingbar.samples;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.github.vejei.ratingbar.StatisticsView;

public class SamplesFragment extends Fragment {
    private static final String KEY_LAYOUT_RES = "layout_res";
    private int layoutRes;

    public SamplesFragment() {}

    public static SamplesFragment newInstance(int layoutRes) {
        Bundle args = new Bundle();
        args.putInt(KEY_LAYOUT_RES, layoutRes);
        SamplesFragment fragment = new SamplesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            layoutRes = arguments.getInt(KEY_LAYOUT_RES);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(layoutRes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (layoutRes == R.layout.statistics_view_samples) {
            Button setDataButton = view.findViewById(R.id.button_set_data);
            StatisticsView statisticsView = view.findViewById(R.id.statistics_view);

            setDataButton.setOnClickListener((v) -> {
                statisticsView.feed(new float[] {1f, 0.9f, 0.3f, 0.2f, 0.005f});
            });
        }
    }
}
