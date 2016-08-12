package ch.haikou.pogomapclient.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ch.haikou.pogomapclient.datas.Pokemon;
import ch.haikou.pogomapclient.datas.Pokestop;
import ch.haikou.pogomapclient.R;
import ch.haikou.pogomapclient.utils.MapHelper;
import ch.haikou.pogomapclient.utils.PogoHttpHelper;
import ch.haikou.pogomapclient.utils.PokemonAppPreferences;
import ch.haikou.pogomapclient.utils.RemoteImageLoader;
import okhttp3.HttpUrl;

public class PokemonDataService extends Service {

    public static final String ACTION_STRING_DATAUPDATED = "PokemonDataServiceUpdate";

    public static final int NOTIFICATION_ID = 1337;

    private static final String TAG = PokemonDataService.class.getName();

    private static boolean notifierRunning = false;
    private final IBinder mBinder = new PokemonDataServiceBinder();
    private boolean mRunning = true;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private Map<String, Pokemon> mPokemons;
    private Map<String, Pokestop> mPokestops;
    private Set<String> sentNotification = new HashSet<>();
    private long refreshRate;
    private LatLngBounds bounds;
    private LatLngBounds notifierBounds;
    private boolean withPokestops = false;

    private Location mLocation;
    private LocationManager mLocationManager;

    private PokemonAppPreferences mPref;
    private NotificationManager mNotificationManager;

    public static boolean isIsNotifierRunning() {
        return notifierRunning;
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "service created", Toast.LENGTH_SHORT).show();

