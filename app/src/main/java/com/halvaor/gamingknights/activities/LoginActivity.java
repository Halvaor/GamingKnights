package com.halvaor.gamingknights.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.halvaor.gamingknights.databinding.ActivityLoginBinding;

import java.util.Optional;

public class LoginActivity extends Activity {

    private static final String TAG = "LoginActivity";
    private FirebaseAuth auth;

    private ActivityLoginBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonLogin.setOnClickListener(view -> {
            try {
                String email = Optional.ofNullable(binding.loginInputEmail.getText().toString()).orElse("");
                String password = Optional.ofNullable(binding.loginInputPassword.getText().toString()).orElse("");
                login(email, password);
            }catch (Exception exception) {
                Log.e(TAG, "Login failed:", exception);
            }
        });

        binding.loginButtonSignIn.setOnClickListener(view -> {
            Intent registerActivityIntent = new Intent(this, RegisterActivity.class);
            startActivity(registerActivityIntent);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null) {
            loadDashboardActivity();
        }
    }

    private void loadDashboardActivity() {
        //ToDo
    }

    private void login(String email, String password) throws Exception {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Login successful");
                        FirebaseUser user = auth.getCurrentUser();
                        //ToDo updateUI(user);
                    } else {
                        Log.w(TAG, "Login failed", task.getException());
                        Toast.makeText(LoginActivity.this, "Login fehlgeschlagen", Toast.LENGTH_SHORT).show();
                        //ToDo updateUI(null);
                    }
                }
            });
        //ToDo weiterleitung bei erfolgreichem/ erfolglosem loginversuch fehlt noch
    }

}
