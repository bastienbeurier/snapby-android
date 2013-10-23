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

        String imageUrl = getIntent().getStringExtra("image") + "--400";

        aq.id(R.id.display_image_view_place_holder).image(R.drawable.shout_image_place_holder_square);
        aq.id(R.id.display_image_view).image(imageUrl);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
        return true;
    }
}
