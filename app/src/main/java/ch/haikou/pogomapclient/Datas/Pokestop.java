package ch.haikou.pogomapclient.datas;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;


/**
 * Created by Zoruk on 09.08.2016.
 */

public class Pokestop implements MarkerObject {
    private String pokestopId;
    private boolean enabled;
    private double latitude;
    private double longitude;
    private long lureExpire;
    private long lastModified;

    public Pokestop(JSONObject pokestop) throws JSONException {
        pokestopId = pokestop.getString("pokestop_id");
        enabled = pokestop.getBoolean("enabled");
        latitude = pokestop.getDouble("latitude");
        longitude = pokestop.getDouble("longitude");
        lureExpire = pokestop.optLong("lure_expiration", -1);
        lastModified = pokestop.getLong("last_modified");
    }

    public void update(Pokestop p) {
        enabled = p.enabled;
        latitude = p.latitude;
        longitude = p.longitude;
        lureExpire = p.lureExpire;
        lastModified = p.lastModified;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public boolean hasTimeout() {
        return false;
    }

    @Override
    public long getRemainingTime() {
        return 0;
    }

    @Override
    public long nextUpdate() {
        return 10000;
    }

    @Override
    public String getMarkerUid() {
        return getPokestopId();
    }

    @Override
    public String getMarkerImgURL() {
        return getImgURL();
    }

    @Override
    public String getTitle() {
        return "Pokéstop";
    }

    public LatLng getLatLng() {
        return new LatLng(
                getLatitude(),
                getLongitude()
        );
    }

    @Override
    public String formatRemainingTime() {
        return formatRemainingTime(System.currentTimeMillis());
    }

    public boolean isLured(long time) {
        long millis = lureExpire - time;
        if (lureExpire == -1) return false;
        return millis > 0;
    }
    public boolean isLured() {
        return isLured(System.currentTimeMillis());
    }

    public long getLureExpire() {
        return lureExpire;
    }

    public String getPokestopId() {
        return pokestopId;
    }


    public String getImgURL() {
        if (isLured()) {
            //return "http://i.imgur.com/GNKbKhC.png";
            return "http://i.imgur.com/pmLrx3R.png";
        }
        //return "http://i.imgur.com/mi6NQWk.png";
        return "http://i.imgur.com/2BI3Cqv.png";
    }

    public String formatRemainingTime(long cur) {
        if (!isLured(cur))
            return "Expired";
        long millis = lureExpire - cur;

        return String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    public long getLastModified() {
        return lastModified;
    }
}
