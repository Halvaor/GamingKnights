package com.halvaor.gamingknights.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.halvaor.gamingknights.databinding.ActivityDashboardBinding;
import com.halvaor.gamingknights.util.UserID;

public class DashboardActivity extends Activity {

    private static final String TAG = "DashboardActivity";
    private ActivityDashboardBinding binding;
    private UserID userID;
    private FirebaseFirestore database;
    private FirebaseAuth auth;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        userID = (UserID) getIntent().getExtras().getSerializable("userID");

        database = FirebaseFirestore.getInstance();
        DocumentReference docRef = database.collection("User").document(userID.getId());
        docRef.get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Log.d(TAG, "User data: " + document.getData());
                    String firstname = String.valueOf(document.get("FirstName"));
                    String lastname = String.valueOf(document.get("LastName"));
                    binding.dashboardToolbarNameView.setText(firstname + " " + lastname + " ");
                } else {
                    Log.d(TAG, "No such document in collection \"User\" : " + userID.getId());
                }
            } else {
                Log.d(TAG, "Failed to retrieve User ", task.getException());
            }
        });

        binding.dashboardCardView.setOnClickListener(view -> {
            Toast.makeText(this, "Knopf gedrückt", Toast.LENGTH_SHORT).show();
        });

    }
}
