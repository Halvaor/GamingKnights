package com.halvaor.gamingknights.activities;

import android.app.Activity;
import android.os.Bundle;

import com.halvaor.gamingknights.databinding.ActivityGameNightBinding;

public class GameNightActivity extends Activity {

    ActivityGameNightBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.binding = ActivityGameNightBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}
