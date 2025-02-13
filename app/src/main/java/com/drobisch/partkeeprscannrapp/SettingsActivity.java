package com.drobisch.partkeeprscannrapp;

import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {

    private AutoCompleteTextView mServerView;
    private Switch mAutoLoginSwitch;
    private TextView mBarcodeTemplateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ActionBar actionBar = getSupportActionBar();
        SharedPreferences prefs = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);

        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mServerView = (AutoCompleteTextView) findViewById(R.id.server);
        mAutoLoginSwitch = (Switch) findViewById(R.id.auto_login_switch);
        mBarcodeTemplateView = (TextView) findViewById(R.id.barcode_template);
        Button mSaveSettingsButton = (Button) findViewById(R.id.save_settings_button);
        mSaveSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSettings();
            }
        });

        mServerView.setText(prefs.getString(getString(R.string.pref_key_server), getString(R.string.pref_default_server)));
        mAutoLoginSwitch.setChecked(Boolean.parseBoolean(prefs.getString(getString(R.string.pref_auto_login), getString(R.string.pref_default_auto_login))));
        mBarcodeTemplateView.setText(prefs.getString(getString(R.string.pref_barcode_template), getString(R.string.pref_default_barcode_template)));
    }

    public void saveSettings() {
        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE).edit();
        editor.putString(getString(R.string.pref_key_server), mServerView.getText().toString());
        editor.putString(getString(R.string.pref_auto_login), Boolean.toString(mAutoLoginSwitch.isChecked()));
        editor.putString(getString(R.string.pref_barcode_template), mBarcodeTemplateView.getText().toString());
        editor.commit();
        finish();
    }

    public boolean onOptionsItemSelected(MenuItem item){
        finish();
        return true;

    }
}
