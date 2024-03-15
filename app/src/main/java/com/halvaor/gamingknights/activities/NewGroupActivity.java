package com.halvaor.gamingknights.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.halvaor.gamingknights.R;
import com.halvaor.gamingknights.databinding.ActivityNewGroupBinding;

import java.util.ArrayList;
import java.util.Optional;

public class NewGroupActivity extends Activity {

    private static final String TAG = "NewGroupActivity";
    private ArrayList<String> groupMember;
    private FirebaseFirestore database;

    //Todo Next: Array list muss dynamisch angezeigt werden nach update

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = FirebaseFirestore.getInstance();
        ActivityNewGroupBinding binding = ActivityNewGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.newGroupCancelButton.setOnClickListener(view -> {
            Intent dashboardActitvityIntent = new Intent(this, DashboardActivity.class);
            startActivity(dashboardActitvityIntent);
        });

        binding.newGroupAddUserButton.setOnClickListener(view -> {
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
                        Log.d(TAG, "Found User with given E-Mail: " + eMail);
                        groupMember.add(eMail);
                        binding.newGroupDescriptionEMail.setTextColor(getResources().getColor(R.color.black));
                    }
                } else {
                    Log.d(TAG, "Failed to retrieve data from collection \"User\". ", task.getException());
                }
            });
        });

        LinearLayout container = binding.newGroupScrollViewContainer;

        groupMember = new ArrayList<>();
        groupMember.add("UserID_2352454545");
        groupMember.add("UserID_2352422342");
        groupMember.add("UserID_df234234ff");
        groupMember.add("UserID_2352454545");
        groupMember.add("UserID_2352422342");
        groupMember.add("UserID_df234234ff");
        groupMember.add("UserID_2352454545");
        groupMember.add("UserID_2352422342");
        groupMember.add("UserID_df234234ff");
        groupMember.add("UserID_2352454545");
        groupMember.add("UserID_2352422342");
        groupMember.add("UserID_df234234ff");
        groupMember.add("UserID_2352454545");
        groupMember.add("UserID_2352422342");
        groupMember.add("UserID_df234234ff");
        groupMember.add("UserID_2352454545");
        groupMember.add("UserID_2352422342");
        groupMember.add("UserID_df234234ff");
        groupMember.add("UserID_2352454545");
        groupMember.add("UserID_2352422342");
        groupMember.add("UserID_df234234ff");
        groupMember.add("UserID_2352454545");
        groupMember.add("UserID_2352422342");
        groupMember.add("UserID_df234234ff");
        groupMember.add("UserID_2352454545");
        groupMember.add("UserID_2352422342");
        groupMember.add("UserID_df234234ff");
        groupMember.add("UserID_2352454545");
        groupMember.add("UserID_2352422342");
        groupMember.add("UserID_df234234ff");

        for(String member : groupMember) {
            LinearLayout item = (LinearLayout) getLayoutInflater().inflate(R.layout.view_item, null);
            TextView textView = item.findViewById(R.id.view_item);
            textView.setText(member);

            container.addView(item);
        }
    }

}
