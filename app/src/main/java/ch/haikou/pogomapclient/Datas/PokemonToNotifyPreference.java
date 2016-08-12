package ch.haikou.pogomapclient.datas;

import android.content.Context;
import android.util.AttributeSet;

import java.util.Set;

/**
 * Created by Zoruk on 08.08.2016.
 */

public class PokemonToNotifyPreference extends PokemonsPreference {
    public PokemonToNotifyPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PokemonToNotifyPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PokemonToNotifyPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PokemonToNotifyPreference(Context context) {
        super(context);
    }

    @Override
    public boolean isDefaultSelected() {
        return false;
    }

    @Override
    public PokemonAdapter createAdapter(CharSequence[] entries) {
        return new PokemonAdapter(getContext(), entries) {
            @Override
            public Set<Integer> initialSelectedPokemon() {
                return mPref.getNotifyPokemonIDs();
            }
        };
    }

    @Override
    public void save(Set<Integer> ids) {
        mPref.setNotifyPokemonIDs(ids);
    }
}
