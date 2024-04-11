package com.halvaor.gamingknights.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.halvaor.gamingknights.R;
import com.halvaor.gamingknights.databinding.ActivityDashboardBinding;
import com.halvaor.gamingknights.domain.id.UserID;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public class DashboardActivity extends Activity {

    private static final String TAG = "DashboardActivity";
    private UserID userID;
    private FirebaseAuth auth;
    private FirebaseFirestore database;
    private String nextGameNightID = "";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        userID = new UserID(auth.getUid());
        database = FirebaseFirestore.getInstance();
        ActivityDashboardBinding binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        retrieveAndSetToolbarData(binding);
        getNextGameNightAndFillDashboard(binding);
        initAndFillScrollView(binding);

        binding.dasboardAddGroupButton.setOnClickListener(view -> {
                Intent newGroupActivityIntent = new Intent(this, NewGroupActivity.class);
                startActivity(newGroupActivityIntent);
        });

        binding.dashboardToolbarEditbutton.setOnClickListener(view -> {
            Intent profileActivityIntent = new Intent(this, ProfileActivity.class);
            startActivity(profileActivityIntent);
        });

        binding.dashboardCardView.setOnClickListener(view -> {
            if(this.nextGameNightID.isEmpty()) {
                Toast.makeText(this, "Keinen anstehenden Spieleabend gefunden", Toast.LENGTH_SHORT).show();
            } else {
                Intent gameNightActivity = new Intent(this, GameNightActivity.class);
                gameNightActivity.putExtra("gameNightID", this.nextGameNightID);
                startActivity(gameNightActivity);
            }
            //ToDo zunächst nur testweise implementiert; Muss noch auf die richtige GameNight verweisen
        });
    }

    private void initAndFillScrollView(ActivityDashboardBinding binding) {
        CollectionReference playgroupRef = database.collection("Playgroup");
        Query query = playgroupRef.where(Filter.arrayContains("Members", userID.getId()));

        query.get().addOnCompleteListener(task -> {

            if (task.isSuccessful()) {
                QuerySnapshot result = task.getResult();

                if(result.isEmpty()) {
                    Log.d(TAG, "Could not find Playgroups. UserID: " + userID.getId());

                } else {
                    LinearLayout container = binding.dashboardYourGroupsScrollViewContainer;

                    for(QueryDocumentSnapshot document : result) {
                        LinearLayout item = (LinearLayout) getLayoutInflater().inflate(R.layout.view_item, null);
                        TextView textView = item.findViewById(R.id.view_item);
                        String groupName = document.getString("Name");
                        String playgroupID = document.getId();

                        textView.setText(groupName);

                        item.setOnClickListener(view -> {
                            Intent groupActivityIntent = new Intent(this, GroupActivity.class);
                            groupActivityIntent.putExtra("playgroupID", playgroupID);
                            groupActivityIntent.putExtra("groupName", groupName);
                            startActivity(groupActivityIntent);
                        });

                        container.addView(item);
                    }

                }
            } else {
                Log.d(TAG, "Failed to retrieve Data for next GameNight. ", task.getException());
            }
        });
    }

    private void getNextGameNightAndFillDashboard(ActivityDashboardBinding binding) {
        CollectionReference gameNightRef = database.collection("GameNight");
        Query query = gameNightRef.whereArrayContains("Participants", userID.getId())
                        .whereGreaterThan("DateTime", Timestamp.now())
                                .orderBy("DateTime", Query.Direction.ASCENDING);

        query.get().addOnCompleteListener(task -> {

            if (task.isSuccessful()) {
                QuerySnapshot result = task.getResult();

                if(result.isEmpty()) {
                    Log.d(TAG, "Could not find next GameNight. UserID: " + userID.getId());
                } else {
                    this.nextGameNightID = result.getDocuments().get(0).getId();

                    binding.dashboardCardPlaygroupValue.setText(result.getDocuments().get(0).getString("PlaygroupName"));

                    Date date = result.getDocuments().get(0).getTimestamp("DateTime").toDate();
                    binding.dashboardCardDateTimeValue.setText(new SimpleDateFormat("dd-MM-yyyy HH:mm").format(date));

                    Map<String, Object> hostData = (Map<String, Object>) result.getDocuments().get(0).get("Host");
                    binding.dashboardCardHostValue.setText(hostData.get("FirstName") + " " + hostData.get("LastName"));

                    String hostAdress = hostData.get("Street") + " " + hostData.get("HouseNumber") + ", \n" + hostData.get("PostalCode") + " " + hostData.get("Town");
                    binding.dashboardCardAdressValue.setText(hostAdress);

                    //determin if there are votes left open to be done
                    //ToDo muss wohl noch verfeinert werden, damit auch foodOrder mit einbezogen werden 

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
                        binding.dashboardCardTableReminderValue.setText("Erledigt");
                        binding.dashboardCardTableReminderValue.setTextColor(getResources().getColor(R.color.green, getTheme()));
                    } else {
                        binding.dashboardCardTableReminderValue.setText("Offen");
                        binding.dashboardCardTableReminderValue.setTextColor(getResources().getColor(R.color.lightRed, getTheme()));
                    }

                    //toDo Intent zur entspr. GameNight muss noch eingefürgt werden, sobald diese Activity existiert.
                }
            } else {
                Log.d(TAG, "Failed to retrieve Data for next GameNight. ", task.getException());
            }
        });
    }


    private void retrieveAndSetToolbarData(ActivityDashboardBinding binding) {
        DocumentReference userRef = database.collection("User").document(userID.getId());
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
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
    }

}


