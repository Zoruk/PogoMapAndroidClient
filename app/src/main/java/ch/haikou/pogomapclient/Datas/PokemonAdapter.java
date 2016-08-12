package ch.haikou.pogomapclient.datas;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.haikou.pogomapclient.R;
import ch.haikou.pogomapclient.utils.PokemonAppPreferences;
import ch.haikou.pogomapclient.utils.RemoteImageLoader;

/**
 * Created by Zoruk on 08.08.2016.
 */

public abstract class PokemonAdapter extends BaseAdapter {

    private LayoutInflater mInflater;

    private final List<CharSequence> mEntries = new ArrayList<>();

    private final Set<Integer> mSelected = new HashSet<>();

    protected PokemonAppPreferences mPref;

    public PokemonAdapter(Context context,
                         CharSequence[] entries) {
        Collections.addAll(mEntries, entries);

        mInflater = LayoutInflater.from(context);
        mPref = new PokemonAppPreferences(context);
        mSelected.addAll(initialSelectedPokemon());
    }

    public abstract Set<Integer> initialSelectedPokemon();

    Set<Integer> getSelectedPokemonIDs() {
        return mSelected;
    }

    @Override
    public int getCount() {
        return mEntries.size();
    }

    @Override
    public Object getItem(int position) {
        return mEntries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        View row = view;
        PokemonAdapter.CustomHolder holder;

        if (row == null) {
            row = mInflater.inflate(R.layout.item_pokemon_to_show_preference, viewGroup, false);
            holder = new PokemonAdapter.CustomHolder(row);
        } else {
            holder = (PokemonAdapter.CustomHolder) row.getTag();
        }

        holder.bind(row, position);
        row.setTag(holder);

        return row;
    }

    private class CustomHolder {
        private CheckedTextView mCheckableTextView = null;
        private ImageView mImageView = null;

        CustomHolder(View row) {
            mCheckableTextView = (CheckedTextView) row.findViewById(R.id.textView);
            mImageView = (ImageView) row.findViewById(R.id.imageView);
        }

        void bind(final View row, final int position) {
            final int pokemonId = position + 1;

            mCheckableTextView.setText((CharSequence) getItem(position));
            mCheckableTextView.setChecked(mSelected.contains(pokemonId));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int pokemonId = position + 1;
                    if (mSelected.contains(pokemonId)) {
                        mSelected.remove(pokemonId);
                    } else {
                        mSelected.add(pokemonId);
                    }
                    mCheckableTextView.setChecked(mSelected.contains(pokemonId));
                }
            });

            RemoteImageLoader.loadInto(mImageView,
                    Pokemon.getImgURL(pokemonId));

        }
    }
}
