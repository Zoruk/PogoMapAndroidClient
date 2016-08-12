package ch.haikou.pogomapclient;

import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import ch.haikou.pogomapclient.datas.Pokemon;

/**
 * Created by Zoruk on 10.08.2016.
 */
public class PokemonInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private Pokemon mPokemon;

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}
