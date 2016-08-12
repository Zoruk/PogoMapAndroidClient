package ch.haikou.pogomapclient.datas;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.haikou.pogomapclient.R;
import ch.haikou.pogomapclient.utils.PokemonAppPreferences;

/**
 * Created by Zoruk on 08.08.2016.
 */

public abstract class PokemonsPreference extends MultiSelectListPreference {
    private PokemonAdapter mAdapter;
    protected PokemonAppPreferences mPref;

    public PokemonsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public PokemonsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public PokemonsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PokemonsPreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> entriesValues = new ArrayList<>();
        Set<String> defaultValues = new HashSet<>();
        defaultValues.add(String.valueOf(0));

        mPref = new PokemonAppPreferences(context);

        String pokemons[] = context.getResources().getStringArray(R.array.pokemons);
        int nbPokemon = context.getResources().getInteger(R.integer.pokemon_count);

        for (int i = 1; i <= nbPokemon; ++i) {
            entries.add(pokemons[i]);

                entriesValues.add(String.valueOf(i));
            if (isDefaultSelected()) {
                defaultValues.add(String.valueOf(i));
            }
        }

        setEntries(entries.toArray(new CharSequence[]{}));
        setEntryValues(entriesValues.toArray(new CharSequence[]{}));

        // all pokemons are shown by default
        setDefaultValue(defaultValues);
    }

    abstract public boolean isDefaultSelected();

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        final CharSequence[] entries = getEntries();
        final CharSequence[] entryValues = getEntryValues();

        if (entries == null || entryValues == null || entries.length != entryValues.length) {
            throw new IllegalStateException("ListPreference requires an entries array and an entryValues array which are both the same length");
        }

        mAdapter = createAdapter(entries);
        builder.setAdapter(mAdapter, null);
    }

    abstract public PokemonAdapter createAdapter(CharSequence[] entries);

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            Set<Integer> pokemonIDs = mAdapter.getSelectedPokemonIDs();

            save(pokemonIDs);
        }
    }
    abstract public void save(Set<Integer> ids);
}
