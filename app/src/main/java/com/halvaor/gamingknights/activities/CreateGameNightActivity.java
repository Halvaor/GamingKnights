package com.halvaor.gamingknights.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.type.Date;
import com.halvaor.gamingknights.DatePickerFragment;
import com.halvaor.gamingknights.DatePickerInterface;
import com.halvaor.gamingknights.IDs.UserID;
import com.halvaor.gamingknights.TimePickerFragment;
import com.halvaor.gamingknights.TimePickerInterface;
import com.halvaor.gamingknights.User;
import com.halvaor.gamingknights.databinding.ActivityCreateGamenightBinding;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class CreateGameNightActivity extends FragmentActivity implements TimePickerInterface, DatePickerInterface {

    private static final String TAG = "CreateGameNightActivity";
    private User chosenHost;
    private String playgroupID;
    private String groupName;
    private ActivityCreateGamenightBinding binding;
    private FirebaseFirestore database;
    private List<String> hostNames;
    private List<User> potentialHosts;
    private int hour;
    private int minute;
    private int day;
    private int month;
    private int year;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = ActivityCreateGamenightBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        this.database = FirebaseFirestore.getInstance();
        this.hostNames = new ArrayList<>();
        this.potentialHosts = new ArrayList<>();

        this.playgroupID = getIntent().getExtras().getString("playgroupID");
        this.groupName = getIntent().getExtras().getString("groupName");

        binding.createGameNightCancelButton.setOnClickListener(view -> {
            Intent playgroupIntent = new Intent(this, GroupActivity.class);
            playgroupIntent.putExtra("playgroupID", playgroupID);
            playgroupIntent.putExtra("groupName", groupName);
            startActivity(playgroupIntent);
        });

        binding.createGameNightValueTime.setOnClickListener(view -> {
            TimePickerFragment timepicker = new TimePickerFragment(this);
            timepicker.show(getSupportFragmentManager(), TAG);
        });

        binding.createGameNightValueDate.setOnClickListener(view -> {
            DatePickerFragment datePicker = new DatePickerFragment(this);
            datePicker.show(getSupportFragmentManager(), TAG);
        });

        binding.createGameNightValueHost.setOnClickListener(view -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("Gastgeber auswählen");

            dialogBuilder.setItems(hostNames.toArray(new String[0]), (dialogInterface, selection) -> {
                        binding.createGameNightValueHost.setText(hostNames.get(selection));
                        this.chosenHost = potentialHosts.get(selection);
                    }
            );
            dialogBuilder.create().show();
        });

        binding.createGameNightCreateButton.setOnClickListener(view -> {
            //GameNight anlegen
            /*
            - validieren ob alle Felder ausgefüllt sind //done
            - GameNight anlegen
            - Auf GameNight Activity wechseln (mind. gameNightID muss bekannt sein)
             */

            String host = Optional.of(String.valueOf(binding.createGameNightValueHost.getText())).orElse("");
            String time = Optional.of(String.valueOf(binding.createGameNightValueTime.getText())).orElse("");
            String date = Optional.of(String.valueOf(binding.createGameNightValueDate.getText())).orElse("");

            if(host.isEmpty() || time.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Bitte fülle alle Felder aus.", Toast.LENGTH_SHORT).show();
            } else {
                Map<String, Object> gameNightData = new HashMap<>();
                LocalDateTime.of(this.year, this.month, this.day, this.hour, this.minute);
                gameNightData.put("DateTime", Timestamp
            }
        });


        determineNextHost();
    }

    private void determineNextHost() {
        CollectionReference hostRef = database.collection("Playgroup").document(this.playgroupID).collection("Host");
        hostRef.orderBy("LastTimeHosting", Query.Direction.ASCENDING).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Log.d(TAG, "Successfully determined host");
                if(task.getResult().isEmpty()) {
                    Log.d(TAG, "Collection \"Host\" is empty");
                } else {
                    List<String> hostIDs = new ArrayList<>();
                    task.getResult().getDocuments().stream().forEach(documentSnapshot -> hostIDs.add(documentSnapshot.getId()));

                    retrieveUserData(hostIDs);

                    //ToDo Host Daten müssen aus "User" entnommen und in GameNight geschrieben werden (erst beim commit)
                }
            } else {
                Log.d(TAG, "Failed to determined host");
            }
        });
    }

    private void retrieveUserData(List<String> hostIDs) {
        Query possibleHostsQuery = database.collection("User").whereIn(FieldPath.documentId(), hostIDs);

        possibleHostsQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Successfully retrieved Host data.");
                if(task.getResult().isEmpty()) {
                    Log.d(TAG, "No Host data found.");
                } else {
                    Log.d(TAG, "Found Host data.");
                    List<DocumentSnapshot> documentSnapshots = task.getResult().getDocuments();

                    for(DocumentSnapshot snapshot : documentSnapshots) {
                        String firstName = snapshot.getString("FirstName");
                        String lastName = snapshot.getString("LastName");
                        String houseNumber = snapshot.getString("HouseNumber");
                        String postalCode = snapshot.getString("PostalCode");
                        String street = snapshot.getString("Street");
                        String town = snapshot.getString("Town");
                        String eMail = snapshot.getString("Email");
                        String userID = snapshot.getId();

                        this.hostNames.add(firstName + " " + lastName);
                        this.potentialHosts.add(new User(eMail, firstName, lastName, houseNumber, postalCode, street, town, userID));
                    }

                   this.chosenHost = potentialHosts.get(0);
                   binding.createGameNightValueHost.setText(chosenHost.getFirstName() + " " + chosenHost.getLastName());
                }
            } else {
                Log.d(TAG, "Failed to retrieve Host Data from User collection");
            }
        });
    }

    @Override
    public void bindTime(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
        binding.createGameNightValueTime.setText(hour + ":" + minute + " Uhr");
    }

    @Override
    public void bindDate(DatePicker datePicker) {
        this.year = datePicker.getYear();
        this.month = datePicker.getMonth();
        this.day = datePicker.getDayOfMonth();

        Calendar calendar = Calendar.getInstance();
        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());

        String month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
        int day = datePicker.getDayOfMonth();
        int year = datePicker.getYear();

        binding.createGameNightValueDate.setText(day + "." + month + "." + year);
    }
}
