package com.tripplanner.add_trip;


import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.tripplanner.Constants;
import com.tripplanner.R;
import com.tripplanner.add_trip.place.PlaceAutoSuggestAdapter;
import com.tripplanner.alarm.NotificationActivity;
import com.tripplanner.data_layer.local_data.entity.Note;
import com.tripplanner.data_layer.local_data.entity.Trip;
import com.tripplanner.databinding.FragmentAddTripBinding;
import com.tripplanner.home.HomeFragment;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static android.content.Context.ALARM_SERVICE;
import static com.tripplanner.Constants.STATUS_UPCOMING;


public class AddTripFragment extends Fragment {
    private AddTripViewModel tripViewModel;
    private FragmentAddTripBinding fragmentAddTripBinding;
    private NoteAdapter noteAdapter;
    public static final int ALARM_ID = 200;
    private static final String TAG = "AddTripFragment";
    private Trip trip;
    PlaceAutoSuggestAdapter Fromadapter;
    PlaceAutoSuggestAdapter toAdapter;
    private DatePickerDialog.OnDateSetListener date;
    private DatePickerDialog.OnDateSetListener dateRound;
    private Calendar myCalendar;

    private Calendar calendar2;

    public AddTripFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentAddTripBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_add_trip, container, false);
        View root = fragmentAddTripBinding.getRoot();
        tripViewModel = new ViewModelProvider(getActivity()).get(AddTripViewModel.class);
        Fromadapter = new PlaceAutoSuggestAdapter(getContext(), R.layout.pop_up_item);
        toAdapter = new PlaceAutoSuggestAdapter(getContext(), R.layout.pop_up_item);
        fragmentAddTripBinding.placeView.fromEt.setAdapter(Fromadapter);
        fragmentAddTripBinding.placeView.toEt.setAdapter(toAdapter);
        fragmentAddTripBinding.placeView.fromEt.setOnItemClickListener((adapterView, view, i, l) -> trip.setStartPoint(Fromadapter.getPlace(i)));
        fragmentAddTripBinding.placeView.toEt.setOnItemClickListener((adapterView, view, i, l) -> trip.setEndPoint(toAdapter.getPlace(i)));
        trip = AddTripFragmentArgs.fromBundle(getArguments()).getTrip();
        setupTimeDatePicker();
        setupTimeDatePickerRound();
        fragmentAddTripBinding.setFragmet(this);
        noteAdapter = new NoteAdapter(new ArrayList<>());
        fragmentAddTripBinding.addNoteCard.noteRv.setAdapter(noteAdapter);
        if (trip == null) {
            trip = new Trip();
        } else {
            timeFormat(trip.getTripDate());
            myCalendar.setTime(trip.getTripDate());
            updateLabel();
            List<Note> noteList = tripViewModel.getTripNote(trip.getId());
            noteAdapter.addList(new ArrayList<>(noteList));
            fragmentAddTripBinding.save.setText("Update");
        }
        trip.setOnline(isNetworkConnected());
        fragmentAddTripBinding.setTrip(trip);
        return root;
    }

    private void timeFormat(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("K:mm a");
        String formattedTime = sdf.format(date);
        fragmentAddTripBinding.dateView.time.setText(formattedTime);
    }

    private void roundtimeFormat(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("K:mm a");
        String formattedTime = sdf.format(date);
        fragmentAddTripBinding.roundDateView.time.setText(formattedTime);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (capabilities != null) {
                    //...
                    return !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
                }
            } else {
                // current code
                return true;
            }
        }
        return false;
    }


    private void setupTimeDatePicker() {
        myCalendar = Calendar.getInstance();

        date = (view, year, monthOfYear, dayOfMonth) -> {
            // TODO Auto-generated method stub
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel();
        };

        fragmentAddTripBinding.dateView.datePicker.setOnClickListener(view ->
        {
            myCalendar.get(Calendar.YEAR);

            DatePickerDialog datePicker =  new DatePickerDialog(getActivity(), date, myCalendar
                    .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH));
            datePicker.getDatePicker().setMinDate(myCalendar.getTimeInMillis());
            datePicker.show();
        });
        fragmentAddTripBinding.dateView.timePicker.setOnClickListener(view -> openTimePicker());
    }

    private void setupTimeDatePickerRound() {
        calendar2 = Calendar.getInstance();

        dateRound = (view, year, monthOfYear, dayOfMonth) -> {
            // TODO Auto-generated method stub
            calendar2.set(Calendar.YEAR, year);
            calendar2.set(Calendar.MONTH, monthOfYear);
            calendar2.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabelRound();
        };

        fragmentAddTripBinding.roundDateView.datePicker.setOnClickListener(view ->
        {
            calendar2.get(Calendar.YEAR);
           DatePickerDialog datePickerDialog= new DatePickerDialog(getActivity(), dateRound, calendar2
                    .get(Calendar.YEAR), calendar2.get(Calendar.MONTH),
                    calendar2.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMinDate(myCalendar.getTimeInMillis());
            datePickerDialog.show();
        });
        fragmentAddTripBinding.roundDateView.timePicker.setOnClickListener(view -> openTimePickerRound());

    }

    // set day and date in date text view
    private void updateLabel() {
        String myFormat = "dd/MM/yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        Date date = myCalendar.getTime();
        fragmentAddTripBinding.dateView.date.setText(sdf.format(date));
    }

    private void updateLabelRound() {
        String myFormat = "dd/MM/yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        Date date = calendar2.getTime();
        fragmentAddTripBinding.roundDateView.date.setText(sdf.format(date));
    }

    private void openTimePicker() {
        TimePickerDialog mTimePicker;
        mTimePicker = new TimePickerDialog(getContext(), (timePicker, selectedHour, selectedMinute) -> {
            myCalendar.set(Calendar.HOUR_OF_DAY, selectedHour);
            myCalendar.set(Calendar.MINUTE, selectedMinute);
            timeFormat(myCalendar.getTime());
        }, 12, 0, true);
        mTimePicker.setTitle("Select Time");
        mTimePicker.show();
    }

    private void openTimePickerRound() {
        TimePickerDialog mTimePicker;
        mTimePicker = new TimePickerDialog(getContext(), (timePicker, selectedHour, selectedMinute) -> {
            calendar2.set(Calendar.HOUR_OF_DAY, selectedHour);
            calendar2.set(Calendar.MINUTE, selectedMinute);
            roundtimeFormat(calendar2.getTime());
        }, 12, 0, true);
        mTimePicker.setTitle("Select Time");
        mTimePicker.show();
    }

    public void roundTrip(View view) {
        Log.d(TAG, "roundTrip: ");
        if (fragmentAddTripBinding.placeView.roundTrip.isChecked()) {
            fragmentAddTripBinding.roundDateView.getRoot().setVisibility(View.VISIBLE);
        } else
            fragmentAddTripBinding.roundDateView.getRoot().setVisibility(View.GONE);
    }

    public void showNoteNameDialog() {
        MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(getContext());
        alert.setTitle("Note Name");
        // Set an EditText view to get user input
        final TextInputEditText input = new TextInputEditText(getContext());
        alert.setView(input);
        alert.setPositiveButton("Confirm", (dialog, whichButton) -> {
            if (!input.getText().toString().isEmpty()) {
                Note note = new Note();
                note.setNoteName(input.getText().toString());
                noteAdapter.addNote(note);

            }
        });

        alert.setNegativeButton("Cancel", (dialog, whichButton) -> {
            // Canceled.
        });

        alert.show();
    }

    public void insertTip(View view) {


        if (tripViewModel.validate(fragmentAddTripBinding)) {
            if (trip.getStartPoint() != null && trip.getEndPoint() != null) {
                view.setEnabled(false);
                trip.setTripDate(myCalendar.getTime());
                trip.setTripStatus(STATUS_UPCOMING);
                tripViewModel.insertTrip(trip, noteAdapter.getNotes()).observe(getActivity(), aLong -> {
                    Toast.makeText(getContext(), "Inserted Successfully", Toast.LENGTH_SHORT).show();
                    setAlarmManger(aLong.intValue());
                    if (fragmentAddTripBinding.placeView.roundTrip.isChecked()) {
                        Trip roundTrip = setRounTrip();
                        ArrayList<Note> notes = noteAdapter.getNotes();
                        tripViewModel.insertTrip(roundTrip, notes).observe(getViewLifecycleOwner(), bLong -> setAlarmManger(bLong.intValue()));
                    }
                    goback(view);
                    view.setEnabled(true);
                });
            }
        } else
            Toast.makeText(getContext(), "Please select places from list", Toast.LENGTH_LONG).show();
    }

    private Trip setRounTrip() {
        Trip trip = new Trip();
        trip.setName(fragmentAddTripBinding.tripToolBar.tripName.getText().toString());
        trip.setStartPoint(this.trip.getEndPoint());
        trip.setEndPoint(this.trip.getStartPoint());
        trip.setTripDate(calendar2.getTime());
        trip.setTripStatus(STATUS_UPCOMING);
        return trip;
    }

    private void setAlarmManger(int tripId) {

        // Set the alarm to start at 8:30 a.m.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.setTime(trip.getTripDate());
        Intent notifyIntent = new Intent(getContext(), NotificationActivity.TripAlarmReciver.class);
        notifyIntent.putExtra(Constants.TRIPS, tripId);
        final PendingIntent notifyPendingIntent = PendingIntent.getBroadcast
                (getContext(), tripId, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        final AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(ALARM_SERVICE);

        if (alarmManager != null) {
            Log.d(TAG, "setAlarmManger: " + alarmManager);
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    notifyPendingIntent);

        }
    }

    public void goback(View v) {
        Navigation.findNavController(v).popBackStack();
    }
}
