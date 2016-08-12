package ch.haikou.pogomapclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import ch.haikou.pogomapclient.R;

/**
 * Created by Zoruk on 08.08.2016.
 */
public class PokemonAppPreferences {
    private SharedPreferences mSharedPreferences;
    private Context mContext;

    public enum PokestopDisplay {
        NONE,
        LURED_ONLY,
        ALL;


        static public PokestopDisplay fromValue(int v) {
            if (v > PokestopDisplay.values().length) return NONE;
            return PokestopDisplay.values()[v];
        }
    }

    final private String[] POKESTOP_DISPLAY_VALUES;

    final String POKEMONS_TO_SHOW;
    final String POKEMONS_TO_NOTIFY;
    final String SERVICE_ENABLED;
    final String SERVICE_RADIUS;
    final String USERNAME;
    final String PASSWORD;
    final String REFRESH_DELAY;
    final String SERVER_HOST;
    final String USE_SSL;
    final String SERVICE_REFRESH_DELAY;
    final String POKESTOP_DISPLAY;

    public PokemonAppPreferences(Context context) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mContext = context;

        POKESTOP_DISPLAY_VALUES = context.getResources().getStringArray(R.array.pref_display_pokestops);

        POKEMONS_TO_SHOW = mContext.getString(R.string.pref_pokemons_to_show_key);
        POKEMONS_TO_NOTIFY = mContext.getString(R.string.pref_pokemons_to_notify_key);
        SERVICE_ENABLED = mContext.getString(R.string.pref_enable_Background_service_key);
        SERVICE_RADIUS = context.getString(R.string.pref_service_distance_key);
        USERNAME = context.getString(R.string.pref_username_key);
        PASSWORD = context.getString(R.string.pref_password_key);
        REFRESH_DELAY = context.getString(R.string.pref_refresh_delay_key);
        SERVER_HOST = context.getString(R.string.pref_host_key);
        USE_SSL = context.getString(R.string.pref_use_ssl_key);
        SERVICE_REFRESH_DELAY = context.getString(R.string.pref_service_refresh_delay_key);
        POKESTOP_DISPLAY = context.getString(R.string.pref_dislpay_pokestop_key);
    }

    public Set<Integer> getShowablePokemonIDs() {
        return  getPokemonIDs(POKEMONS_TO_SHOW, true);
    }

    public void setShowablePokemonIDs(Set<Integer> ids) {
        setPokemonIDs(POKEMONS_TO_SHOW, ids);
    }

    public Set<Integer> getNotifyPokemonIDs() {
        return  getPokemonIDs(POKEMONS_TO_NOTIFY, false);
    }

    public void setNotifyPokemonIDs(Set<Integer> ids) {
        setPokemonIDs(POKEMONS_TO_NOTIFY, ids);
    }

    private Set<Integer> getPokemonIDs(String property, boolean defaultSelect) {
        Set<String> pokemonStringIDs = mSharedPreferences.getStringSet(property, null);
        if(pokemonStringIDs == null) {
            //Provides the filter with all available pokemon if no filter is set.
            pokemonStringIDs = new HashSet<>();

            if (defaultSelect) {
                int nb_pokemons = mContext.getResources().getInteger(R.integer.pokemon_count);
                for (int i = 1; i <= nb_pokemons; ++i) {
                    pokemonStringIDs.add(String.valueOf(i));
                }
            }
        }
        Set<Integer> pokemonIDs = new HashSet<>();
        for (String stringId : pokemonStringIDs) {
            pokemonIDs.add(Integer.valueOf(stringId));
        }
        return pokemonIDs;
    }

    private void setPokemonIDs(String property, Set<Integer> ids) {
        Set<String> pokemonStringIDs = new HashSet<>();
        for (Integer pokemonId : ids) {
            pokemonStringIDs.add(String.valueOf(pokemonId));
        }
        mSharedPreferences.edit().putStringSet(property, pokemonStringIDs).apply();
    }

    public boolean isServiceEnabled() {
        return mSharedPreferences.getBoolean(SERVICE_ENABLED, false);
    }

    public int getServiceRadius() {
        return Integer.parseInt(mSharedPreferences.getString(SERVICE_RADIUS, "500"));
    }

    public long getRefreshDelay() {
        return Long.parseLong(
                mSharedPreferences.getString(
                        REFRESH_DELAY,
                        "5"
                )
        ) * 1000;
    }

    public String getServer() {
        return mSharedPreferences.getString(SERVER_HOST, null);
    }

    public boolean useSSL() {
        return mSharedPreferences.getBoolean(USE_SSL, true);
    }

    public long getServerRefreshDelay() {
        return Long.parseLong(
               mSharedPreferences.getString(
                       SERVICE_REFRESH_DELAY,
                       "5"
               )
        ) * 1000;
    }

    public String getUsername() {
        return mSharedPreferences.getString(USERNAME, null);
    }

    public String getPassword() {
        return mSharedPreferences.getString(PASSWORD, null);
    }

    public PokestopDisplay getPokestopDisplay() {
        String val = mSharedPreferences.getString(POKESTOP_DISPLAY, POKESTOP_DISPLAY_VALUES[0]);

        for (int i = 0; i < POKESTOP_DISPLAY_VALUES.length; ++i) {
            if (POKESTOP_DISPLAY_VALUES[i].compareTo(val) == 0)
                return PokestopDisplay.fromValue(i);
        }
        return PokestopDisplay.NONE;
    }

    public String getPokemonName(int id) {
        String pokemonsName[] = mContext.getResources().getStringArray(R.array.pokemons);
        if (id >= pokemonsName.length) id = 0;
        return pokemonsName[id];
    }
}
