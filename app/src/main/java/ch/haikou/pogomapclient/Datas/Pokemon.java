package ch.haikou.pogomapclient.datas;

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import ch.haikou.pogomapclient.PogoMapActivity;

/**
 * Created by Zoruk on 08.08.2016.
 */

public class Pokemon implements MarkerObject {
    private long disappearTime;
    private double latitude;
    private double longitude;
    private int id;
    private String name;
    private String rarity;
    private String encounterId;

    public Pokemon(JSONObject pokemon) throws JSONException {
        disappearTime = pokemon.getLong("disappear_time");
        encounterId = pokemon.getString("encounter_id");
        latitude = pokemon.getDouble("latitude");
        longitude = pokemon.getDouble("longitude");
        id = pokemon.getInt("pokemon_id");
        name = pokemon.getString("pokemon_name");
    }

    public Pokemon() {
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public long getDisappearTime() {
        return disappearTime;
    }

    public void setDisappearTime(long disappearTime) {
        this.disappearTime = disappearTime;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public LatLng getLatLng() {
        return new LatLng(getLatitude(), getLongitude());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public String getImgURL() {
        return getImgURL(getId());
    }

    public static String getImgURL(int id) {
        return "http://serebii.net/pokemongo/pokemon/" + String.format("%03d", id) + ".png";
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Date netDate = (new Date(disappearTime));
        return name + " " + id + " " +  sdf.format(netDate);
    }

    @Override
    public boolean hasTimeout() {
        return true;
    }

    public long getRemainingTime() {
        return getRemainingTime(System.currentTimeMillis());
    }

    @Override
    public long nextUpdate() {
        return 0;
    }

    @Override
    public String getMarkerUid() {
        return getEncounterId();
    }

    @Override
    public String getMarkerImgURL() {
        return getImgURL();
    }

    @Override
    public String getTitle() {
        return getName();
    }

    public long getRemainingTime(long cur) {
        return disappearTime - cur;
    }

    public String formatRemainingTime(long cur) {
        long millis = getRemainingTime(cur);

        return String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    public String formatRemainingTime() {
        return formatRemainingTime(System.currentTimeMillis());
    }

    public Intent generateIntent(Context context) {
        Intent intent = new Intent(context, PogoMapActivity.class);
        intent.putExtra(PogoMapActivity.INTENT_LATITUDE, getLatitude());
        intent.putExtra(PogoMapActivity.INTENT_LONGITUDE, getLongitude());
        intent.putExtra(PogoMapActivity.INTENT_SEL_POKEMON, getEncounterId());

        return intent;
    }
}
