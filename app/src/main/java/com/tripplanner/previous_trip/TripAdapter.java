package com.tripplanner.previous_trip;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.tripplanner.Constants;
import com.tripplanner.R;
import com.tripplanner.data_layer.local_data.entity.Trip;
import com.tripplanner.databinding.PreviousTripContentBinding;
import com.tripplanner.previous_trip_details.GsonUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripAdapter  extends RecyclerView.Adapter<TripAdapter.PreviousTripViewHandler> {
    private List<Trip> tripList;

    public TripAdapter(List<Trip> tripList) {
        this.tripList = tripList;

    }

    @NonNull
    @Override
    public PreviousTripViewHandler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.previous_trip_content, parent, false);
        return new PreviousTripViewHandler(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PreviousTripViewHandler holder, int position) {
        Trip trip = tripList.get(position);
        holder.tripName.setText(trip.getName());
        holder.itemView.setOnClickListener(view -> {
            Bundle bundle = new Bundle();
            String personJsonString = GsonUtils.getGsonParser().toJson(trip);
            bundle.putString(Constants.KEY_TRIP, personJsonString);
            Navigation.findNavController(view).navigate(R.id.action_previousFragment_to_tripDetailFragment,bundle);
        });
        holder.tripDate.setText("Date: "+getDate(trip.getTripDate()));
        holder.tripTime.setText("Time: "+getTime(trip.getTripDate()));
        holder.tripTo.setText(trip.getStartPoint().getName());
        holder.tripFrom.setText(trip.getEndPoint().getName());
        if (trip.getTripStatus()== Constants.STATUS_DONE)
        {
            holder.tripTaskProgress.setTextColor(Color.parseColor("#ED77F9"));
        }
        else
        {
            holder.tripTaskProgress.setTextColor(Color.parseColor("#E68516"));
            holder.tripTaskProgress.setText("Canceled Trip");

        }
    }
    public String getDate(Date date)
    {
        String myFormat = "dd/MM/yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        return sdf.format(date).toString();
    }
    public String getTime(Date date)
    {
        String time = new SimpleDateFormat("hh:mm", Locale.getDefault()).format(date);
        return  time;
    }
    @Override
    public int getItemCount() {
        return tripList.size();
    }

    public void setArray(List<Trip> trips) {
        this.tripList=trips;
        notifyDataSetChanged();
    }

    public class PreviousTripViewHandler extends RecyclerView.ViewHolder {
        PreviousTripContentBinding binding;
        public TextView tripName ;
        public TextView tripDate;
        public TextView tripTime;
        public TextView tripTaskProgress;
        public TextView tripFrom;
        public TextView tripTo;
        public RelativeLayout viewBackground;
        public CardView viewForeground;
        public PreviousTripViewHandler(@NonNull View itemView) {
            super(itemView);
            tripName = itemView.findViewById(R.id.tripNameTextView);
            tripDate = itemView.findViewById(R.id.tripDateTextView);
            tripTime =itemView.findViewById(R.id.tripTimeTextView);
            tripTaskProgress = itemView.findViewById(R.id.taskProgressTextView);
            tripFrom = itemView.findViewById(R.id.fromTextView);
            tripTo = itemView.findViewById(R.id.toTextView);
            viewBackground=itemView.findViewById(R.id.view_background);
            viewForeground=itemView.findViewById(R.id.view_foreground);
        }

    }
    public void removeItem(int position) {
        tripList.remove(position);
        notifyItemRemoved(position);
    }

    public void restoreItem(Trip item, int position) {
        tripList.add(position, item);
        notifyItemInserted(position);
    }
}
