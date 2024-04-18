package com.halvaor.gamingknights.dialog;

import android.app.Dialog;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener  {

    private static final String TAG = "TimePickerFragment";
    private TimePickerInterface tpi;

   public TimePickerFragment(TimePickerInterface tpi) {
       this.tpi = tpi;
   }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker.
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        return new TimePickerDialog(getActivity(), this, hour, minute,
                true);
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
        tpi.bindTime(hour, minute);
    }

}
