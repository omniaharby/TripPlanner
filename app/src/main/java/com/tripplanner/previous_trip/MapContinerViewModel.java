package com.tripplanner.previous_trip;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.tripplanner.data_layer.Repository;
import com.tripplanner.data_layer.local_data.entity.Trip;

import java.util.List;

public class MapContinerViewModel extends AndroidViewModel {
    // TODO: Implement the ViewModel
    Repository repository;

    public MapContinerViewModel(@NonNull Application application) {
        super(application);
        repository = new Repository(application);
    }
    public LiveData<List<Trip>> getDoneTrip() {
        return repository.getDoneTrips();
    }

}
