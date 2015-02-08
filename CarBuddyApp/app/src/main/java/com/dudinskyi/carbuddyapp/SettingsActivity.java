package com.dudinskyi.carbuddyapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Settings activity
 *
 * @author Oleksandr Dudinskyi (dudinskyj@gmail.com)
 */
public class SettingsActivity extends Activity {
    private SeekBar mRSSISeekBar;
    private TextView mSignalStrength;
    private SeekBar mUpdateFrSeekBar;
    private TextView mUpdateFrequency;
    private Button mGoBackBtn;
    private final String signalStrengthText = getResources().getString(R.string.signal_strength);
    private final String frequencyUpdateText = getResources().getString(R.string.update_fr);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        mRSSISeekBar = (SeekBar) findViewById(R.id.rssi_seek_bar);
        mSignalStrength = (TextView) findViewById(R.id.signal_strength);
        mUpdateFrSeekBar = (SeekBar) findViewById(R.id.update_fr_seek_bar);
        mUpdateFrequency = (TextView) findViewById(R.id.update_frequency);
        mGoBackBtn = (Button) findViewById(R.id.go_back_btn);
        mSignalStrength.setText(signalStrengthText + " " + 40);
        mUpdateFrequency.setText(frequencyUpdateText + " " + 10);
        setupInitValues();
        mRSSISeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                progress = progressValue + 60;
                mSignalStrength.setText(signalStrengthText + " " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mUpdateFrSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                progress = progressValue + 10;
                mUpdateFrequency.setText(frequencyUpdateText + " " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mGoBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult();
                finish();
            }
        });
    }

    private void setResult() {
        Intent goBackIntent = new Intent();
        goBackIntent.putExtra(Constants.EXTRA_SETTINGS_RSSI, mRSSISeekBar.getProgress() + 40);
        goBackIntent.putExtra(Constants.EXTRA_SETTINGS_UPDATE_TIME, mUpdateFrSeekBar.getProgress() + 10);
        setResult(RESULT_OK, goBackIntent);
    }

    private void setupInitValues() {
        int rssi = getIntent().getExtras().getInt(Constants.EXTRA_SETTINGS_RSSI, 0);
        mRSSISeekBar.setProgress(rssi - 60);
        mSignalStrength.setText(signalStrengthText + " " + rssi);
        int updateTime = getIntent().getExtras().getInt(Constants.EXTRA_SETTINGS_UPDATE_TIME, 0);
        mUpdateFrSeekBar.setProgress(updateTime - 10);
        mUpdateFrequency.setText(frequencyUpdateText + " " + updateTime);
    }

    @Override
    public void onBackPressed() {
        setResult();
        super.onBackPressed();
    }
}
