package ch.haikou.pogomapclient;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import ch.haikou.pogomapclient.datas.MarkerObject;
import ch.haikou.pogomapclient.datas.Pokemon;
import ch.haikou.pogomapclient.datas.Pokestop;
import ch.haikou.pogomapclient.service.PokemonDataService;
import ch.haikou.pogomapclient.utils.MapHelper;
import ch.haikou.pogomapclient.utils.PokemonAppPreferences;
import ch.haikou.pogomapclient.utils.RemoteImageLoader;

public class PogoMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    static final public String TAG = "PogoMapActivity";

    public final static String INTENT_LATITUDE = "latitude";
    public final static String INTENT_LONGITUDE = "longitude";
    public final static String INTENT_SEL_POKEMON = "pokemon";

    static final private int PERMISSION_REQUEST_LOCATION_FINE = 123;
    private Set<Integer> mShowablePokemonIDs;
    private Set<Integer> mNotifyPokemonIDs;

    private GoogleMap mMap;
    private PokemonDataService mService;
    private Handler mHandler;
    private PokemonAppPreferences.PokestopDisplay mPokestopDisplay;
    private PokemonAppPreferences mPref;
    private Timer mTimer = null;
    private Set<Marker> mMarkers;
    private Set<String> mMarkersIds;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            PokemonDataService.PokemonDataServiceBinder binder = (PokemonDataService.PokemonDataServiceBinder) iBinder;
            mService = binder.getService();

            long refreshRate = mPref.getRefreshDelay();

            mService.setRefreshRate(refreshRate);
            mService.setWithPokestops(
                    mPokestopDisplay == PokemonAppPreferences.PokestopDisplay.ALL ||
                    mPokestopDisplay == PokemonAppPreferences.PokestopDisplay.LURED_ONLY
            );

            Intent i = PogoMapActivity.this.getIntent();
            if (i == null || !(i.hasExtra(INTENT_LATITUDE) && i.hasExtra(INTENT_LONGITUDE)))
                setCameraLocation(mService.getLocation());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
        }
    };
    private Marker mActiveMarker;

    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mMarkers = new HashSet<>();
        mMarkersIds = new HashSet<>();

        mPokestopDisplay = PokemonAppPreferences.PokestopDisplay.NONE;
        mPref = new PokemonAppPreferences(this);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updatePokemons();
                updatePokestops();
            }
        };
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setIcon(R.drawable.ic_map);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onStart() {
        super.onStart();
        mShowablePokemonIDs = mPref.getShowablePokemonIDs();
        mNotifyPokemonIDs = mPref.getNotifyPokemonIDs();

        mPokestopDisplay = mPref.getPokestopDisplay();

        mHandler = new Handler();
        //startService(new Intent(getBaseContext(), PokemonDataService.class));
        Intent i = new Intent(this, PokemonDataService.class);
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);

        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateActiveMarker();
                    }
                });
            }
        };

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(tt, 0, 1000);

        boolean isServiceRunning = PokemonDataService.isIsNotifierRunning();
        Intent service = new Intent(this, PokemonDataService.class);
        if (mPref.isServiceEnabled()) {
            if (!isServiceRunning) {
                startService(service);
            }
        } else {
            if (isServiceRunning) {
                stopService(service);
            }
        }

        if (mReceiver != null) {
            IntentFilter filter = new IntentFilter(PokemonDataService.ACTION_STRING_DATAUPDATED);
            registerReceiver(mReceiver, filter);
        }

        List<Marker> markerToRemove = new LinkedList<>();
        for (final Marker m : mMarkers) {
            MarkerObject tag = (MarkerObject) m.getTag();
            long nextUpdate = tag.nextUpdate();
            if (tag.hasTimeout()) {
                long remainingTime = tag.getRemainingTime();
                if (remainingTime < 1000) {
                    markerToRemove.add(m);
                } else {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            removeMarker(m);
                        }
                    }, remainingTime);
                }
            } else if (nextUpdate > 0) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateMarker(m);
                    }
                }, nextUpdate);
            }
        }

        for (Marker m : markerToRemove) {
            removeMarker(m);
        }

        checkMarkerForRemove();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);

        mTimer.cancel();
        mTimer.purge();

        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }

        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
        for (String key : intent.getExtras().keySet()) {
            Log.d(TAG, key + " = " + intent.getExtras().get(key));
        }
        moveFromIntent(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_LOCATION_FINE: {
                if (grantResults.length > 0) {

                    if (mMap != null) {
                        mMap.setMyLocationEnabled(true);
                        if (mService != null) {
                            Location loc = mService.getLocation();
                            setCameraLocation(loc);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        mMap.moveCamera(CameraUpdateFactory.zoomTo(16f));

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                Log.d(TAG, "New Zoom" + cameraPosition.zoom);
                if (cameraPosition.zoom < 12f) return;
                fetchData(mMap.getProjection().getVisibleRegion().latLngBounds);
            }
        });

        mMap.setInfoWindowAdapter(new MarkerInfoWindowAdapter());
        mMap.setOnInfoWindowClickListener(new InfoWindowClickListener());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION_FINE);
            return;
        }

        mMap.setMyLocationEnabled(true);

        Intent i = getIntent();
        if (!moveFromIntent(i) && mService != null)
            setCameraLocation(mService.getLocation());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings: {
                Toast.makeText(this, "Settings !", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void setCameraLocation(Location loc) {
        if (mMap != null && loc != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(loc.getLatitude(), loc.getLongitude())));
        }
    }

    private void updateActiveMarker() {

        if (mActiveMarker == null) {
            return;
        }

        updateMarker(mActiveMarker);
        /*long time = System.currentTimeMillis();

        Iterator<PokemonMarker> it = mMarkerPokemon.values().iterator();

        while (it.hasNext()) {
            PokemonMarker pm = it.next();
            long disappearTime = pm.pokemon.getDisappearTime();
            if (disappearTime <= time || !mShowablePokemonIDs.contains(pm.pokemon.getId())) {
                //pm.marker.remove();
                it.remove();
            } else {
                pm.marker.setSnippet(
                        pm.pokemon.formatRemainingTime(time)
                );
                if (pm.marker.isInfoWindowShown()) {
                    pm.marker.hideInfoWindow();
                    pm.marker.showInfoWindow();
                }
            }
        }

        final int markerSize = getResources().getDimensionPixelSize(R.dimen.pokemon_marker);

        Iterator<PokestopMarker> stopIt = mMarkerPokestop.values().iterator();

        while (stopIt.hasNext()) {
            final PokestopMarker pm = stopIt.next();
            final boolean isLured = pm.pokestop.isLured(time);

            if (mPokestopDisplay == PokemonAppPreferences.PokestopDisplay.NONE) {
                pm.marker.remove();
                stopIt.remove();
                continue;
            }

            if (mPokestopDisplay == PokemonAppPreferences.PokestopDisplay.LURED_ONLY) {
                if (!isLured) {
                    pm.marker.remove();
                    stopIt.remove();
                    continue;
                }
            }
            if (isLured != pm.isLured) {
                RemoteImageLoader.loadMapIcon(getApplication(),
                        pm.pokestop.getImgURL()
                        , markerSize / 2, markerSize / 2, new RemoteImageLoader.Callback() {

                            @Override
                            public void onFetch(Bitmap bitmap) {
                                pm.marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
                            }
                        });
                pm.isLured = isLured;
            }

            if (isLured) {
                pm.marker.setSnippet(pm.pokestop.formatRemainingTime(time));
            } else {
                pm.marker.setSnippet("");
            }
            if (pm.marker.isInfoWindowShown()) {
                pm.marker.hideInfoWindow();
                pm.marker.showInfoWindow();
            }
        }*/
    }

    public void fetchData(LatLngBounds bounds) {
        if (mService != null) {
            mService.setBounds(bounds);
        }
    }

    private void updatePokemons() {
        if (mMap == null)
            return;
        if (mService == null)
            return;

        final int markerSize = getResources().getDimensionPixelSize(R.dimen.pokemon_marker);

        Map<String, Pokemon> pokemonMap = mService.getPokemons();

        final LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;

        synchronized (pokemonMap) {
            for (Map.Entry<String, Pokemon> entry : pokemonMap.entrySet()) {
                final Pokemon pokemon = entry.getValue();
                final LatLng latLen = pokemon.getLatLng();
                if (!mShowablePokemonIDs.contains(pokemon.getId()))
                    continue;
                if (!bounds.contains(latLen)) {
                    continue;
                }
                addMarker(pokemon);
            }
        }
    }

    private void updatePokestops() {
        if (mMap == null)
            return;
        if (mService == null)
            return;

        final int markerSize = getResources().getDimensionPixelSize(R.dimen.pokemon_marker);
        if (mPokestopDisplay == PokemonAppPreferences.PokestopDisplay.LURED_ONLY ||
                mPokestopDisplay == PokemonAppPreferences.PokestopDisplay.ALL) {
            Map<String, Pokestop> pokestopMap = mService.getPokestops();
            final LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
            synchronized (pokestopMap) {
                for (Map.Entry<String, Pokestop> entry : pokestopMap.entrySet()) {
                    final String pokestopId = entry.getKey();
                    final Pokestop pokestop = entry.getValue();
                    final LatLng latLen = pokestop.getLatLng();

                    switch (mPokestopDisplay) {
                        case LURED_ONLY:
                            if (!pokestop.isLured()) continue;
                        default: break;
                    }

                    if (!bounds.contains(latLen)) {
                        continue;
                    }

                    addMarker(pokestop);
                }
            }
        }
    }

    private void addMarker(final MarkerObject markerObject) {
        if (mMarkersIds.contains(markerObject.getMarkerUid()))
            return;
        final int markerSize = getResources().getDimensionPixelSize(R.dimen.pokemon_marker);
        RemoteImageLoader.loadMapIcon(getApplication(),
                markerObject.getMarkerImgURL()
                , markerSize, markerSize, new RemoteImageLoader.Callback() {

                    @Override
                    public void onFetch(Bitmap bitmap) {

                        long remaining = markerObject.getRemainingTime();
                        if(bitmap == null)
                            return;

                        if(markerObject.hasTimeout() && remaining < 3000) {
                            return;
                        }

                        final Marker m = mMap.addMarker(new MarkerOptions().position(
                                markerObject.getLatLng())
                                .title(markerObject.getTitle())
                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                        );

                        m.setTag(markerObject);

                        mMarkers.add(m);
                        mMarkersIds.add(markerObject.getMarkerUid());

                        if (markerObject.hasTimeout()) {
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    removeMarker(m);
                                }
                            }, remaining);
                        }

                        long nextUpdate = markerObject.nextUpdate();

                        if (nextUpdate > 0) {
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    updateMarker(m, true);
                                }
                            }, nextUpdate);
                        }
                    }
                });
    }

    private void updateMarker(final Marker marker) {
        updateMarker(marker, false);
    }

    private void updateMarker(final Marker marker, boolean recursive) {
        //Log.d(TAG, "updateMarker");
        MarkerObject markerObject = (MarkerObject) marker.getTag();

        if (markerObject instanceof Pokemon) {
            Pokemon p = (Pokemon) markerObject;

            if(marker.isInfoWindowShown()) {
                marker.setSnippet(p.formatRemainingTime());
                marker.showInfoWindow();
            }
        }

        if (markerObject instanceof Pokestop) {
            Pokestop p = (Pokestop) markerObject;

            if (mPokestopDisplay == PokemonAppPreferences.PokestopDisplay.NONE) {
                removeMarker(marker);
                return;
            }

            if (!p.isLured() && mPokestopDisplay == PokemonAppPreferences.PokestopDisplay.LURED_ONLY) {
                removeMarker(marker);
                return;
            }

            int size = getResources().getDimensionPixelSize(R.dimen.pokemon_marker);

            RemoteImageLoader.loadMapIcon(this, p.getMarkerImgURL(), size, size, new RemoteImageLoader.Callback() {
                @Override
                public void onFetch(Bitmap bitmap) {
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
                }
            });

            if(marker.isInfoWindowShown()){
                if (p.isLured()) {
                    marker.setSnippet(p.formatRemainingTime());
                } else {
                    marker.setSnippet(null);
                }
                marker.showInfoWindow();
            }
        }

        if (recursive) {
            long nextUpdate = markerObject.nextUpdate();

            if (nextUpdate > 0) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateMarker(marker, true);
                    }
                }, nextUpdate);
            }
        }
    }

    private void removeMarker(Marker m) {
        if (mActiveMarker == m)
            mActiveMarker = null;
        m.remove();
        MarkerObject markerObject = (MarkerObject) m.getTag();
        mMarkersIds.remove(markerObject);
        m.setTag(null);
        mMarkers.remove(m);
    }

    public boolean moveFromIntent(Intent i) {
        if (i != null && i.hasExtra(INTENT_LATITUDE) && i.hasExtra(INTENT_LONGITUDE)) {
            Location targetLocation = new Location("");
            targetLocation.setLatitude(i.getDoubleExtra(INTENT_LATITUDE, 0));
            targetLocation.setLongitude(i.getDoubleExtra(INTENT_LONGITUDE, 0));
            setCameraLocation(targetLocation);
            return true;
        }
        return false;
    }
    class MarkerInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        View pokemonView;
        TextView pokemonNameTextView;
        TextView pokemonTimeoutTextView;
        TextView pokemonDistanceTextView;

        private MarkerInfoWindowAdapter() {
            pokemonView = getLayoutInflater().inflate(R.layout.pokemon_info_window, null);
            pokemonNameTextView = (TextView) pokemonView.findViewById(R.id.name);
            pokemonTimeoutTextView = (TextView) pokemonView.findViewById(R.id.timeout);
            pokemonDistanceTextView = (TextView) pokemonView.findViewById(R.id.distance);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            mActiveMarker = marker;

            MarkerObject markerObject = (MarkerObject) marker.getTag();
            if (markerObject instanceof Pokemon) {
                Pokemon p = (Pokemon) markerObject;

                pokemonNameTextView.setText(p.getName());
                pokemonTimeoutTextView.setText(p.formatRemainingTime());
                if (mService != null && mService.getLocation() != null) {
                    Location l = mService.getLocation();
                    Log.d(TAG, "LOC " + l);
                    double distance = MapHelper.distance(p.getLatLng(), new LatLng(l.getLatitude(), l.getLongitude()));
                    String dist = String.format(getString(R.string.info_window_distanmce), (int)distance);
                    pokemonDistanceTextView.setText(dist);
                } else {
                    pokemonDistanceTextView.setText("");
                }

                return pokemonView;
            }
            return null;
        }
    }

    private void checkMarkerForRemove() {
        List<Marker> markersToRemove = new LinkedList<>();

        for (Marker m : mMarkers) {
            MarkerObject markerObject = (MarkerObject) m.getTag();

            if (markerObject instanceof Pokemon) {
                if (!mShowablePokemonIDs.contains(((Pokemon) markerObject).getId())) {
                    markersToRemove.add(m);
                }
            }
        }

        for (Marker m: markersToRemove) {
            removeMarker(m);
        }
    }

    class InfoWindowClickListener implements GoogleMap.OnInfoWindowClickListener {

        @Override
        public void onInfoWindowClick(Marker marker) {
            Log.d(TAG, "onInfoWindowClick");

            MarkerObject markerObject = (MarkerObject) marker.getTag();

            if (markerObject instanceof Pokemon) {
                final Pokemon p = (Pokemon) markerObject;
                AlertDialog.Builder builder = new AlertDialog.Builder(PogoMapActivity.this);

                builder.setTitle(p.getName());
                builder.setCancelable(true);

                boolean values[] = { mShowablePokemonIDs.contains(p.getId()),
                        mNotifyPokemonIDs.contains(p.getId())
                };
                builder.setMultiChoiceItems(R.array.dialog_pokemon_items, values, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                        switch (i) {
                            case 0:
                                if (b) {
                                    mShowablePokemonIDs.add(p.getId());
                                } else {
                                    mShowablePokemonIDs.remove(p.getId());
                                    checkMarkerForRemove();
                                }
                                mPref.setShowablePokemonIDs(mShowablePokemonIDs);
                                break;
                            case 1:
                                if (b) {
                                    mNotifyPokemonIDs.add(p.getId());
                                } else {
                                    mNotifyPokemonIDs.remove(p.getId());
                                }
                                mPref.setNotifyPokemonIDs(mNotifyPokemonIDs);
                                break;
                        }
                    }
                });
                builder.setPositiveButton("Ok", null);
                builder.create().show();

                Log.d(TAG, "End onInfoWindowClick");
            }
        }
    }
}
