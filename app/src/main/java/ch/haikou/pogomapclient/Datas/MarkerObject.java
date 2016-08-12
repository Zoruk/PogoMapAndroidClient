package ch.haikou.pogomapclient.datas;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Zoruk on 11.08.2016.
 */
public interface MarkerObject {
    boolean hasTimeout();
    long getRemainingTime();
    long nextUpdate();
    String getMarkerUid();
    String getMarkerImgURL();
    String getTitle();
    LatLng getLatLng();

    String formatRemainingTime();
}
