package com.halvaor.gamingknights.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.halvaor.gamingknights.IDs.PlaygroupID;
import com.halvaor.gamingknights.IDs.UserID;
import com.halvaor.gamingknights.R;
import com.halvaor.gamingknights.databinding.ActivityNewGroupBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NewGroupActivity extends Activity {

    private static final String TAG = "NewGroupActivity";
    private List<String> groupMemberEMail = new ArrayList<>();
    private List<String> groupMemberUserID = new ArrayList<>();
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
        groupMemberUserID.add(new UserID(auth.getUid()).getId());
        groupMemberEMail.add("Aktueller Nutzer");
        fillUserView(binding);

        //setListeners
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
        for (String member : groupMemberEMail) {
            LinearLayout item = (LinearLayout) getLayoutInflater().inflate(R.layout.view_item, null);
            TextView textView = item.findViewById(R.id.view_item);
            textView.setText(member);

            container.addView(item);
        }
    }

    private void addUser(ActivityNewGroupBinding binding) {
            String eMail = Optional.ofNullable(binding.newGroupValueEMail.getText().toString()).orElse("");

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
                        if(groupMemberEMail.contains(eMail) || groupMemberUserID.contains(result.getDocuments().get(0).getId())) {
                            Log.d(TAG, "User already added to list of groupmembers: " + eMail);
                            Toast.makeText(NewGroupActivity.this, "User bereits hinterlegt.", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "Found User with given E-Mail: " + eMail);
                            groupMemberEMail.add(eMail);
                            binding.newGroupDescriptionEMail.setTextColor(getResources().getColor(R.color.black));
                            binding.newGroupValueEMail.setText("");

                            //save userID of added user
                            groupMemberUserID.add(result.getDocuments().get(0).getId());

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
        PlaygroupID playgroupID = new PlaygroupID(playgroupRef.document().getId());

        //Get and validate Groupname
        String groupName = Optional.ofNullable(binding.newGroupValueGroupname.getText().toString()).orElse("");
        if(groupName.isEmpty()) {
            Log.w(TAG, "GroupName is empty");
            binding.newGroupDescriptionGroupname.setTextColor(getResources().getColor(R.color.lightRed));
            Toast.makeText(NewGroupActivity.this, "Bitte gib einen Gruppennamen an.",
                    Toast.LENGTH_SHORT).show();
        } else {

            Map<String, Object> groupData = new HashMap<>();
            groupData.put("Name", groupName);
            groupData.put("Members", groupMemberUserID);

            playgroupRef.document(playgroupID.getId()).set(groupData);

            Intent dashboardIntent = new Intent(this, DashboardActivity.class);
            startActivity(dashboardIntent);
        }
    }

}