        mPref = new PokemonAppPreferences(this);
        mPokemons = new HashMap<>();
        mPokestops = new HashMap<>();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mHandlerThread = new HandlerThread("PokemonDataService");
        mHandlerThread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper());

        refreshRate = 5000;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateOnce(bounds);
                if (mRunning)
                    mHandler.postDelayed(this, refreshRate);
            }
        });

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mLocationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String provider = mLocationManager.getBestProvider(criteria, true);
        mLocation = mLocationManager.getLastKnownLocation(provider);

        updateServiceBound();

        mLocationManager.requestLocationUpdates(provider, 1000, 1f, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mLocation = location;
                updateServiceBound();
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        });
    }

    private void updateServiceBound() {
        if (mLocation == null)
            return;
        LatLng latLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());

        int notificationRadius = mPref.getServiceRadius() * 1000;
        notifierBounds = new LatLngBounds.Builder()
                .include(
                        MapHelper.translatePoint(latLng, notificationRadius, 0)
                )
                .include(
                        MapHelper.translatePoint(latLng, notificationRadius, 90)
                )
                .include(
                        MapHelper.translatePoint(latLng, notificationRadius, 180)
                )
                .include(
                        MapHelper.translatePoint(latLng, notificationRadius, 270)
                )
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateOnce(notifierBounds);
                if (mPref.isServiceEnabled())
                    mHandler.postDelayed(this, mPref.getServerRefreshDelay());
            }
        });
        notifierRunning = true;

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void updateOnce(LatLngBounds bounds) {
        Log.d(TAG, "Starting new request");

        String host = mPref.getServer();
        boolean useSSL = mPref.useSSL();

        if (host == null) {
            Toast.makeText(this, getString(R.string.no_server_set), Toast.LENGTH_LONG).show();
            return;
        }

        if (bounds != null) {

            HttpUrl.Builder urlbuilder = new HttpUrl.Builder();

            if (useSSL)
                urlbuilder.scheme("https");
            else
                urlbuilder.scheme("http");
            if(withPokestops)
                urlbuilder.addQueryParameter("pokestops", "true");
            else
                urlbuilder.addQueryParameter("pokestops", "false");
            HttpUrl url = urlbuilder
                    .host(host)
                    .addPathSegment("raw_data")
                    .addQueryParameter("pokemon", "true")
                    .addQueryParameter("gyms", "false")
                    .addQueryParameter("scanned", "false")
                    .addQueryParameter("swLat", Double.toString(bounds.southwest.latitude))
                    .addQueryParameter("swLng", Double.toString(bounds.southwest.longitude))
                    .addQueryParameter("neLat", Double.toString(bounds.northeast.latitude))
                    .addQueryParameter("neLng", Double.toString(bounds.northeast.longitude))
                    .build();


            Log.d(TAG, url.toString());
            String result = PogoHttpHelper.request(url, mPref.getUsername(), mPref.getPassword());

            try {
                if (result == null) {
                    Log.d(TAG, "Request failed");
                } else {
                    Log.d(TAG, "Success");
                    JSONObject json = new JSONObject(result);
                    //Log.d(TAG, json.toString());
                    JSONArray pokemons = json.getJSONArray("pokemons");
                    for (int i = 0; i < pokemons.length(); ++i) {
                        JSONObject pokemon = pokemons.getJSONObject(i);

                        final Pokemon p = new Pokemon(pokemon);

                        synchronized (mPokemons) {
                            if (!mPokemons.containsKey(p.getEncounterId())) {
                                mPokemons.put(p.getEncounterId(), p);
                            }
                        }
                    }

                    if (withPokestops) {
                        JSONArray pokestops = json.getJSONArray("pokestops");
                        for (int i = 0; i < pokestops.length(); ++i) {
                            JSONObject pokestop = pokestops.getJSONObject(i);

                            Pokestop p = new Pokestop(pokestop);

                            synchronized (mPokestops) {
                                if (mPokestops.containsKey(p.getPokestopId())) {
                                    mPokestops.get(p.getPokestopId()).update(p);
                                } else {
                                    mPokestops.put(p.getPokestopId(), p);
                                }
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        long time = System.currentTimeMillis();
        synchronized (mPokemons) {
            Iterator<Pokemon> it = mPokemons.values().iterator();

            while (it.hasNext()) {
                Pokemon p = it.next();
                if (time >= p.getDisappearTime()) {
                    it.remove();
                    if (sentNotification.contains(p.getEncounterId())) {
                        sentNotification.remove(p.getEncounterId());
                        mNotificationManager.cancel(p.getEncounterId(), NOTIFICATION_ID);
                    }
                }
            }
        }

        if (mPref.isServiceEnabled()) {
            generateNotifications();
        }

        Intent i = new Intent();
        i.setAction(ACTION_STRING_DATAUPDATED);
        sendBroadcast(i);
    }

    private void generateNotifications() {
        Set<Integer> notifyPokemons = mPref.getNotifyPokemonIDs();
        int serviceRadius = mPref.getServiceRadius();
        synchronized (mPokemons) {
            Iterator<Pokemon> it = mPokemons.values().iterator();

            while (it.hasNext()) {
                Pokemon p = it.next();
                double distance = MapHelper.distance(
                        p.getLatLng(),
                        new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));

                if (notifyPokemons.contains(p.getId()) && distance <= serviceRadius) {

                    Intent intent = p.generateIntent(this);

                    PendingIntent pendingIntent =
                            PendingIntent.getActivity(this, p.getEncounterId().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    Bitmap bitmap = RemoteImageLoader.loadSyncIcon(getApplicationContext(), p.getImgURL());

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(PokemonDataService.this);

                    if (bitmap != null)
                        builder.setLargeIcon(bitmap);

                    Notification notification = builder
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.ic_my_location_white_24dp)
                            .setWhen(p.getDisappearTime())
                            .setContentTitle(p.getName())
                            .setContentIntent(pendingIntent)
                            .setContentText(String.valueOf((long)distance))

                            .build();


                    mNotificationManager.notify(p.getEncounterId(), NOTIFICATION_ID, notification);
                    sentNotification.add(p.getEncounterId());
                }
            }
        }
    }

    public void requestUpdate(final LatLngBounds bounds) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateOnce(bounds);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
        mRunning = false;
        mHandlerThread.quitSafely();
        notifierRunning = false;
    }

    public void setRefreshRate(long refreshRate) {
        this.refreshRate = refreshRate;
    }

    public Map<String, Pokemon> getPokemons() {
        return mPokemons;
    }

    public Map<String, Pokestop> getPokestops() {
        return mPokestops;
    }

    public void setBounds(LatLngBounds bounds) {
        Log.d(TAG, "setBounds" + bounds);
        this.bounds = bounds;
        requestUpdate(bounds);
    }

    public boolean isWithPokestops() {
        return withPokestops;
    }

    public void setWithPokestops(boolean withPokestops) {
        this.withPokestops = withPokestops;
    }

    public Location getLocation() {
        return mLocation;
    }

    public class PokemonDataServiceBinder extends Binder {
        public PokemonDataService getService() {
            return PokemonDataService.this;
        }
    }
}
