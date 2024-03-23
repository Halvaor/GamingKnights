package com.halvaor.gamingknights.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.halvaor.gamingknights.IDs.UserID;
import com.halvaor.gamingknights.R;
import com.halvaor.gamingknights.databinding.ActivityEditGroupBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EditGroupActivity extends Activity {

    private static final String TAG = "EditGroupActivity";
    private FirebaseFirestore database;
    private FirebaseAuth auth;
    private String playgroupID;
    private String groupName;
    private UserID userID;
    private List<String> userMails;
    private LinearLayout container;
    private ActivityEditGroupBinding binding;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = ActivityEditGroupBinding.inflate(getLayoutInflater());
        setContentView(this.binding.getRoot());
        container = this.binding.editGroupScrollViewContainer;

        database = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        userMails = new ArrayList<>();

        this.playgroupID = getIntent().getExtras().getString("playgroupID");
        this.groupName = getIntent().getExtras().getString("groupName");
        this.userID = new UserID(auth.getUid());

        this.binding.editGroupCancelButton.setOnClickListener(view -> {
            Intent groupActivityIntent = new Intent(this, GroupActivity.class);
            groupActivityIntent.putExtra("playgroupID", playgroupID);
            groupActivityIntent.putExtra("groupName", groupName);
            startActivity(groupActivityIntent);
        });

        determineGroupMembers();

        this.binding.editGroupAddUserButton.setOnClickListener(view -> {
            validateAndAddUser();
        });

    }

    private void validateAndAddUser() {
        Runnable runnable = () -> {
            String eMail = Optional.ofNullable(binding.editGroupValueEMail.getText().toString()).orElse("");

            CollectionReference userRef = database.collection("User");
            Query query = userRef.whereEqualTo("Email", eMail);

            //Check if given User can be found in the Database
            query.get().addOnCompleteListener(task -> {

                if (task.isSuccessful()) {
                    QuerySnapshot result = task.getResult();

                    if (result.isEmpty()) {
                        Log.i(TAG, "Given User can´t be found in the Database: E-Mail: " + eMail);
                        Toast.makeText(EditGroupActivity.this, "User konnte nicht gefunden werden", Toast.LENGTH_SHORT).show();
                        binding.editGroupValueEMail.setText("");
                        binding.editGroupDescriptionEMail.setTextColor(getResources().getColor(R.color.lightRed));
                    } else {
                        //duplication check
                        if (userMails.contains(eMail)) {
                            Log.d(TAG, "User already added to list of groupmembers: " + eMail);
                            Toast.makeText(EditGroupActivity.this, "User bereits hinterlegt.", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "Found User with given E-Mail: " + eMail);
                            String userID = result.getDocuments().get(0).getId();
                            binding.editGroupDescriptionEMail.setTextColor(getResources().getColor(R.color.black));
                            binding.editGroupValueEMail.setText("");

                            addUserToPlaygroup(userID);
                        }
                    }
                } else {
                    Log.d(TAG, "Failed to retrieve data from collection \"User\". ", task.getException());
                }
            });
        };

        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void addUserToPlaygroup(String userID) {
        DocumentReference playgroupRef = database.collection("Playgroup").document(this.playgroupID);
        playgroupRef.update("Members", FieldValue.arrayUnion(userID)).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Log.d(TAG, "Successfully added user " + userID + " to members of the Playgroup " + this.playgroupID + ".");
                addMembership(userID);
            }else {
                Log.d(TAG, "Failed to add user " + userID + " to members of the Playgroup " + this.playgroupID + ".");
            }
        });
    }

    private void addMembership(String userID) {
        DocumentReference userRef = database.collection("User").document(userID);
        userRef.update("Membership", FieldValue.arrayUnion(this.playgroupID)).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Log.d(TAG, "Successfully added playgroup " + this.playgroupID + " To the memberships of user " + userID + ".");
                this.container.removeAllViews();
                determineGroupMembers();
            }else {
                Log.d(TAG, "Failed to add playgroup " + this.playgroupID + " To the memberships of user " + userID + ".");
            }
        });
    }


    private void determineGroupMembers() {
        CollectionReference userRef = database.collection("User");
        Query query = userRef.whereArrayContains("Membership", playgroupID);
        userMails.clear();

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot result = task.getResult();
                if (result.isEmpty()) {
                    Log.d(TAG, "Could not find any user of given Playgroup. PlaygroupID: " + playgroupID);
                } else {
                    for (QueryDocumentSnapshot document : result) {
                        LinearLayout item = (LinearLayout) getLayoutInflater().inflate(R.layout.view_item_with_button, null);
                        TextView userMailElement = item.findViewById(R.id.view_item);
                        String userMail = document.getString("Email");
                        if (userMail != null) {
                            userMails.add(userMail);
                            userMailElement.setText(userMail);

                            configureDeleteButton(item, document);

                            this.container.addView(item);
                        }
                    }
                }
            }else {
                Log.d(TAG, "Could not find any Groupmembers. PlaygroupID: " + playgroupID);
            }
        });
    }

    private void configureDeleteButton(LinearLayout item, QueryDocumentSnapshot document) {
        Button button = item.findViewById(R.id.item_button);
        button.setTag(document.getId());

        button.setOnClickListener(view -> {
            String userIDToRemove = (String) view.getTag();
            removeUserFromPlaygroup(userIDToRemove, this.playgroupID);

            //Removed user was myself
            if(userIDToRemove.equals(new UserID(auth.getUid()).getId())) {
                Intent dashboardActivityIntent = new Intent(this, DashboardActivity.class);
                startActivity(dashboardActivityIntent);
            } else {
                this.container.removeAllViews();
                determineGroupMembers();
            }
        });
    }

    private void removeUserFromPlaygroup(String userIDToRemove, String playgroupID) {
        Runnable runnable = () -> {

            DocumentReference userRef = database.collection("User").document(userIDToRemove);
            userRef.update("Membership", FieldValue.arrayRemove(playgroupID));

            DocumentReference playgroupRef = database.collection("Playgroup").document(playgroupID);
            playgroupRef.update("Members", FieldValue.arrayRemove(userIDToRemove));

            Query upcomingGameNights = database.collection("GameNight")
                    .whereGreaterThan("DateTime", Timestamp.now())
                    .whereArrayContains("Participants", userIDToRemove);

            upcomingGameNights.get().addOnCompleteListener(task -> {
                if(task.isSuccessful()) {
                    QuerySnapshot result = task.getResult();
                    if(result.isEmpty()) {
                        Log.d(TAG, "Could not find next GameNight for UserID: " + userID.getId());
                    }else {
                        for(QueryDocumentSnapshot gameNightDocument : result) {
                            DocumentReference gameNightRef = database.collection("GameNight").document(gameNightDocument.getId());
                            gameNightRef.update("Participants", FieldValue.arrayRemove(userIDToRemove));
                            Log.d(TAG, "Successfully deleted user " + userIDToRemove + "from upcoming GameNights");
                        }
                    }
                } else {
                    Log.d(TAG, "Failed to retrieve GameNights with Participant: " + userIDToRemove);
                }
            });
        };

        Thread thread = new Thread(runnable);
        thread.start();
    }

}
