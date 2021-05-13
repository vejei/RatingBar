package io.github.vejei.ratingbar.samples;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class SamplesActivity extends AppCompatActivity {
    static final String NAME_SAMPLE_INDEX = "sample_index";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_samples);

        Intent intent = getIntent();
        int sampleIndex = intent.getIntExtra(NAME_SAMPLE_INDEX, 0);

        int layoutRes = -1;
        switch (sampleIndex) {
            case 0:
                layoutRes = R.layout.rating_bar_samples;
                break;
            case 1:
                layoutRes = R.layout.statistics_view_samples;
                break;
        }

        if (layoutRes != -1 && (savedInstanceState == null)) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, SamplesFragment.newInstance(layoutRes))
                    .commit();
        }
    }
}