
package info.guardianproject.intheclear;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class InTheClearActivity extends Activity implements OnClickListener {

    ImageView logoPanic;
    GridView launchGrid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (prefs.getBoolean("IsVirginUser", true)) {
            AlertDialog.Builder ad = new AlertDialog.Builder(this);
            ad.setTitle(getResources().getString(R.string.KEY_PREF_LANGUAGE_TITLE));

            CharSequence[] langs = getResources().getStringArray(R.array.languages);
            ad.setItems(langs, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (setNewLocale(getResources().getStringArray(R.array.languages_values)[which]))
                        launchWizard();
                }
            });

            AlertDialog alert = ad.create();
            alert.show();
        }

        logoPanic = (ImageView) findViewById(R.id.logoPanic);
        logoPanic.setOnClickListener(this);

        launchGrid = (GridView) findViewById(R.id.launchGrid);
        launchGrid.setAdapter(new ImageAdapter(this));
        launchGrid.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                if (position == 0)
                    launchShout();
                else if (position == 1)
                    launchWipe();
                else if (position == 2)
                    launchWizard();
                else if (position == 3)
                    launchPreferences();
            }
        });
    }

    private void launchWizard() {
        Intent i = new Intent(this, WizardActivity.class);
        startActivity(i);
    }

    private void launchWipe() {
        Intent i = new Intent(this, WipeActivity.class);
        startActivity(i);
    }

    private void launchShout() {
        Intent i = new Intent(this, ShoutActivity.class);
        startActivity(i);
    }

    private void launchPanic() {
        Intent i = new Intent(this, PanicActivity.class).addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(i);
    }

    private void launchPreferences() {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    public boolean setNewLocale(String localeCode) {
        Configuration config = new Configuration();
        config.locale = new Locale(localeCode);
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
        Log.d(ITCConstants.Log.ITC, "current configuration = "
                + getBaseContext().getResources().getConfiguration().locale);

        return true;
    }

    @Override
    public void onClick(View v) {
        if (v == logoPanic)
            launchPanic();
    }

    public class ImageAdapter extends BaseAdapter
    {
        Context mContext;

        public ImageAdapter(Context c)
        {
            mContext = c;
        }

        @Override
        public int getCount()
        {
            return 4;
        }

        @SuppressLint("InflateParams")
		@Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View gridItem = convertView;

            if (convertView == null)
            {
                // Inflate the layout
                LayoutInflater li = getLayoutInflater();
                gridItem = li.inflate(R.layout.grid_item, null);

                // Add image & text
                TextView tv = (TextView) gridItem.findViewById(R.id.grid_item_text);
                ImageView iv = (ImageView) gridItem.findViewById(R.id.grid_item_image);
                gridItem.setId(position);

                switch (position) {
                    case 0:
                        tv.setText(getResources().getString(R.string.KEY_EMERGENCY_SMS_TITLE));
                        iv.setImageResource(R.drawable.btn_shout);
                        break;
                    case 1:
                        tv.setText(getResources().getString(R.string.KEY_WIPE_ACTIVITY_TITLE));
                        iv.setImageResource(R.drawable.btn_wipe);
                        break;
                    case 2:
                        tv.setText(getResources().getString(R.string.KEY_MAIN_TOWIZARD));
                        iv.setImageResource(R.drawable.btn_wizard);
                        break;
                    case 3:
                        tv.setText(getResources().getString(R.string.KEY_MAIN_TOPREFS));
                        iv.setImageResource(R.drawable.btn_settings);
                        break;
                }
            }
            return gridItem;
        }

        @Override
        public Object getItem(int arg0) {
            // TODO auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            // TODO auto-generated method stub
            return 0;
        }
    }
}
