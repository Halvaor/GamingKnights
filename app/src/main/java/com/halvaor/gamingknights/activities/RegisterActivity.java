package com.halvaor.gamingknights.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.halvaor.gamingknights.R;
import com.halvaor.gamingknights.databinding.ActivityRegisterBinding;
import com.halvaor.gamingknights.util.IdPrefix;
import com.halvaor.gamingknights.util.UserID;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

        binding.registerButtonSignIn.setOnClickListener(view -> {
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

        inputs.put("firstname", Optional.ofNullable(binding.registerInputFirstname.getText().toString()).orElse(""));
        inputs.put("lastname", Optional.ofNullable(binding.registerInputLastname.getText().toString()).orElse(""));
        inputs.put("street", Optional.ofNullable(binding.registerInputStreet.getText().toString()).orElse(""));
        inputs.put("houseNumber", Optional.ofNullable(binding.registerInputHouseNumber.getText().toString()).orElse(""));
        inputs.put("postalCode", Optional.ofNullable(binding.registerInputPostalCode.getText().toString()).orElse(""));
        inputs.put("town", Optional.ofNullable(binding.registerInputTown.getText().toString()).orElse(""));
        inputs.put("email", Optional.ofNullable(binding.registerInputEmail.getText().toString()).orElse(""));
        inputs.put("password", Optional.ofNullable(binding.registerInputPassword.getText().toString()).orElse(""));

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
                    UserID userID = createUserEntry(auth.getCurrentUser(), inputs);
                    loadDashboardActivity(userID);
                } else {
                    Log.w(TAG, "User creation failed.", task.getException());
                    Toast.makeText(RegisterActivity.this, task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                    binding.registerInputEmail.setText("");
                    binding.registerInputPassword.setText("");
                    binding.registerDescriptionEmail.setTextColor(getResources().getColor(R.color.lightRed));
                    binding.registerDescriptionPassword.setTextColor(getResources().getColor(R.color.lightRed));
                }
            });
    }

    private UserID createUserEntry(FirebaseUser user, Map<String, String> inputs) {
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

        return userID;
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

    private void loadDashboardActivity(UserID userID) {
        Intent dashboardActivityIntent = new Intent(this, DashboardActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("userID", userID);
        dashboardActivityIntent.putExtras(bundle);
        startActivity(dashboardActivityIntent);
    }

}
