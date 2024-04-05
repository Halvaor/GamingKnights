package com.halvaor.gamingknights.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.halvaor.gamingknights.IDs.UserID;
import com.halvaor.gamingknights.R;
import com.halvaor.gamingknights.databinding.ActivityGroupBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public class GroupActivity extends Activity {

    private static final String TAG = "GroupActivity";
    private UserID userID;
    private String playgroupID;
    private String groupName;
    private FirebaseAuth auth;
    private FirebaseFirestore database;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityGroupBinding binding = ActivityGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();
        this.playgroupID = getIntent().getExtras().getString("playgroupID");
        this.groupName = getIntent().getExtras().getString("groupName");
        this.userID = new UserID(auth.getUid());

        binding.groupToolbarGroupnameView.setText(groupName);

        binding.groupToolbarEditbutton.setOnClickListener(view -> {
            Intent editGroupIntent = new Intent(this, EditGroupActivity.class);
            editGroupIntent.putExtra("playgroupID", playgroupID);
            editGroupIntent.putExtra("groupName", groupName);
            startActivity(editGroupIntent);
        });

        binding.groupBackButton.setOnClickListener(view -> {
            Intent dashboardActivityIntent = new Intent(this, DashboardActivity.class);
            startActivity(dashboardActivityIntent);
        });

        binding.groupCreateGameNightButton.setOnClickListener(view -> {
            Intent createGameNightIntent = new Intent(this, CreateGameNightActivity.class);
            createGameNightIntent.putExtra("playgroupID", this.playgroupID);
            createGameNightIntent.putExtra("groupName", this.groupName);
            startActivity(createGameNightIntent);
        });

        getNextGameNightAndFillDashboard(binding);
        initAndFillScrollView(binding);


    }

    private void getNextGameNightAndFillDashboard(ActivityGroupBinding binding) {
        CollectionReference gameNightRef = database.collection("GameNight");
        Query query = gameNightRef.whereEqualTo("PlaygroupID", playgroupID)
                .whereGreaterThan("DateTime", Timestamp.now())
                .orderBy("DateTime", Query.Direction.ASCENDING);

        query.get().addOnCompleteListener(task -> {

            if (task.isSuccessful()) {
                QuerySnapshot result = task.getResult();

                if(result.isEmpty()) {
                    Log.d(TAG, "Could not find next GameNight. PlaygroupID: " + playgroupID);
                } else {
                    
                    //ToDo OnClickListener muss noch gesetzt werden 
                    
                    Date date = result.getDocuments().get(0).getTimestamp("DateTime").toDate();
                    binding.groupCardDateTimeValue.setText(new SimpleDateFormat("dd-MM-yyyy HH:mm").format(date));

                    Map<String, Object> hostData = (Map<String, Object>) result.getDocuments().get(0).get("Host");
                    binding.groupCardHostValue.setText(hostData.get("FirstName") + " " + hostData.get("LastName"));

                    String hostAdress = hostData.get("Street") + " " + hostData.get("HouseNumber") + ", \n" + hostData.get("PostalCode") + " " + hostData.get("Town");
                    binding.groupCardAdressValue.setText(hostAdress);

                    //determin if there are votes left open to be done

                    Optional<Object> userVote_foodType = Optional.empty();
                    Map<String, Object> foodTypeVotes = (Map<String, Object>) (result.getDocuments().get(0).get("FoodTypeVotes"));
                    if(foodTypeVotes != null) {
                        userVote_foodType = Optional.ofNullable(foodTypeVotes.get(userID.getId()));
                    }

                    Optional<Object> userVote_gameSuggestion = Optional.empty();
                    Map<String, Object> gameSuggestionVotes = (Map<String, Object>) result.getDocuments().get(0).get("GameSuggestionVotes");
                    if(gameSuggestionVotes != null) {
                        userVote_gameSuggestion = Optional.ofNullable(gameSuggestionVotes.get(userID.getId()));
                    }

                    if(userVote_foodType.isPresent() && userVote_gameSuggestion.isPresent()) {
                        binding.groupCardTableReminderValue.setText("Erledigt");
                        binding.groupCardTableReminderValue.setTextColor(getResources().getColor(R.color.green, getTheme()));
                    } else {
                        binding.groupCardTableReminderValue.setText("Offen");
                        binding.groupCardTableReminderValue.setTextColor(getResources().getColor(R.color.lightRed, getTheme()));
                    }
                }
            } else {
                Log.d(TAG, "Failed to retrieve Data for next GameNight. ", task.getException());
            }
        });
    }

    private void initAndFillScrollView(ActivityGroupBinding binding) {
        CollectionReference gameNightRef = database.collection("GameNight");
        Query query = gameNightRef.whereEqualTo("PlaygroupID", playgroupID).orderBy("DateTime", Query.Direction.DESCENDING);

        query.get().addOnCompleteListener(task -> {

            if (task.isSuccessful()) {
                QuerySnapshot result = task.getResult();

                if(result.isEmpty()) {
                    Log.d(TAG, "Could not find GameNight. PlaygroupID: " + playgroupID);

                } else {
                    LinearLayout container = binding.groupGamenightsScrollViewContainer;

                    for(QueryDocumentSnapshot document : result) {
                        LinearLayout item = (LinearLayout) getLayoutInflater().inflate(R.layout.view_item_with_rating, null);
                        TextView dateValue = item.findViewById(R.id.view_item_with_rating);

                        setListenerOnItem(document, item);
                        setDateTime(document, dateValue);
                        setFoodRating(document, item);
                        setHostRating(document, item);
                        setGeneralRating(document, item);

                        container.addView(item);
                    }
                }
            } else {
                Log.d(TAG, "Failed to retrieve GameNights data", task.getException());
            }
        });
    }

    private void setListenerOnItem(QueryDocumentSnapshot document, LinearLayout item) {
        String gameNightID = document.getString("GameNightID");
        item.setOnClickListener(view -> {
            Intent gameNightActivityIntent = new Intent(this, GroupActivity.class);
            //Todo GameNightActivity existiert noch nicth
            gameNightActivityIntent.putExtra("GameNightID", gameNightID);
            startActivity(gameNightActivityIntent);
        });
    }

    private void setDateTime(QueryDocumentSnapshot document, TextView dateValue) {
        Timestamp timestamp = document.getTimestamp("DateTime");
        if(timestamp != null) {
            dateValue.setText((new SimpleDateFormat("dd-MM-yyyy").format(timestamp.toDate())));
        }else {
            dateValue.setText("");
        }
    }

    private void setGeneralRating(QueryDocumentSnapshot document, LinearLayout item) {
        Map<String, Long> generalRating = (Map) document.get("GeneralRatings");
        if(generalRating != null) {
            Long generalRatingUser = generalRating.get(userID.getId());
            TextView generalRatingElement = item.findViewById(R.id.general_rating);

            if(generalRatingUser != null) {
                generalRatingElement.setText(String.valueOf(generalRatingUser));
            }else {
                generalRatingElement.setText("Offen");
                generalRatingElement.setTextColor(getResources().getColor(R.color.lightRed));
            }
        }
    }

    private void setHostRating(QueryDocumentSnapshot document, LinearLayout item) {
        Map<String, Long> hostRating = (Map) document.get("HostRatings");
        if(hostRating != null) {
            Long hostRatingUser = hostRating.get(userID.getId());
            TextView hostRatingElement = item.findViewById(R.id.host_rating);

            if(hostRatingUser != null) {
                hostRatingElement.setText(String.valueOf(hostRatingUser));
            }else {
                hostRatingElement.setText("Offen");
                hostRatingElement.setTextColor(getResources().getColor(R.color.lightRed));
            }
        }
    }

    private void setFoodRating(QueryDocumentSnapshot document, LinearLayout item) {
        Map<String, Long> foodRatings = (Map) document.get("FoodRatings");
        if(foodRatings != null) {
            Long foodRatingOfUser = foodRatings.get(userID.getId());
            TextView foodRatingElement = item.findViewById(R.id.food_rating);

            if(foodRatingOfUser != null) {
                foodRatingElement.setText(String.valueOf(foodRatingOfUser));
            }else {
                foodRatingElement.setText("Offen");
                foodRatingElement.setTextColor(getResources().getColor(R.color.lightRed));
            }
        }
    }

}
