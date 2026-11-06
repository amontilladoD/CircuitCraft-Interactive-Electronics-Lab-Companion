package com.circuitcraft.app.ui;

import android.content.res.Configuration;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.circuitcraft.app.R;

/**
 * Parent of every screen that has a top bar: adds the sun / moon theme button.
 *   ☀ sun  = light mode is on  (tap -> dark)
 *   🌙 moon = dark mode is on   (tap -> light)
 */
public abstract class BaseActivity extends AppCompatActivity {

    private boolean isNight() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_theme, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_theme);
        if (item != null) {
            boolean night = isNight();
            item.setIcon(night ? R.drawable.ic_moon : R.drawable.ic_sun);
            item.setTitle(night ? "Dark mode - tap for light" : "Light mode - tap for dark");
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_theme) {
            ThemeHelper.setNight(this, !isNight());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
