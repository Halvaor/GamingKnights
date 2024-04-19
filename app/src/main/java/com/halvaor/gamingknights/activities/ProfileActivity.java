package com.halvaor.gamingknights.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.halvaor.gamingknights.databinding.ActivityProfileBinding;
import com.halvaor.gamingknights.domain.id.UserID;

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
        this.auth = FirebaseAuth.getInstance();
        this.database = FirebaseFirestore.getInstance();
        this.userID = new UserID(auth.getUid());

        fetchUserData(binding);
        bindListeners(binding);
    }

    private void bindListeners(ActivityProfileBinding binding) {
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
        DocumentReference userRef = database.collection("User").document(this.userID.getId());
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
                    Log.d(TAG, "No such document in collection \"User\" : " + this.userID.getId());
                }
            } else {
                Log.d(TAG, "Failed to retrieve User ", task.getException());
            }
        });
    }

    private void updateProfile(Map<String, Object> updateInformation) {
        Runnable runnable = () -> {
            DocumentReference userRef = database.collection("User").document(this.userID.getId());
            userRef.update(updateInformation)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User successfully updated!");
                        Toast.makeText(this, "Profiländerung erfolgreich", Toast.LENGTH_SHORT).show();
                        updateProfileInUpcomingGameNights(updateInformation);
                    })
                    .addOnFailureListener(exception -> {
                        Log.w(TAG, "Error updating User", exception);
                        Toast.makeText(this, "Profiländerung fehlgeschlagen", Toast.LENGTH_SHORT).show();
                    });
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void updateProfileInUpcomingGameNights(Map<String, Object> updateInformation) {
        Query query = database.collection("GameNight")
                .whereGreaterThan("DateTime", Timestamp.now())
                .whereEqualTo("Host.UserID", this.userID.getId());

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Successfully fetched upcoming GameNights where Host is " + this.userID.getId());
                if (task.getResult().isEmpty()) {
                    Log.d(TAG, "Could not find upcoming GameNights where Host is " + this.userID.getId());
                } else {
                    Map<String, Object> hostData = new HashMap<>();

                    hostData.put("Host.FirstName", updateInformation.get("FirstName"));
                    hostData.put("Host.LastName", updateInformation.get("LastName"));
                    hostData.put("Host.Street", updateInformation.get("Street"));
                    hostData.put("Host.HouseNumber", updateInformation.get("HouseNumber"));
                    hostData.put("Host.PostalCode", updateInformation.get("PostalCode"));
                    hostData.put("Host.Town", updateInformation.get("Town"));

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        DocumentReference gameNightRef = database.collection("GameNight").document(document.getId());

                        gameNightRef.update(hostData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Successfully updated HostData in GameNight: " + document.getId());
                                })
                                .addOnFailureListener(exception -> {
                                    Log.e(TAG, "Failed to updated HostData in GameNight: " + document.getId(), exception);
                                    Toast.makeText(this, "Profiländerung fehlgeschlagen", Toast.LENGTH_SHORT).show();
                                });
                    }
                }
            } else {
                Log.d(TAG, "Failed to fetched upcoming GameNights where Host is " + this.userID.getId());
            }
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
