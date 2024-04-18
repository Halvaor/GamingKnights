package com.halvaor.gamingknights.dialog;

import android.app.DatePickerDialog;
import android.app.Dialog;

import android.os.Bundle;
import android.widget.DatePicker;

import androidx.fragment.app.DialogFragment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private DatePickerInterface dpi;

    public DatePickerFragment(DatePickerInterface dpi) {
        this.dpi = dpi;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
       final Calendar c = Calendar.getInstance();
       int year = c.get(Calendar.YEAR);
       int month = c.get(Calendar.MONTH);
       int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), this, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());

        return  datePickerDialog;
    }

    @Override
    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
        dpi.bindDate(datePicker);
    }
}
