package com.halvaor.gamingknights;

import android.app.Activity;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.halvaor.gamingknights.databinding.ActivityLoginBinding;
import com.halvaor.gamingknights.databinding.ActivityRegisterBinding;

public class RegisterActivity extends Activity {

    private static final String TAG = "RegisterActivity";
    private FirebaseAuth auth;
    private ActivityRegisterBinding binding;

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        auth = FirebaseAuth.getInstance();
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    public void createAccount(String email, String password) {

    }

}
