package com.halvaor.gamingknights.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.halvaor.gamingknights.R;
import com.halvaor.gamingknights.databinding.ActivitySigninBinding;
import com.halvaor.gamingknights.domain.id.UserID;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SignInActivity extends Activity {

    private static final String TAG = "signInActivity";
    private FirebaseAuth auth;
    private ActivitySigninBinding binding;

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        auth = FirebaseAuth.getInstance();
        binding = ActivitySigninBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.signInButtonSignIn.setOnClickListener(view -> {
            try {
                createAccount(validateInput(fetchInput()));
            }catch(Exception exception) {
                Log.e(TAG, "SignIn failed:", exception);
                Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Map<String, String> fetchInput() {
        Map<String, String> inputs = new HashMap<>();

        inputs.put("firstname", Optional.of(binding.signInInputFirstname.getText().toString()).orElse(""));
        inputs.put("lastname", Optional.of(binding.signInInputLastname.getText().toString()).orElse(""));
        inputs.put("street", Optional.of(binding.signInInputStreet.getText().toString()).orElse(""));
        inputs.put("houseNumber", Optional.of(binding.signInInputHouseNumber.getText().toString()).orElse(""));
        inputs.put("postalCode", Optional.of(binding.signInInputPostalCode.getText().toString()).orElse(""));
        inputs.put("town", Optional.of(binding.signInInputTown.getText().toString()).orElse(""));
        inputs.put("email", Optional.of(binding.signInInputEmail.getText().toString()).orElse(""));
        inputs.put("password", Optional.of(binding.signInInputPassword.getText().toString()).orElse(""));

        return inputs;
    }

    private Map<String, String> validateInput(Map<String, String> inputs) throws IllegalArgumentException {
        if(inputs.get("firstname").isEmpty()) throw new IllegalArgumentException("Firstname can not be empty");
        if(inputs.get("lastname").isEmpty()) throw new IllegalArgumentException("Lastname can not be empty");
        if(inputs.get("street").isEmpty()) throw new IllegalArgumentException("Street can not be empty");
        if(inputs.get("houseNumber").isEmpty()) throw new IllegalArgumentException("House number can not be empty");
        if(inputs.get("postalCode").isEmpty()) throw new IllegalArgumentException("Postal code can not be empty");
        if(inputs.get("town").isEmpty()) throw new IllegalArgumentException("Town can not be empty");
        if(inputs.get("email").isEmpty()) throw new IllegalArgumentException("E-Mail can not be empty");
        if(inputs.get("password").isEmpty()) throw new IllegalArgumentException("Password can not be empty");

        return inputs;
    }

    private void createAccount(Map<String, String> inputs) {
        auth.createUserWithEmailAndPassword(inputs.get("email"), inputs.get("password"))
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "User creation successful.");
                    createUserEntry(auth.getCurrentUser(), inputs);
                    loadDashboardActivity();
                } else {
                    Log.w(TAG, "User creation failed.", task.getException());
                    Toast.makeText(SignInActivity.this, task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                    binding.signInInputEmail.setText("");
                    binding.signInInputPassword.setText("");
                    binding.signInDescriptionEmail.setTextColor(getResources().getColor(R.color.lightRed));
                    binding.signInDescriptionPassword.setTextColor(getResources().getColor(R.color.lightRed));
                }
            });
    }

    private void createUserEntry(FirebaseUser user, Map<String, String> inputs) {
        UserID userID = new UserID(user.getUid());
        Map<String, Object> userEntry = new HashMap<>();

        userEntry.put("FirstName", inputs.get("firstname"));
        userEntry.put("LastName", inputs.get("lastname"));
        userEntry.put("Street", inputs.get("street"));
        userEntry.put("HouseNumber", inputs.get("houseNumber"));
        userEntry.put("PostalCode", inputs.get("postalCode"));
        userEntry.put("Town", inputs.get("town"));
        userEntry.put("Email", user.getEmail());

        insertUserEntry(user, userID, userEntry);
    }

    private void insertUserEntry(FirebaseUser user, UserID userID, Map<String, Object> userEntry) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        database.collection("User").document(userID.getId())
                .set(userEntry)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "User: " + userID.getId() + " was successfully inserted in database."))
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Error while trying to insert new user " + userID.getId() + "into databse.");
                    Toast.makeText(this, "Failed to insert new User", Toast.LENGTH_SHORT);
                    user.delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "User " + userID.getId() + " deleted.");
                                }
                            });
                });
    }

    private void loadDashboardActivity() {
        Intent dashboardActivityIntent = new Intent(this, DashboardActivity.class);
        startActivity(dashboardActivityIntent);
    }

}
