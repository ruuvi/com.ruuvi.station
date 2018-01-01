package com.ruuvi.station.feature.main;


import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ruuvi.station.R;
import com.ruuvi.station.adapters.AddTagAdapter;
import com.ruuvi.station.feature.TagSettings;
import com.ruuvi.station.model.RuuviTag;
import com.ruuvi.station.service.ScannerService;
import com.ruuvi.station.util.DataUpdateListener;

public class AddTagFragment extends Fragment implements DataUpdateListener {
    private AddTagAdapter adapter;
    private ListView beaconListView;
    private TextView noTagsTextView;

    public AddTagFragment() {
        // Required empty public constructor
    }

    public static AddTagFragment newInstance() {
        AddTagFragment fragment = new AddTagFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_add_tag, container, false);

        noTagsTextView = view.findViewById(R.id.no_tags);
        beaconListView = view.findViewById(R.id.tag_listView);
        adapter = new AddTagAdapter(getActivity(), ((MainActivity)getActivity()).otherRuuviTags);
        beaconListView.setAdapter(adapter);

        beaconListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                RuuviTag tag = (RuuviTag)beaconListView.getItemAtPosition(i);
                if (RuuviTag.get(tag.id) != null) {
                    Toast.makeText(getActivity(), getActivity().getString(R.string.tag_already_added), Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                tag.save();
                ScannerService.logTag(tag);
                ((MainActivity)getActivity()).openFragment(1);
                Intent settingsIntent = new Intent(getActivity(), TagSettings.class);
                settingsIntent.putExtra(TagSettings.TAG_ID, tag.id);
                startActivity(settingsIntent);
            }
        });

        adapter.notifyDataSetChanged();

        return view;
    }

    @Override
    public void dataUpdated() {
        if (adapter != null) adapter.notifyDataSetChanged();

        if (((MainActivity)getActivity()).otherRuuviTags.size() > 0) {
            noTagsTextView.setVisibility(View.INVISIBLE);
        } else {
            noTagsTextView.setVisibility(View.VISIBLE);
        }
    }
}
