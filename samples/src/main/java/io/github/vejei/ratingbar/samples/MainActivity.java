package io.github.vejei.ratingbar.samples;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import static io.github.vejei.ratingbar.samples.SamplesActivity.NAME_SAMPLE_INDEX;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onTextClick(View view) {
        int id = view.getId();
        Intent intent = null;
        if (id == R.id.text_rating_bar_samples) {
            intent = new Intent(MainActivity.this, SamplesActivity.class);
            intent.putExtra(NAME_SAMPLE_INDEX, 0);
        } else if (id == R.id.text_statistics_view_samples) {
            intent = new Intent(MainActivity.this, SamplesActivity.class);
            intent.putExtra(NAME_SAMPLE_INDEX, 1);
        }
        if (intent != null) {
            startActivity(intent);
        }
    }
}