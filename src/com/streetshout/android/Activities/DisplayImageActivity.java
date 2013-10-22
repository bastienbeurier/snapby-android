package com.streetshout.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import com.androidquery.AQuery;
import com.androidquery.callback.ImageOptions;
import com.streetshout.android.R;

public class DisplayImageActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_image);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        AQuery aq = new AQuery(this);

        findViewById(R.id.display_image_view);

        String imageUrl = getIntent().getStringExtra("image") + "--400";

        ImageOptions options = new ImageOptions();
        options.round = 20;

        aq.id(R.id.display_image_view).image(imageUrl, options);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
        return true;
    }
}
