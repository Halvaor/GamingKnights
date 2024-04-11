package com.halvaor.gamingknights.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.halvaor.gamingknights.databinding.ActivityCreateGamenightBinding;
import com.halvaor.gamingknights.dialog.DatePickerFragment;
import com.halvaor.gamingknights.dialog.DatePickerInterface;
import com.halvaor.gamingknights.dialog.TimePickerFragment;
import com.halvaor.gamingknights.dialog.TimePickerInterface;
import com.halvaor.gamingknights.domain.User;
import com.halvaor.gamingknights.domain.id.GameNightID;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        init();
        bindListeners();
        determineNextHost();
    }

    private void bindListeners() {
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
            createAndInsertGameNight();
        });
    }

    private void init() {
        this.binding = ActivityCreateGamenightBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        this.database = FirebaseFirestore.getInstance();
        this.hostNames = new ArrayList<>();
        this.potentialHosts = new ArrayList<>();

        this.playgroupID = getIntent().getExtras().getString("playgroupID");
        this.groupName = getIntent().getExtras().getString("groupName");
    }

    private void createAndInsertGameNight() {
        String host = Optional.of(String.valueOf(binding.createGameNightValueHost.getText())).orElse("");
        String time = Optional.of(String.valueOf(binding.createGameNightValueTime.getText())).orElse("");
        String date = Optional.of(String.valueOf(binding.createGameNightValueDate.getText())).orElse("");

        if(host.isEmpty() || time.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Bitte fülle alle Felder aus.", Toast.LENGTH_SHORT).show();
        } else {
            Map<String, Object> gameNightData = buildGameNightData();

            //Generate documentID
            CollectionReference gameNightRef = database.collection("GameNight");
            GameNightID gameNightID = new GameNightID(gameNightRef.document().getId());

            //Insert new gameNight
            database.collection("GameNight").document(gameNightID.getId()).set(gameNightData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Successfully inserted gameNight " + gameNightID.getId() + " into database");
                    })
                    .addOnFailureListener(e -> {
                            Log.d(TAG, "Failed to inserted gameNight " + gameNightID.getId() + " into database", e);
                    });

            //ToDo es muss noch ein Intent erzeugt werden, der auf die neue GameNight Activity führt.
        }
    }

    @NonNull
    private Map<String, Object> buildGameNightData() {
        //month +1 because LocalDateTime counts months from 1-12 while Calender, etc. counts from 0-11.
        LocalDateTime localDateTime = LocalDateTime.of(this.year, this.month +1, this.day, this.hour, this.minute);
        Instant gameNightInstant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();

        Map<String, Object> gameNightData = new HashMap<>();
        Map<String, Object> deliveryService = new HashMap<>();
        Map<String, Object> foodOrders = new HashMap<>();
        Map<String, Object> foodRatings = new HashMap<>();
        Map<String, Object> foodTypeVotes = new HashMap<>();
        Map<String, Object> gameSuggestionVotes = new HashMap<>();
        Map<String, Object> generalRatings = new HashMap<>();
        Map<String, Object> hostRatings = new HashMap<>();
        Map<String, Object> hostData = new HashMap<>();
        List<String> gameSuggestions = new ArrayList<>();

        hostData.put("FirstName", this.chosenHost.getFirstName());
        hostData.put("LastName", this.chosenHost.getLastName());
        hostData.put("HouseNumber", this.chosenHost.getHouseNumber());
        hostData.put("PostalCode", this.chosenHost.getPostalCode());
        hostData.put("Street", this.chosenHost.getStreet());
        hostData.put("Town", this.chosenHost.getTown());
        hostData.put("UserID", this.chosenHost.getUserID());

        List<String> participantsUserIDs = this.potentialHosts.stream().map(user -> user.getUserID()).collect(Collectors.toList());

        gameNightData.put("DateTime", new Timestamp(gameNightInstant.getEpochSecond(),0));
        gameNightData.put("DeliveryService", deliveryService);
        gameNightData.put("FoodOrders", foodOrders);
        gameNightData.put("FoodRatings", foodRatings);
        gameNightData.put("FoodTypeVotes", foodTypeVotes);
        gameNightData.put("GameSuggestionVotes", gameSuggestionVotes);
        gameNightData.put("GeneralRatings", generalRatings);
        gameNightData.put("HostRatings", hostRatings);
        gameNightData.put("GameSuggestions", gameSuggestions);
        gameNightData.put("Participants", participantsUserIDs);
        gameNightData.put("PlaygroupID", this.playgroupID);
        gameNightData.put("PlaygroupName", this.groupName);
        gameNightData.put("Host", hostData);

        return gameNightData;
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
        LocalTime time = LocalTime.of(hour, minute);
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");
        this.hour = hour;
        this.minute = minute;

        binding.createGameNightValueTime.setText(time.format(timeFormat) + " Uhr");
    }

    @Override
    public void bindDate(DatePicker datePicker) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());

        this.month = calendar.get(Calendar.MONTH);
        this.day = calendar.get(Calendar.DAY_OF_MONTH);
        this.year = calendar.get(Calendar.YEAR);

        LocalDateTime date = LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.systemDefault());
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        binding.createGameNightValueDate.setText(date.format(dateFormat));
    }
}
