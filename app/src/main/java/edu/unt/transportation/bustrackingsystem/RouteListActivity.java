package edu.unt.transportation.bustrackingsystem;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.firebase.client.Firebase;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import edu.unt.transportation.bustrackingsystem.model.BusRoute;
import edu.unt.transportation.bustrackingsystem.model.BusStop;
import edu.unt.transportation.bustrackingsystem.model.Vehicle;

/**
 * Created by gdawg on 10/15/2016.
 */
public class RouteListActivity extends AppCompatActivity implements AdapterView
        .OnItemSelectedListener, AdapterView.OnItemClickListener, Serializable,
        ValueEventListener, ChildEventListener
{
    private static final String ROOT_ROUTE = "routes";
    private ListView routeList;
    private RouteAdapter routeAdapter;
    private FirebaseController firebaseController;
    private HashMap<String, BusRoute> busRoute;
    private BusRoute selectedRoute;
    private DatabaseReference mDatabase;
    private DatabaseReference routeRoot;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);

        Bundle extras = getIntent().getExtras();
        if (extras.containsKey(FirebaseController.KEY_FIREBASE_CONTROLLER))
        {
            firebaseController = (FirebaseController) extras.getSerializable(FirebaseController
                    .KEY_FIREBASE_CONTROLLER);
        }
        else
        {
            firebaseController = new FirebaseController(RouteListActivity.this);
        }
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.addChildEventListener(this);
        routeRoot = mDatabase.child(ROOT_ROUTE);
        routeRoot.addValueEventListener(this);
        setContentView(R.layout.activity_routes);
        routeList = (ListView) findViewById(R.id.list_routes);
        routeAdapter = new RouteAdapter(RouteListActivity.this, R.layout.row_template_routes);
        routeList.setAdapter(routeAdapter);
        routeList.setOnItemSelectedListener(this);
        routeList.setOnItemClickListener(this);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        routeRoot.removeEventListener((ChildEventListener) this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        onItemSelected(parent, view, position, id);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        BusRoute route = routeAdapter.getItem(position);
        if (route == null) return;
        setSelectedRoute(route);
        navigateToMap();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {

    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot)
    {
        try
        {
            setUpRoutes(dataSnapshot.child(ROOT_ROUTE));
        } catch (Exception e)
        {
            Log.e("onDataChange", "Error loading routes", e);
        }
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s)
    {
        if (dataSnapshot.getKey().equals(ROOT_ROUTE))
        {
            setUpRoutes(dataSnapshot);
        }
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s)
    {
        if (dataSnapshot.getKey().equals(ROOT_ROUTE))
        {
            setUpRoutes(dataSnapshot);
        }
    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot)
    {

    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s)
    {

    }

    @Override
    public void onCancelled(DatabaseError databaseError)
    {

    }

    private HashMap<String, BusRoute> getBusRoutes()
    {
        if (busRoute == null)
        {
            busRoute = new HashMap<>();
        }
        return busRoute;
    }

    public ArrayList<BusStop> getBusStops()
    {
        return new ArrayList<>();
    }

    public BusRoute getSelectedRoute()
    {
        return selectedRoute;
    }

    public void setSelectedRoute(BusRoute selectedRoute)
    {
        this.selectedRoute = selectedRoute;
    }

    public ArrayList<Vehicle> getVehicles()
    {
        return new ArrayList<>();
    }

    private void navigateToMap()
    {
        Bundle b = new Bundle();
        b.putSerializable(TrackerMapActivity.KEY_ROUTE_ID, getSelectedRoute().getRouteId());
        ActivityUtil.showScreen(RouteListActivity.this, TrackerMapActivity.class, b);
    }

    private void setUpRoutes(DataSnapshot dataSnapshot)
    {
        for (DataSnapshot snapshot : dataSnapshot.getChildren())
        {
            BusRoute b = snapshot.getValue(BusRoute.class);
            getBusRoutes().put(b.getRouteId(), b);
            if (b.getBusStopMap() != null)
            {
                for (String id : b.getBusStopMap().keySet())
                {
                    BusStopListener busStopListener = new BusStopListener(b.getRouteId());
                    busStopListener.registerListener(id);
                }
            }/*
            if (b.getVehicleMap() != null)
            {
                for (String id : b.getVehicleMap().keySet())
                {
                    Vehicle value = dataSnapshot.child(FirebaseController.FIREBASE_VEHICLES)
                            .child(id).getValue(Vehicle
                                    .class);
                    if (value != null)
                    {
                        b.putVehicle(id, value);
                    }
                }
            }*/
        }
        routeAdapter.clear();
        routeAdapter.addAll(getBusRoutes().values());
        routeAdapter.notifyDataSetChanged();
        if (BusTrackingSystem.isMapActivityVisible())
        {
            navigateToMap();
        }
    }

    private class BusStopListener implements ValueEventListener
    {

        private final String routeID;
        DatabaseReference busStopReference;

        public BusStopListener(String routeId)
        {
            this.routeID = routeId;
        }

        public void registerListener(String stopID)
        {
            busStopReference = mDatabase.child("stops");
            busStopReference.child(stopID).addListenerForSingleValueEvent(this);
        }

        @Override
        public void onDataChange(DataSnapshot dataSnapshot)
        {
            BusStop busStopSnapShot = dataSnapshot.getValue(BusStop.class);
            if (getBusRoutes().containsKey(routeID))
            {
                getBusRoutes().get(routeID).getBusStopObjectMap().put(busStopSnapShot.getStopID()
                        , busStopSnapShot);
                routeAdapter.notifyDataSetChanged();

            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError)
        {

        }
    }


}
