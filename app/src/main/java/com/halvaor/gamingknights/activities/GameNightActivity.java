package com.halvaor.gamingknights.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.halvaor.gamingknights.R;
import com.halvaor.gamingknights.databinding.ActivityGameNightBinding;
import com.halvaor.gamingknights.dialog.DatePickerFragment;
import com.halvaor.gamingknights.dialog.DatePickerInterface;
import com.halvaor.gamingknights.dialog.TimePickerFragment;
import com.halvaor.gamingknights.dialog.TimePickerInterface;
import com.halvaor.gamingknights.domain.User;
import com.halvaor.gamingknights.domain.id.UserID;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GameNightActivity extends FragmentActivity implements TimePickerInterface, DatePickerInterface {

    private static final String TAG = "GameNightActivity";
    private ActivityGameNightBinding binding;
    private FirebaseFirestore database;
    private String gameNightID;
    private String userID;
    private List<String> participantNames;
    private List<User> usersOfGameNight;
    private DocumentSnapshot gameNightData;
    private List<String> gameSuggestions;
    private List<String> foodTypes;
    private Map<String, String> gameSuggestionVotes;
    private Map<String, String> foodVotes;
    private Map<String, String> foodOrders;
    private String deliveryServiceUrl = "";
    private String deliveryServiceName = "";
    private int hour;
    private int minute;
    private int month;
    private int day;
    private int year;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.database = FirebaseFirestore.getInstance();
        this.usersOfGameNight = new ArrayList<>();
        this.participantNames = new ArrayList<>();
        this.gameSuggestions = new ArrayList<>();
        this.foodTypes = new ArrayList<>();
        this.foodOrders = new HashMap<>();
        this.userID = new UserID(FirebaseAuth.getInstance().getUid()).getId();
        this.gameNightID = getIntent().getStringExtra("gameNightID");
        this.binding = ActivityGameNightBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initGameNightView();

        bindListeners();
    }

    private void bindListeners() {
        this.binding.gameNightTimePlaceButton.setOnClickListener(view -> {
            updateDateTime();
        });

        this.binding.gameNightChangeHostButton.setOnClickListener(view -> {
            changeHost();
        });

        this.binding.gameNightGameProposalButton.setOnClickListener(view -> {
            addGameSuggestion();
        });

        this.binding.gameNightGameVoteButton.setOnClickListener(view -> {
            addGameVoting();
        });

        this.binding.gameNightFoodVoteButton.setOnClickListener(view -> {
            addFoodVoting();
        });

        this.binding.gameNightDeliveryServiceButton.setOnClickListener(view -> {
            showDeliveryServiceDialog();
        });

        this.binding.gameNightFoodOrderButton.setOnClickListener(view -> {
            addFoodOrder();
        });

        this.binding.gameNightMessageButton.setOnClickListener(view -> {
            retrieveUsersAndSendMessage();
        });

        this.binding.gameNightCancelButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, GroupActivity.class);
            intent.putExtra("playgroupID", this.gameNightData.getString("PlaygroupID"));
            intent.putExtra("groupName", this.gameNightData.getString("PlaygroupName"));

            startActivity(intent);
        });

        this.binding.gameNightDeleteGameNightButton.setOnClickListener(view -> {
            startDeleteGameNightDialog();
        });
    }

    private void startDeleteGameNightDialog() {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage("Möchten sie diesen Spieleabend wirklich löschen?");
            dialogBuilder.setCancelable(true);

            dialogBuilder.setPositiveButton("Ja", (dialog, id) -> {
                dialog.cancel();
                deleteGameNight();

                Intent dashboardActivityIntent = new Intent(this, DashboardActivity.class);
                startActivity(dashboardActivityIntent);
            });

            dialogBuilder.setNegativeButton("Nein", (dialog, id) -> {
                dialog.cancel();
            });

            dialogBuilder.create().show();
    }

    private void deleteGameNight() {
        Runnable runnable = () -> {
            DocumentReference gameNightRef = database.collection("GameNight").document(this.gameNightID);
            gameNightRef.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Successfully deleted GameNight with ID: " + this.gameNightID);
                } else {
                    Log.d(TAG, "Failed to delete GameNight with ID: " + this.gameNightID);
                }
            });
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }


    private void retrieveUsersAndSendMessage() {
        List<String> participants = (List<String>) (this.gameNightData.get("Participants"));
        if(participants == null) {
            participants = new ArrayList<>();
        }
        Query possibleHostsQuery = database.collection("User").whereIn(FieldPath.documentId(), participants);

        possibleHostsQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Successfully retrieved Host data.");
                if(task.getResult().isEmpty()) {
                    Log.d(TAG, "No Host data found.");
                } else {
                    Log.d(TAG, "Found Host data.");
                    List<DocumentSnapshot> documentSnapshots = task.getResult().getDocuments();
                    List<User> users = new ArrayList<>();

                    for(DocumentSnapshot snapshot : documentSnapshots) {
                        String firstName = snapshot.getString("FirstName");
                        String lastName = snapshot.getString("LastName");
                        String houseNumber = snapshot.getString("HouseNumber");
                        String postalCode = snapshot.getString("PostalCode");
                        String street = snapshot.getString("Street");
                        String town = snapshot.getString("Town");
                        String eMail = snapshot.getString("Email");
                        String id = snapshot.getId();

                        users.add(new User(eMail, firstName, lastName, houseNumber, postalCode, street, town, id));
                    }

                    sendMessage(users);
                }
            } else {
                Log.d(TAG, "Failed to retrieve Host Data from User collection");
            }
        });
    }

    private void sendMessage(List<User> users) {
        List<String> mailAdresses = users.stream()
                .filter(user -> !user.getUserID().equals(this.userID))
                .map(user -> user.getEmail()).collect(Collectors.toList());

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, mailAdresses.toArray(new String[0]));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Nachricht aus deiner Spielgruppe " + this.gameNightData.get("PlaygroupName"));

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void determineRatingAccess(DocumentSnapshot document) {
        if(Timestamp.now().compareTo((Timestamp) document.get("DateTime")) < 0) {
            binding.gameNightRateButton.setOnClickListener(view -> {
                Toast.makeText(this, "Eine Bewertung ist erst nach dem Spieleabend möglich.", Toast.LENGTH_SHORT).show();
            });
        } else {
            binding.gameNightRateButton.setOnClickListener(view -> {
                startHostRatingDialog();
            });
        }
    }

    private void startHostRatingDialog() {
        Map<String, String> ratings = new HashMap<>();
        String[] ratingOptions = new String[]{"1", "2", "3", "4", "5", "6"};

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Gib dem Gastgeber eine Note");
        dialogBuilder.setSingleChoiceItems(ratingOptions, 0, (dialogInterface, selection) -> {

            ratings.put("hostRating", ratingOptions[selection]);
            dialogInterface.cancel();
            startFoodRatingDialog(ratings);

        });
        dialogBuilder.create().show();
    }

    private void startFoodRatingDialog(Map<String, String> ratings) {
        String[] ratingOptions = new String[]{"1", "2", "3", "4", "5", "6"};

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Gib dem Essen eine Note");
        dialogBuilder.setSingleChoiceItems(ratingOptions, 0, (dialogInterface, selection) -> {

            ratings.put("foodRating", ratingOptions[selection]);
            dialogInterface.cancel();
            startTotalRatingDialog(ratings);

        });
        dialogBuilder.create().show();
    }

    private void startTotalRatingDialog(Map<String, String> ratings) {
        String[] ratingOptions = new String[]{"1", "2", "3", "4", "5", "6"};

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Gib dem Spieleabend eine Note");
        dialogBuilder.setSingleChoiceItems(ratingOptions, 0, (dialogInterface, selection) -> {

            ratings.put("generalRating", ratingOptions[selection]);
            dialogInterface.cancel();

            insertRating(ratings);

        });
        dialogBuilder.create().show();
    }

    public void insertRating(Map<String, String> ratings) {
        Runnable runnable = () -> {
            Map<String, Object> ratingsData = new HashMap<>();

            ratingsData.put("HostRatings." + this.userID, ratings.get("hostRating"));
            ratingsData.put("FoodRatings." + this.userID, ratings.get("foodRating"));
            ratingsData.put("GeneralRatings." + this.userID, ratings.get("generalRating"));

            DocumentReference gameNightRef = database.collection("GameNight").document(this.gameNightID);
            gameNightRef.update(ratingsData)
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "Sucessfully inserted ratings of User : "
                                + this.userID
                                + " into gameNight document: "
                                + this.gameNightID);
                        Toast.makeText(this, "Bewertungen hinzugefügt.", Toast.LENGTH_SHORT).show();
                        updateRatingsView();
                    })
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "Failed to inserted ratings of User : "
                                + this.userID
                                + " into gameNight document: "
                                + this.gameNightID, e);
                        Toast.makeText(this, "Fehler beim hinzufügen der Bewertungen.", Toast.LENGTH_SHORT).show();
                    });
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void updateRatingsView() {
        DocumentReference gameNightRef = database.collection("GameNight").document(gameNightID);
        gameNightRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Sucessfully retrieved gameNight document: " + gameNightID);
                if (task.getResult().exists()) {

                    this.gameNightData = task.getResult();
                    getRatings(task.getResult());

                } else {
                    Log.d(TAG, "Could´t find gameNight document with documentID: " + gameNightID);
                }
            } else {
                Log.d(TAG, "Failed to retrieved gameNight document: " + gameNightID);
            }
        });
    }

    private void addFoodOrder() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Für wen ist die Bestellung?");
        dialogBuilder.setItems(this.participantNames.toArray(new String[0]), (dialogInterface, selection) -> {
            String selectedUser = usersOfGameNight.get(selection).getUserID();

            startFoodOrderDialog(selectedUser);
        });

        dialogBuilder.create().show();
    }

    private void startFoodOrderDialog(String userID) {
        final EditText input = new EditText(this);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Gib deine Bestellung ein");
        dialogBuilder.setView(input);

        dialogBuilder.setPositiveButton("OK", (dialogInterface, i) -> {
            if (input.getText().toString().isEmpty()) {
                Toast.makeText(this, "Deine Bestellung kann nicht leer sein.", Toast.LENGTH_SHORT).show();
            } else {
                String order = input.getText().toString();
                insertOrder(userID, order);
            }
        });
        dialogBuilder.setNegativeButton("Abbrechen", (dialogInterface, i) -> {
            dialogInterface.cancel();
        });
        dialogBuilder.create().show();
    }

    private void insertOrder(String userID, String order) {
        Runnable runnable = () -> {
            DocumentReference gameNightRef = database.collection("GameNight").document(this.gameNightID);
            gameNightRef.update("FoodOrders." + userID, order)
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "Successfully inserted food order");

                        loadFoodOrders();
                    })
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "Failed to insert food order", e);
                    });
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void loadFoodOrders() {
        DocumentReference gameNightRef = database.collection("GameNight").document(gameNightID);
        gameNightRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Sucessfully retrieved gameNight document: " + gameNightID);
                if (task.getResult().exists()) {
                    this.gameNightData = task.getResult();

                    getFoodOrders();
                    createFoodOrderItems(this.usersOfGameNight);

                } else {
                    Log.d(TAG, "Could´t find gameNight document with documentID: " + gameNightID);
                }
            } else {
                Log.d(TAG, "Failed to retrieved gameNight document: " + gameNightID);
            }
        });
    }

    private void retrieveUsersAndCreateFoodOrderItems() {
        List<String> participants = (List<String>) (this.gameNightData.get("Participants"));
        if(participants == null ) participants = new ArrayList<>();
        Query getUsersQuery = database.collection("User").whereIn(FieldPath.documentId(), participants);

        getUsersQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Successfully retrieved User data.");
                if(task.getResult().isEmpty()) {
                    Log.d(TAG, "No User data found.");
                } else {
                    Log.d(TAG, "Found User data.");

                    List<User> users = new ArrayList<>();
                    for(DocumentSnapshot snapshot : task.getResult().getDocuments()) {
                        String firstName = snapshot.getString("FirstName");
                        String lastName = snapshot.getString("LastName");
                        String houseNumber = snapshot.getString("HouseNumber");
                        String postalCode = snapshot.getString("PostalCode");
                        String street = snapshot.getString("Street");
                        String town = snapshot.getString("Town");
                        String eMail = snapshot.getString("Email");
                        String id = snapshot.getId();

                        users.add(new User(eMail, firstName, lastName, houseNumber, postalCode, street, town, id));
                    }

                    createFoodOrderItems(users);
                }
            }
        });
    }

    private void createFoodOrderItems(List<User> users) {
        binding.gameNightFoodOrderContainer.removeAllViews();
        LinearLayout container = binding.gameNightFoodOrderContainer;

        for(Map.Entry<String, String> entry : foodOrders.entrySet()) {
            LinearLayout item = (LinearLayout) getLayoutInflater().inflate(R.layout.view_item_order, null);
            TextView participant = item.findViewById(R.id.view_item_name);
            TextView order = item.findViewById(R.id.view_item_orderDetails);

            //Match user to determine name of user
            User matchingUser = users.stream().filter(user -> user.getUserID().equals(entry.getKey())).findFirst().orElse(null);

            if(matchingUser != null) {
                Log.d(TAG, "Found matching user for given food Order. UserID: " + matchingUser.getUserID());

                String firstAndLastName = matchingUser.getFirstName() + " " + matchingUser.getLastName();
                participant.setText(firstAndLastName);
                order.setText(entry.getValue());

                container.addView(item);
            } else {
                Log.e(TAG, "Could´t find matching user for given food Order. UserID: " + entry.getKey());
            }
        }
    }

    private void getFoodOrders() {
        this.foodOrders = (Map<String, String>) this.gameNightData.get("FoodOrders");
    }

    private void showDeliveryServiceDialog() {
        final EditText input =  new EditText(this);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Name des Lieferanten");
        dialogBuilder.setView(input);

        dialogBuilder.setPositiveButton("OK", (dialogInterface, i) -> {
            if(input.getText().toString().isEmpty()) {
                Toast.makeText(this, "Bitte gib den Namen des Lieferanten ein", Toast.LENGTH_SHORT).show();
            } else {
                this.deliveryServiceName = input.getText().toString();
                showDeliveryServiceUrlDialog();
            }
        });
        dialogBuilder.setNegativeButton("Abbrechen", (dialogInterface, i) -> {
            dialogInterface.cancel();
        });
        dialogBuilder.create().show();
    }

    private void showDeliveryServiceUrlDialog() {
        final EditText input = new EditText(this);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Link zur Speisekarte");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS);
        dialogBuilder.setView(input);

        dialogBuilder.setPositiveButton("OK", (dialogInterface, i) -> {
            if(input.getText().toString().isEmpty()) {
                Toast.makeText(this, "Bitte gib die Url zur Speisekarte an", Toast.LENGTH_SHORT).show();
            } else {
                this.deliveryServiceUrl = input.getText().toString();
                insertDeliveryServiceData();
            }
        });
        dialogBuilder.setNegativeButton("Abbrechen", (dialogInterface, i) -> {
            dialogInterface.cancel();
        });
        dialogBuilder.create().show();
    }

    private void insertDeliveryServiceData() {
        Runnable runnable = () -> {
            DocumentReference gameNightRef = database.collection("GameNight").document(this.gameNightID);
            Map<String, Object> deliveryServiceData = new HashMap<>();
            deliveryServiceData.put("Name", this.deliveryServiceName);
            deliveryServiceData.put("Url", this.deliveryServiceUrl);

            gameNightRef.update("DeliveryService", deliveryServiceData)
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "Successfully updated DeliveryService in GameNight: " + this.gameNightID);

                        updateDeliveryServiceView();
                    })
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "Failed to updated DeliveryService in GameNight: " + this.gameNightID, e);
                    });
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void updateDeliveryServiceView() {
        binding.gameNightDeliveryServiceNameValue.setText(this.deliveryServiceName);
        binding.gameNightDeliveryServiceUrlValue.setText(this.deliveryServiceUrl);
    }

    private void addFoodVoting() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Stimme für eine Essensrichtung ab");
        dialogBuilder.setSingleChoiceItems(this.foodTypes.toArray(new String[0]), 0, ((dialogInterface, selection) -> {

            Runnable runnable = () -> {
                DocumentReference gameNightRef = database.collection("GameNight").document(this.gameNightID);
                gameNightRef.update("FoodTypeVotes." + this.userID, this.foodTypes.get(selection))
                        .addOnSuccessListener(unused -> {
                            Log.d(TAG, "Successfully updated FoodTypeVotes");

                            loadFoodTypeVotes();
                        })
                        .addOnFailureListener(e -> {
                            Log.d(TAG, "Failed to updated FoodTypeVotes", e);
                        });
                dialogInterface.cancel();
            };
            Thread thread = new Thread(runnable);
            thread.start();

        }));

        dialogBuilder.create().show();
    }

    private void loadFoodTypeVotes() {
        DocumentReference gameNightRef = database.collection("GameNight").document(gameNightID);
        gameNightRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Sucessfully retrieved gameNight document: " + gameNightID);
                if (task.getResult().exists()) {
                    this.gameNightData = task.getResult();

                    getFoodTypeVotes();
                    createFoodTypeVoteItems();

                } else {
                    Log.d(TAG, "Could´t find gameNight document with documentID: " + gameNightID);
                }
            } else {
                Log.d(TAG, "Failed to retrieved gameNight document: " + gameNightID);
            }
        });
    }

    private void createFoodTypeVoteItems() {
        binding.gameNightFoodProposalContainer.removeAllViews();
        LinearLayout container = binding.gameNightFoodProposalContainer;

        for(String foodType : this.foodTypes) {
            LinearLayout item = (LinearLayout) getLayoutInflater().inflate(R.layout.view_item_with_votes, null);
            TextView foodTypeName = item.findViewById(R.id.view_item);
            TextView foodTypeVotes = item.findViewById(R.id.view_item_votes);

            Long sumOfVotes = this.foodVotes.values().stream().filter(vote -> vote.equals(foodType)).count();

            if(sumOfVotes != 0) {
                foodTypeName.setText(foodType);
                foodTypeVotes.setText(String.valueOf(sumOfVotes));

                container.addView(item);
            }
        }
    }

    private void getFoodTypeVotes() {
        this.foodVotes = (Map<String, String>) gameNightData.get("FoodTypeVotes");
    }

    private void getFoodTypesAndLoadVotes() {
        DocumentReference foodTypeRef = database.collection("FoodType").document("default");
        foodTypeRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Successfully retrieved foodType document: default");
                if (task.getResult().exists()) {
                    this.foodTypes = (List<String>) task.getResult().get("Type");
                    loadFoodTypeVotes();

                } else {
                    Log.d(TAG, "Could´t find foodType document with ID: default ");
                }
            } else {
                Log.d(TAG, "Failed to retrieved foodType document: default");
            }
        });
    }


    private void addGameVoting() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Stimme für ein Spiel ab");
        dialogBuilder.setSingleChoiceItems(this.gameSuggestions.toArray(new String[0]), 0, ((dialogInterface, selection) -> {

            DocumentReference gameNightRef = database.collection("GameNight").document(this.gameNightID);
            gameNightRef.update("GameSuggestionVotes." + this.userID, this.gameSuggestions.get(selection))
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Successfully updated GameSuggestionsVotes");

                    loadGameVotings();
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Failed to updated GameSuggestionsVotes", e);
                });
            dialogInterface.cancel();
        }));
        dialogBuilder.create().show();
    }

    private void addGameSuggestion() {
        String gameProposal = Optional.of(String.valueOf(binding.gameNightGameProposalValue.getText())).orElse("");

        if (gameProposal.isEmpty()) {
            Toast.makeText(this, "Bitte trage einen Spielenamen ein", Toast.LENGTH_SHORT).show();
        } else {
            if (this.gameSuggestions.contains(gameProposal)) {
                Toast.makeText(this, "Spiel ist bereits hinterlegt", Toast.LENGTH_SHORT).show();
            } else {
                DocumentReference gameNightRef = database.collection("GameNight").document(this.gameNightID);
                gameNightRef.update("GameSuggestions", FieldValue.arrayUnion(gameProposal))
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "Successfully updated GameSuggestions: " + gameProposal);

                        retrieveGameSuggestionsAndCreateItems();

                    })
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "Failed to updated GameSuggestions: " + gameProposal, e);
                    });
            }
        }
        binding.gameNightGameProposalValue.setText("");
    }

    private void retrieveGameSuggestionsAndCreateItems() {
        DocumentReference gameNightRef = database.collection("GameNight").document(this.gameNightID);
        gameNightRef.get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Log.d(TAG, "Successfully retrieved GameNightDate for: " + this.gameNightID);
                if (task.getResult().exists()) {
                    getGameSuggestionAndVotes(task.getResult());
                    createGameSuggestionItems();
                } else {
                    Log.e(TAG, "Could´t find GameNight for: " + this.gameNightID);
                }
            }else {
                Log.e(TAG, "Failed to retrieved GameNightData for: " + this.gameNightID);
            }
        });
    }

    private void getGameSuggestionAndVotes(DocumentSnapshot gameNightData) {
        this.gameSuggestions =(List<String>)gameNightData.get("GameSuggestions");
        this.gameSuggestionVotes = (Map<String, String>) gameNightData.get("GameSuggestionVotes");
    }

    private void createGameSuggestionItems() {
        binding.gameNightGameProposalContainer.removeAllViews();

        LinearLayout container = binding.gameNightGameProposalContainer;

        for(String gameSuggestion : this.gameSuggestions) {
            LinearLayout item = (LinearLayout) getLayoutInflater().inflate(R.layout.view_item_with_votes, null);
            TextView gameName = item.findViewById(R.id.view_item);
            TextView gameVotes = item.findViewById(R.id.view_item_votes);

            gameName.setText(gameSuggestion);
            Long sumOfVotes = this.gameSuggestionVotes.values().stream().filter(vote -> vote.equals(gameSuggestion)).count();
            gameVotes.setText(String.valueOf(sumOfVotes));

            container.addView(item);
        }
    }

    private void changeHost() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Gastgeber auswählen");

        dialogBuilder.setItems(this.participantNames.toArray(new String[0]), (dialogInterface, selection) -> {
            Runnable runnable = () -> {
                //update view
                String hostName =  this.usersOfGameNight.get(selection).getFirstName()
                        + " "
                        + this.usersOfGameNight.get(selection).getLastName();
                String streetAndHouseNumber = this.usersOfGameNight.get(selection).getStreet()
                        + " "
                        + this.usersOfGameNight.get(selection).getHouseNumber();
                String townAndPostalCode =  this.usersOfGameNight.get(selection).getPostalCode()
                        + " "
                        + this.usersOfGameNight.get(selection).getTown();

                binding.gameNightHostNameValue.setText(hostName);
                binding.gameNightHostStreetHousnumberValue.setText(streetAndHouseNumber);
                binding.gameNightHostTownPostalcodeValue.setText(townAndPostalCode);

                //update db
                Map<String, Object> hostData = new HashMap<>();
                User chosenHost = this.usersOfGameNight.get(selection);

                hostData.put("FirstName", chosenHost.getFirstName());
                hostData.put("LastName", chosenHost.getLastName());
                hostData.put("HouseNumber", chosenHost.getHouseNumber());
                hostData.put("PostalCode", chosenHost.getPostalCode());
                hostData.put("Street", chosenHost.getStreet());
                hostData.put("Town", chosenHost.getTown());
                hostData.put("UserID", chosenHost.getUserID());

                database.collection("GameNight").document(this.gameNightID).update("Host", hostData)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Successfully updated Host to " + hostData.get("UserID"));
                        })
                        .addOnFailureListener(e -> {
                            Log.d(TAG, "Failed to update Host to" + hostData.get("UserID"), e);
                        });
            };
            Thread thread = new Thread(runnable);
            thread.start();
        });

        dialogBuilder.create().show();
    }

    private void retrieveUserData() {
        List<String> participants = (List<String>) (this.gameNightData.get("Participants"));
        if(participants == null) participants = new ArrayList<>();
        Query possibleHostsQuery = database.collection("User").whereIn(FieldPath.documentId(), participants);

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
                        String id = snapshot.getId();

                        this.participantNames.add(firstName + " " + lastName);
                        this.usersOfGameNight.add(new User(eMail, firstName, lastName, houseNumber, postalCode, street, town, id));
                    }
                }
            } else {
                Log.d(TAG, "Failed to retrieve Host Data from User collection");
            }
        });
    }

    private void initGameNightView() {
        Runnable runnable = () -> {

            DocumentReference gameNightRef = database.collection("GameNight").document(this.gameNightID);
            gameNightRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Sucessfully retrieved gameNight document: " + this.gameNightID);
                    if (task.getResult().exists()) {
                        this.gameNightData = task.getResult();
                        getPlaygroupName(task.getResult());
                        retrieveUserData();
                        getRatings(task.getResult());
                        getDateAndTime(task.getResult());
                        getHost(task.getResult());
                        getGameSuggestionAndVotes(task.getResult());
                        createGameSuggestionItems();
                        getFoodTypesAndLoadVotes();
                        getDeliveryServiceData();
                        getFoodOrders();
                        retrieveUsersAndCreateFoodOrderItems();
                        determineRatingAccess(task.getResult());
                    } else {
                        Log.d(TAG, "Could´t find gameNight document with documentID: " + this.gameNightID);
                    }
                } else {
                    Log.d(TAG, "Failed to retrieved gameNight document: " + this.gameNightID);
                }
            });
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void getPlaygroupName(DocumentSnapshot documentSnapshot) {
        String playgroupName = documentSnapshot.getString("PlaygroupName");
        if(playgroupName != null && !playgroupName.isEmpty()) {
            binding.gameNightHeaderGroupname.setText(playgroupName);
        }
    }

    private void getDeliveryServiceData() {
        Map<String, String> deliveryServiceData =  (Map<String, String>) this.gameNightData.get("DeliveryService");
        this.deliveryServiceName = Optional.ofNullable(deliveryServiceData.get("Name")).orElse("");
        this.deliveryServiceUrl = Optional.ofNullable(deliveryServiceData.get("Url")).orElse("");

        binding.gameNightDeliveryServiceNameValue.setText(this.deliveryServiceName);
        binding.gameNightDeliveryServiceUrlValue.setText(this.deliveryServiceUrl);
    }

    private void loadGameVotings() {
        Runnable runnable = () -> {

            DocumentReference gameNightRef = database.collection("GameNight").document(gameNightID);
            gameNightRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Sucessfully retrieved gameNight document: " + gameNightID);
                    if (task.getResult().exists()) {
                        this.gameNightData = task.getResult();
                        getGameSuggestionAndVotes(task.getResult());
                        createGameSuggestionItems();
                    } else {
                        Log.d(TAG, "Could´t find gameNight document with documentID: " + gameNightID);
                    }
                } else {
                    Log.d(TAG, "Failed to retrieved gameNight document: " + gameNightID);
                }
            });
        };

        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void getHost(DocumentSnapshot gameNightData) {
        if(gameNightData.exists()) {
            Map<String, Object> hostData = (Map<String, Object>) gameNightData.get("Host");
            String firstName = (String) hostData.get("FirstName");
            String lastName = (String) hostData.get("LastName");
            String street = (String) hostData.get("Street");
            String houseNumber= (String) hostData.get("HouseNumber");
            String postalCode = (String) hostData.get("PostalCode");
            String town = (String) hostData.get("Town");

            binding.gameNightHostNameValue.setText(firstName + " " + lastName);
            binding.gameNightHostStreetHousnumberValue.setText((street + " " + houseNumber));
            binding.gameNightHostTownPostalcodeValue.setText(postalCode + " " + town);
        } else {
            Log.d(TAG, "Could´t find gameNight document with documentID: " + gameNightID);
        }
    }


    private void getRatings(DocumentSnapshot gameNightData) {
        if(gameNightData.exists()) {
            //HostRating
            Map<String, String> hostRatings = (Map<String, String>) gameNightData.get("HostRatings");
            String userHostRating = hostRatings.get(userID) == null ? "*" : hostRatings.get(userID);
            binding.gameNightRatingsHostMyRating.setText(userHostRating);

            Double averageHostRating = hostRatings.values().stream().collect(Collectors.averagingInt(value -> Integer.valueOf(value)));
            binding.gameNightRatingsHostTotal.setText(String.valueOf(averageHostRating));

            //FoodRating
            Map<String, String> footRatings = (Map<String, String>) gameNightData.get("FoodRatings");
            String userFoodRating = footRatings.get(userID) == null ? "*" : footRatings.get(userID);
            binding.gameNightRatingsFoodMyRating.setText(userFoodRating);

            Double averageFootRatings = footRatings.values().stream().collect(Collectors.averagingInt(value -> Integer.valueOf(value)));
            binding.gameNightRatingsFoodTotal.setText(String.valueOf(averageFootRatings));

            //TotalRating
            Map<String, String> totalRatings = (Map<String, String>) gameNightData.get("GeneralRatings");
            String userTotalRating = totalRatings.get(userID) == null ? "*" : totalRatings.get(userID);
            binding.gameNightRatingsTotalMyRating.setText(userTotalRating);

            Double averageTotalRating = totalRatings.values().stream().collect(Collectors.averagingInt(value -> Integer.valueOf(value)));
            binding.gameNightRatingsTotalTotal.setText(String.valueOf(averageTotalRating));

        } else {
            Log.d(TAG, "Could´t find gameNight document with documentID: " + gameNightID);
        }
    }

    private void getDateAndTime(DocumentSnapshot gameNightData) {
        if(gameNightData.exists()) {
            Timestamp dateTime = (Timestamp) gameNightData.get("DateTime");
            Instant instant = Instant.ofEpochSecond(dateTime.getSeconds());
            LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");

            binding.gameNightDateTimeValue.setText(ldt.format(dateFormat) +"\n" + ldt.format(timeFormat) + " Uhr");
        } else {
            Log.d(TAG, "Could´t find gameNight document with documentID: " + gameNightID);
        }
    }

    private void updateDateTime() {
        Runnable runnable = () -> {
            //Calls a chain of DatePicker, TimePicker, update to db and update to view
            DatePickerFragment datePicker = new DatePickerFragment(this);
            datePicker.show(getSupportFragmentManager(), TAG);
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }


    @Override
    public void bindTime(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;

        //insert into db
        //month +1 because LocalDateTime counts months from 1-12 while Calender, etc. counts from 0-11.
        LocalDateTime localDateTime = LocalDateTime.of(this.year, this.month +1, this.day, this.hour, this.minute);
        Instant gameNightInstant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();

        Map<String, Object> datTimeData = new HashMap<>();
        datTimeData.put("DateTime", new Timestamp(gameNightInstant.getEpochSecond(), 0));

        DocumentReference gameNightRef = database.collection("GameNight").document(this.gameNightID);
        gameNightRef.update(datTimeData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully updated dateTime of gameNight: " + this.gameNightID);

                    //update view
                    DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");

                    binding.gameNightDateTimeValue.setText(
                            localDateTime.format(dateFormat) +
                            "\n" +
                            localDateTime.format(timeFormat) +
                            " Uhr");
                })
                .addOnFailureListener(e ->
                    Log.w(TAG, "Failed to update dateTime of gameNight: " + this.gameNightID, e)
                );
    }


    @Override
    public void bindDate(DatePicker datePicker) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());

        this.year = datePicker.getYear();
        this.month = datePicker.getMonth();
        this.day = datePicker.getDayOfMonth();

        TimePickerFragment timepicker = new TimePickerFragment(this);
        timepicker.show(getSupportFragmentManager(), TAG);
    }


}
