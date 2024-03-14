package com.halvaor.gamingknights.activities;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.rpc.context.AttributeContext;
import com.halvaor.gamingknights.R;
import com.halvaor.gamingknights.adapter.CustomAdapter;
import com.halvaor.gamingknights.databinding.ActivityNewGroupBinding;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class NewGroupActivity extends Activity {
    private ArrayList<String> groupmember;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityNewGroupBinding binding = ActivityNewGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        LinearLayout container = binding.newGroupScrollViewContainer;

        groupmember = new ArrayList<>();
        groupmember.add("UserID_2352454545");
        groupmember.add("UserID_2352422342");
        groupmember.add("UserID_df234234ff");
        groupmember.add("UserID_2352454545");
        groupmember.add("UserID_2352422342");
        groupmember.add("UserID_df234234ff");
        groupmember.add("UserID_2352454545");
        groupmember.add("UserID_2352422342");
        groupmember.add("UserID_df234234ff");
        groupmember.add("UserID_2352454545");
        groupmember.add("UserID_2352422342");
        groupmember.add("UserID_df234234ff");
        groupmember.add("UserID_2352454545");
        groupmember.add("UserID_2352422342");
        groupmember.add("UserID_df234234ff");
        groupmember.add("UserID_2352454545");
        groupmember.add("UserID_2352422342");
        groupmember.add("UserID_df234234ff");
        groupmember.add("UserID_2352454545");
        groupmember.add("UserID_2352422342");
        groupmember.add("UserID_df234234ff");
        groupmember.add("UserID_2352454545");
        groupmember.add("UserID_2352422342");
        groupmember.add("UserID_df234234ff");
        groupmember.add("UserID_2352454545");
        groupmember.add("UserID_2352422342");
        groupmember.add("UserID_df234234ff");
        groupmember.add("UserID_2352454545");
        groupmember.add("UserID_2352422342");
        groupmember.add("UserID_df234234ff");

        for(String member : groupmember) {
            LinearLayout item = (LinearLayout) getLayoutInflater().inflate(R.layout.view_item, null);
            TextView textView = item.findViewById(R.id.view_item);
            textView.setText(member);

            container.addView(item);
        }
    }

}
