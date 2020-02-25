package com.tripplanner.data_layer;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.tripplanner.data_layer.local_data.entity.Note;
import com.tripplanner.data_layer.local_data.entity.Place;
import com.tripplanner.data_layer.local_data.entity.Trip;
import com.tripplanner.data_layer.local_data.entity.User;
import com.tripplanner.data_layer.local_data.Room;
import com.tripplanner.data_layer.local_data.TripDao;
import com.tripplanner.data_layer.remote.Firebase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.tripplanner.Constants.END_POINT;
import static com.tripplanner.Constants.NAME;
import static com.tripplanner.Constants.ONLINE;
import static com.tripplanner.Constants.START_POINT;
import static com.tripplanner.Constants.TRIPS;
import static com.tripplanner.Constants.TRIP_STATUS;
import static com.tripplanner.Constants.TRIP_TYPE;
import static com.tripplanner.Constants.USER_ID;


@Singleton
public class Repository {
    private static final String TAG = "Repository";
    private TripDao tripDao;
    private Firebase firebase;
    static User user;

    // TODO create repo
    @Inject
    public Repository(Application application) {
        Room db = Room.getInstance(application);
        tripDao = db.tripDao();
        this.firebase = new Firebase();
    }

    public LiveData<List<Trip>> getUpComingTrips() {


        getRemoteUpComingTrips();
        // return local data until load updated trips from server
        return tripDao.getTrips(user.getId(), Trip.STATUS_UPCOMING);

    }

    public LiveData<List<Trip>> getDoneTrips() {
        getRemoteDoneTrips();
        // return local data until load updated trips from server
        return tripDao.getTrips(user.getId(), Trip.STATUS_DONE);

    }

    public LiveData<List<Trip>> getCancelTrips() {


        getRemoteCanceledTrips();
        // return local data until load updated trips from server
        return tripDao.getTrips(user.getId(), Trip.STATUS_CANCELED);

    }

    private void getRemoteUpComingTrips() {

        Date currentTime = Calendar.getInstance().getTime();
        firebase.getUserDocument(user.getId()).collection(TRIPS)
                .whereGreaterThanOrEqualTo("date", currentTime)
                .get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ArrayList<Trip> upComingTrips = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    upComingTrips.add(getTrip(document));

                }
                // update database
                tripDao.insertTrip(upComingTrips);
            } else {
                Log.w(TAG, "Error getting documents.", task.getException());
            }
        });


    }


    public void getRemoteDoneTrips() {
        Date currentTime = Calendar.getInstance().getTime();
        firebase.getUserDocument(user.getId()).collection(TRIPS)
                .whereLessThanOrEqualTo("date", currentTime)
                .whereEqualTo(TRIP_STATUS, Trip.STATUS_DONE)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    ArrayList<Trip> pastTrips = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        pastTrips.add(getTrip(document));
                    }
                    tripDao.insertTrip(pastTrips);
                } else {
                    Log.w(TAG, "Error getting documents.", task.getException());
                }
            }
        });


    }

    public void getRemoteCanceledTrips() {
        firebase.getUserDocument(user.getId()).collection(TRIPS)
                .whereEqualTo(TRIP_STATUS, Trip.STATUS_CANCELED)
                .get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ArrayList<Trip> pastTrips = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    pastTrips.add(getTrip(document));
                }
                tripDao.insertTrip(pastTrips);
            } else {
                Log.w(TAG, "Error getting documents.", task.getException());
            }
        });


    }


    public void insertUser(User user) {
        Map<String, String> userData = new HashMap<>();
        userData.put(NAME, user.getName());
        userData.put("imageUrl", user.getProfileUrl());
        firebase.getUserDocument(user.getId()).set(userData);
    }

    public LiveData<List<Note>> getTodoNotes(final int tripId) {
        final MutableLiveData<List<Note>> notes = new MutableLiveData<>();

        firebase.getNotesCollection(user.getId(), String.valueOf(tripId))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<Note> documentNotes = new ArrayList<>();
                    for (DocumentSnapshot documentReference : queryDocumentSnapshots.getDocuments()) {
                        documentNotes.add(new Note(Integer.parseInt(documentReference.getId()),
                                documentReference.getString(NAME),
                                tripId,
                                documentReference.getBoolean("checked")));
                    }
                    notes.postValue(documentNotes);
                    tripDao.insertNote(documentNotes);
                });
        return notes;
    }

    public LiveData<Boolean> insertTrip(final Trip trip, ArrayList<Note> notes) {
        MutableLiveData<Boolean> inserted = new MutableLiveData<>();
        Room.databaseWriteExecutor.execute(() -> {
            long id = tripDao.insertTrip(trip);

            if (id != -1) {
                // insert notes if it has
                if (!notes.isEmpty())
                    for (Note note : notes) {
                        note.setTripId(id);
                    }
                long[] ids = tripDao.insertNote(notes);

                // update the server
                if (!trip.isOnline()) {
                    firebase.getTripDocument(user.getId(), String.valueOf(id))
                            .set(trip).addOnSuccessListener(aVoid ->
                    {
                        for (int i = 0; i < notes.size(); i++) {
                            notes.get(i).setId(ids[i]);
                            firebase.getNotesCollection(user.getId(), String.valueOf(id)).document(String.valueOf(ids[i])).set(notes.get(i));
                        }
                        inserted.postValue(true);
                    });
                }
            }
        });
        return inserted;
    }

    private Trip getTrip(QueryDocumentSnapshot document) {
        Map<String, Object> startPlace = (Map<String, Object>) document.get(START_POINT);
        Map<String, Object> endPlace = (Map<String, Object>) document.get(END_POINT);
        return new Trip(
                Integer.parseInt(document.getId()),
                document.getString(USER_ID),
                document.getString(NAME),
                new Place(String.valueOf(startPlace.get(NAME)), (double) startPlace.get("lat"), (double) startPlace.get("long")),
                new Place(String.valueOf(endPlace.get(NAME)), (double) endPlace.get("lat"), (double) endPlace.get("long")),
                document.getBoolean(TRIP_TYPE),
                document.getLong(TRIP_STATUS).intValue(),
                document.getDate("date"),
                document.getBoolean(ONLINE));

    }

    public void setCurrentUser(User user) {
        Repository.user = user;
    }

    public void updateTrip(Trip trip, Map<String, Object> tripData) {
        Room.databaseWriteExecutor.execute(() -> {
            int id = tripDao.updateTrip(trip);
            if (id != -1) {
                firebase.getTripDocument(user.getId(), String.valueOf(trip.getId()))
                        .update(tripData)
                        .addOnSuccessListener(aVoid -> Log.i(TAG, "onSuccess: update trip"));
            }
        });

    }


    public void updateNote(Note note, Map<String, Object> noteData) {
        Room.databaseWriteExecutor.execute(() -> {
            int id = tripDao.updateNote(note);
            if (id != -1) {
                firebase.getNotesCollection(user.getId(), String.valueOf(note.getTripId()))
                        .document(String.valueOf(note.getId()))
                        .update(noteData)
                        .addOnSuccessListener(aVoid -> Log.i(TAG, "onSuccess: update note"));
            }
        });

    }

    public void deleteTrip(int tripId) {
        tripDao.deleteTrip(tripId);
        tripDao.deleteTripNote(tripId);

    }

    public void deleteNote(Note note) {
        tripDao.deleteTripNote(note);
    }


}
