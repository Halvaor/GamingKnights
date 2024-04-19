package com.halvaor.gamingknights.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.halvaor.gamingknights.domain.id.UserID;
import com.halvaor.gamingknights.R;
import com.halvaor.gamingknights.databinding.ActivityEditGroupBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        this.database = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.userMails = new ArrayList<>();
        this.playgroupID = getIntent().getExtras().getString("playgroupID");
        this.groupName = getIntent().getExtras().getString("groupName");
        this.userID = new UserID(auth.getUid());

        determineGroupMembers();
        bindListeners();
    }

    private void bindListeners() {
        this.binding.editGroupCancelButton.setOnClickListener(view -> {
            Intent groupActivityIntent = new Intent(this, GroupActivity.class);
            groupActivityIntent.putExtra("playgroupID", playgroupID);
            groupActivityIntent.putExtra("groupName", groupName);
            startActivity(groupActivityIntent);
        });

        this.binding.editGroupAddUserButton.setOnClickListener(view -> {
            validateAndAddUser();
        });

        this.binding.editGroupChangeGroupNameButton.setOnClickListener(view -> {
            changeGroupName();
        });

        this.binding.editGroupDeleteGroupButton.setOnClickListener(view -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage("Möchten sie die Gruppe wirklich löschen?");
            dialogBuilder.setCancelable(true);

            dialogBuilder.setPositiveButton("Ja", (dialog, id) -> {
                dialog.cancel();
                deletePlaygroup();

                Intent dashboardActivityIntent = new Intent(this, DashboardActivity.class);
                startActivity(dashboardActivityIntent);
            });

            dialogBuilder.setNegativeButton("Nein", (dialog, id) -> {
                dialog.cancel();
            });

            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();
        });
    }
    private void deletePlaygroup() {
        Runnable runnable = () -> {
            removeMemberships();
            deleteRelatedGameNights();
            deletePlaygroupFromPlaygroupCollection();
        };

        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void removeMemberships() {
        Query query = database.collection("User").whereArrayContains("Membership", this.playgroupID);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot result = task.getResult();
                if (result.isEmpty()) {
                    Log.d(TAG, "Could not find any User who is a member of playgroupID: " + this.playgroupID);
                } else {
                    for(QueryDocumentSnapshot documentSnapshot : result) {
                        DocumentReference userRef = database.collection("User").document(documentSnapshot.getId());
                        userRef.update("Membership", FieldValue.arrayRemove(this.playgroupID));
                        Log.d(TAG, "Successfully deleted playgroupID of Membership-Array for User: " + documentSnapshot.getId());
                    }
                }
            } else {
                Log.d(TAG, "Failed to retrieve members of playgroupID: " + this.playgroupID);
            }
        });
    }

    private void deleteRelatedGameNights() {
        Query query = database.collection("GameNight").whereEqualTo("PlaygroupID", this.playgroupID);
        query.get().addOnCompleteListener(getGameNightsTask -> {
            if (getGameNightsTask.isSuccessful()) {
                QuerySnapshot result = getGameNightsTask.getResult();
                if (result.isEmpty()) {
                    Log.d(TAG, "Could not find any GameNight for playgroupID: " + this.playgroupID);
                } else {
                    for(QueryDocumentSnapshot documentSnapshot : result) {
                        DocumentReference gameNightRef = database.collection("GameNight").document(documentSnapshot.getId());
                        gameNightRef.delete().addOnCompleteListener(deleteGameNightTask -> {
                            if(deleteGameNightTask.isSuccessful()) {
                                Log.d(TAG, "Successfully deleted GameNight with ID: " + gameNightRef.getId());
                            } else {
                                Log.d(TAG, "Failed to delete GameNight with ID: " + gameNightRef.getId());
                            }
                        });
                    }
                }
            } else {
                Log.d(TAG, "Failed to retrieve GameNights of playgroupID: " + this.playgroupID);
            }
        });
    }

    private void deletePlaygroupFromPlaygroupCollection() {
        DocumentReference playgroupRef = database.collection("Playgroup").document(this.playgroupID);
        playgroupRef.delete().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Log.d(TAG, "Successfully deleted Playgroup with ID: " + this.playgroupID);
            } else {
                Log.d(TAG, "Failed to delete Playgroup with ID: " + this.playgroupID);
            }
        });
    }

    private void changeGroupName() {
        String groupName = Optional.of(String.valueOf(binding.editGroupValueGroupname.getText())).orElse("");

        if(groupName.isEmpty()) {
            binding.editGroupDescriptionGroupname.setTextColor(getResources().getColor(R.color.lightRed));
            Toast.makeText(this, "Bitte geben Sie einen Gruppenname an.", Toast.LENGTH_SHORT).show();
        } else {
            binding.editGroupDescriptionGroupname.setTextColor(getResources().getColor(R.color.black));

            Runnable runnable = () -> {
                updateGroupNameInPlaygroup(groupName);
                updateGroupNameInUpcomingGameNights(groupName);
            };

            Thread thread = new Thread(runnable);
            thread.start();
        }
        Toast.makeText(this, "Änderung veranlasst.", Toast.LENGTH_SHORT).show();
    }

    private void updateGroupNameInUpcomingGameNights(String groupName) {
        Map<String, Object> updateGameNightsData = new HashMap<>();
        updateGameNightsData.put("PlaygroupName", groupName);

        Query upcomingGameNights = database.collection("GameNight")
                .whereGreaterThan("DateTime", Timestamp.now())
                .whereEqualTo("PlaygroupID", this.playgroupID);

        upcomingGameNights.get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                QuerySnapshot result = task.getResult();
                if(result.isEmpty()) {
                    Log.d(TAG, "Could not find next GameNight for playgroupID: " + playgroupID);
                }else {
                    for(QueryDocumentSnapshot gameNightDocument : result) {
                        DocumentReference gameNightRef = database.collection("GameNight").document(gameNightDocument.getId());
                        gameNightRef.update(updateGameNightsData).addOnCompleteListener(updateTask -> {
                            if(updateTask.isSuccessful()) {
                                Log.d(TAG, "Successfully changed PlaygroupName to " + updateGameNightsData.get("PlaygroupName") + " for upcoming GameNights");
                            } else {
                                Log.d(TAG, "Failed to change PlaygroupName to " + updateGameNightsData.get("PlaygroupName") + " for upcoming GameNights");
                            }
                        });
                    }
                }
            } else {
                Log.d(TAG, "Failed to retrieve GameNights with PlaygroupID: " + this.playgroupID);
            }
        });
    }

    private void updateGroupNameInPlaygroup(String groupName) {
        Map<String, Object> updatePlaygroupData = new HashMap<>();
        updatePlaygroupData.put("Name", groupName);
        DocumentReference playgroupRef = database.collection("Playgroup").document(this.playgroupID);

        playgroupRef.update(updatePlaygroupData).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Log.d(TAG, "Successfully changed PlaygroupName to " + updatePlaygroupData.get("Name") + " for Playgroup: " + this.playgroupID);
            } else {
                Log.d(TAG, "Failed to change PlaygroupName to " + updatePlaygroupData.get("Name") + " for Playgroup: " + this.playgroupID);
            }
        });
    }

    private void validateAndAddUser() {
        Runnable runnable = () -> {
            String eMail = Optional.of(String.valueOf(binding.editGroupValueEMail.getText())).orElse("");

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
                addUserToHosts(userID);
                addUserToGameNightParticipants(userID);
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

    private void addUserToHosts(String userID) {
        Map<String, Object> hostData = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(1900, Calendar.JANUARY, 1);
        hostData.put("LastTimeHosting", new Timestamp(calendar.getTime()));

        CollectionReference hostRef = database.collection("Playgroup").document(playgroupID).collection("Host");
        hostRef.document(userID).set(hostData).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Log.d(TAG, "Successfully added user " + userID + " to the Hosts of playgroup " + this.playgroupID + ".");
            }else {
                Log.d(TAG, "Failed to add user " + userID + " to the Hosts of playgroup " + this.playgroupID + ".");
            }
        });
    }

    private void addUserToGameNightParticipants(String userID) {
        Map<String, Object> participant = new HashMap<>();
        participant.put("Participants", FieldValue.arrayUnion(userID));

        Query upcomingGameNights = database.collection("GameNight")
                .whereGreaterThan("DateTime", Timestamp.now());

        upcomingGameNights.get().addOnSuccessListener(queryDocumentSnapshots -> {

            for(QueryDocumentSnapshot document : queryDocumentSnapshots) {
                database.collection("GameNight").document(document.getId())
                        .update(participant).addOnSuccessListener(unused -> {
                            Log.d(TAG, "Successfully added user " + userID + " to Participants of playgroup.");
                        })
                        .addOnFailureListener(e -> {
                            Log.d(TAG, "Failed to added user " + userID + " to Participants of playgroup.", e);
                        });
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
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage("Möchten sie das Mitglied wirklich löschen?");
            dialogBuilder.setCancelable(true);

            dialogBuilder.setPositiveButton(
                    "Ja",
                    (dialog, id) -> {
                        dialog.cancel();

                        String userIDToRemove = (String) view.getTag();
                        removeUserFromPlaygroup(userIDToRemove, this.playgroupID);

                        //Removed user was current user
                        if(userIDToRemove.equals(new UserID(auth.getUid()).getId())) {
                            Intent dashboardActivityIntent = new Intent(this, DashboardActivity.class);
                            startActivity(dashboardActivityIntent);
                        } else {
                            this.container.removeAllViews();
                            determineGroupMembers();
                        }
                    });

            dialogBuilder.setNegativeButton(
                    "Nein",
                    (dialog, id) -> {
                        dialog.cancel();
                    });

            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();
        });
    }

    private void removeUserFromPlaygroup(String userIDToRemove, String playgroupID) {
        Runnable runnable = () -> {

            //remove membership
            DocumentReference userRef = database.collection("User").document(userIDToRemove);
            userRef.update("Membership", FieldValue.arrayRemove(playgroupID));

            //remove from members
            DocumentReference playgroupRef = database.collection("Playgroup").document(playgroupID);
            playgroupRef.update("Members", FieldValue.arrayRemove(userIDToRemove));

            //remove from Host
            DocumentReference hostRef = database.collection("Playgroup").document(playgroupID).collection("Host").document(userIDToRemove);
            hostRef.delete().addOnCompleteListener((task) -> {
                if(task.isSuccessful()) {
                    Log.d(TAG, "User " + userIDToRemove + " was successfully removed from Host-Collection.");
                } else {
                    Log.d(TAG, "Failed to remove User " + userIDToRemove + "from Host-Collection.");
                }
            });

            //remove from upcoming game nights
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
