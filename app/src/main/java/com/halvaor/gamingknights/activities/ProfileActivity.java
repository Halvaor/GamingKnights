package com.halvaor.gamingknights.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.halvaor.gamingknights.domain.id.UserID;
import com.halvaor.gamingknights.databinding.ActivityProfileBinding;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ProfileActivity extends Activity {

   private FirebaseAuth auth;
   private FirebaseFirestore database;
   private UserID userID;
   private static final String TAG = "ProfileActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityProfileBinding binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();
        userID = new UserID(auth.getUid());

        fetchUserData(binding);

        binding.profileCancelButton.setOnClickListener(view -> {
            Intent dashboardActivityIntent = new Intent(this, DashboardActivity.class);
            startActivity(dashboardActivityIntent);
        });

        binding.profileLogoutButton.setOnClickListener(view -> {
            Intent loginActivityIntent = new Intent(this, LoginActivity.class);
            startActivity(loginActivityIntent);
            auth.signOut();
        });

        binding.profileSaveButton.setOnClickListener(view -> {
            try {
                updateProfile(validateInput(fetchInput(binding)));
            }catch(Exception exception) {
                Log.e(TAG, "Error updating User", exception);
                Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUserData(ActivityProfileBinding binding) {
        DocumentReference userRef = database.collection("User").document(userID.getId());
        userRef.get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if(document.exists()) {
                    Log.d(TAG, "User data: " + document.getData());

                    binding.profileFirstnameValue.setText(document.getString("FirstName"));
                    binding.profileLastnameValue.setText(document.getString("LastName"));
                    binding.profileHouseNumberValue.setText(document.getString("HouseNumber"));
                    binding.profileStreetValue.setText(document.getString("Street"));
                    binding.profileTownValue.setText(document.getString("Town"));
                    binding.profilePostalcodeValue.setText(document.getString("PostalCode"));
                    binding.profileEMailValue.setText(document.getString("Email"));
                } else {
                    Log.d(TAG, "No such document in collection \"User\" : " + userID.getId());
                }
            } else {
                Log.d(TAG, "Failed to retrieve User ", task.getException());
            }
        });
    }

    private void updateProfile(Map<String, Object> updateInformation) {
        DocumentReference userRef = database.collection("User").document(userID.getId());
        userRef.update(updateInformation)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User successfully updated!");
        })
                .addOnFailureListener(exception -> {
                    Log.w(TAG, "Error updating User", exception);
                    Toast.makeText(this, "Profiländerung fehlgeschlagen", Toast.LENGTH_SHORT).show();
                });
    }

    private Map<String, Object> fetchInput(ActivityProfileBinding binding) {
        Map<String, Object> inputs = new HashMap<>();

        inputs.put("FirstName", Optional.of(String.valueOf(binding.profileFirstnameValue.getText())).orElse(""));
        inputs.put("LastName", Optional.of(String.valueOf(binding.profileLastnameValue.getText())).orElse(""));
        inputs.put("Street", Optional.of(String.valueOf(binding.profileStreetValue.getText())).orElse(""));
        inputs.put("HouseNumber", Optional.of(String.valueOf(binding.profileHouseNumberValue.getText())).orElse(""));
        inputs.put("PostalCode", Optional.of(String.valueOf(binding.profilePostalcodeValue.getText())).orElse(""));
        inputs.put("Town", Optional.of(String.valueOf(binding.profileTownValue.getText())).orElse(""));

        return inputs;
    }

    private Map<String, Object> validateInput(Map<String, Object> inputs) throws IllegalArgumentException {
        if(Objects.equals(inputs.get("FirstName"), "")) throw new IllegalArgumentException("Firstname can not be empty");
        if(Objects.equals(inputs.get("LastName"), "")) throw new IllegalArgumentException("Lastname can not be empty");
        if(Objects.equals(inputs.get("Street"), "")) throw new IllegalArgumentException("Street can not be empty");
        if(Objects.equals(inputs.get("HouseNumber"), "")) throw new IllegalArgumentException("House number can not be empty");
        if(Objects.equals(inputs.get("PostalCode"), "")) throw new IllegalArgumentException("Postal code can not be empty");
        if(Objects.equals(inputs.get("Town"), "")) throw new IllegalArgumentException("Town can not be empty");

        return inputs;
    }
}
