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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.halvaor.gamingknights.domain.id.PlaygroupID;
import com.halvaor.gamingknights.domain.id.UserID;
import com.halvaor.gamingknights.R;
import com.halvaor.gamingknights.databinding.ActivityNewGroupBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class NewGroupActivity extends Activity {

    private static final String TAG = "NewGroupActivity";
    private List<String> groupMemberEMails = new ArrayList<>();
    private List<String> groupMemberUserIDs = new ArrayList<>();
    private FirebaseFirestore database;
    private FirebaseAuth auth;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        ActivityNewGroupBinding binding = ActivityNewGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //add current user
        groupMemberUserIDs.add(new UserID(auth.getUid()).getId());
        groupMemberEMails.add(Objects.requireNonNull(auth.getCurrentUser()).getEmail());
        fillUserView(binding);

        bindListeners(binding);
    }

    private void bindListeners(ActivityNewGroupBinding binding) {
        binding.newGroupCancelButton.setOnClickListener(view -> {
            Intent dashboardActitvityIntent = new Intent(this, DashboardActivity.class);
            startActivity(dashboardActitvityIntent);
        });
        binding.newGroupAddUserButton.setOnClickListener(view -> addUser(binding));
        binding.newGroupSubmitButton.setOnClickListener(view -> createGroup(binding));
    }

    private void fillUserView(ActivityNewGroupBinding binding) {
        LinearLayout container = binding.newGroupScrollViewContainer;
        container.removeAllViews();

        //fill container
        for (String member : groupMemberEMails) {
            LinearLayout item = (LinearLayout) getLayoutInflater().inflate(R.layout.view_item, null);
            TextView textView = item.findViewById(R.id.view_item);
            textView.setText(member);

            container.addView(item);
        }
    }

    private void addUser(ActivityNewGroupBinding binding) {
            String eMail = Optional.of(String.valueOf(binding.newGroupValueEMail.getText())).orElse("");

            CollectionReference userRef = database.collection("User");
            Query query = userRef.whereEqualTo("Email", eMail);

            //Check if given User can be found in the Database
            query.get().addOnCompleteListener(task -> {

                if(task.isSuccessful()) {
                    QuerySnapshot result = task.getResult();

                    if (result.isEmpty()) {
                        Log.i(TAG, "Given User can´t be found in the Database: E-Mail: " + eMail);
                        Toast.makeText(NewGroupActivity.this, "User konnte nicht gefunden werden", Toast.LENGTH_SHORT).show();
                        binding.newGroupValueEMail.setText("");
                        binding.newGroupDescriptionEMail.setTextColor(getResources().getColor(R.color.lightRed));
                    } else {
                        //duplication check
                        if(groupMemberEMails.contains(eMail) || groupMemberUserIDs.contains(result.getDocuments().get(0).getId())) {
                            Log.d(TAG, "User already added to list of groupmembers: " + eMail);
                            Toast.makeText(NewGroupActivity.this, "User bereits hinterlegt.", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "Found User with given E-Mail: " + eMail);
                            binding.newGroupDescriptionEMail.setTextColor(getResources().getColor(R.color.black));
                            binding.newGroupValueEMail.setText("");

                            //add User to Lists
                            groupMemberEMails.add(eMail);
                            groupMemberUserIDs.add(result.getDocuments().get(0).getId());

                            fillUserView(binding);
                        }
                    }
                } else {
                    Log.d(TAG, "Failed to retrieve data from collection \"User\". ", task.getException());
                }
            });
        }


    private void createGroup(ActivityNewGroupBinding binding) {
        CollectionReference playgroupRef = database.collection("Playgroup");
        //Generate documentID
        PlaygroupID playgroupID = new PlaygroupID(playgroupRef.document().getId());

        //Get and validate GroupName
        String groupName = Optional.of(String.valueOf(binding.newGroupValueGroupname.getText())).orElse("");
        if(groupName.isEmpty()) {
            Log.w(TAG, "GroupName is empty");
            binding.newGroupDescriptionGroupname.setTextColor(getResources().getColor(R.color.lightRed));
            Toast.makeText(NewGroupActivity.this, "Bitte gib einen Gruppennamen an.",
                    Toast.LENGTH_SHORT).show();
        } else {
            insertPlaygroupAndAddMemberships(playgroupRef, playgroupID, groupName);

            Intent dashboardIntent = new Intent(this, DashboardActivity.class);
            startActivity(dashboardIntent);
        }
    }

    private void insertPlaygroupAndAddMemberships(CollectionReference playgroupRef, PlaygroupID playgroupID, String groupName) {
        Runnable runnable = () -> {
            //insert Playgroup
            Map<String, Object> groupData = new HashMap<>();
            groupData.put("Name", groupName);
            groupData.put("Members", this.groupMemberUserIDs);

            playgroupRef.document(playgroupID.getId()).set(groupData);

            Map<String, Object> hostData = new HashMap<>();
            hostData.put("LastTimeHosting", Timestamp.now());

            //Update Membership
            for(String userID : this.groupMemberUserIDs) {
                DocumentReference userDocument = database.collection("User").document(userID);
                userDocument.update("Membership", FieldValue.arrayUnion(playgroupID.getId()));

                //Insert Hosts
                CollectionReference hostRef = database.collection("Playgroup").document(playgroupID.getId()).collection("Host");
                hostRef.document(userID).set(hostData);
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();
    }

}
