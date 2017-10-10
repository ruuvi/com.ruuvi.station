package com.ruuvi.tag.feature.main;


import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.ruuvi.tag.R;
import com.ruuvi.tag.adapters.RuuviTagAdapter;
import com.ruuvi.tag.util.DataUpdateListener;
import com.ruuvi.tag.util.DeviceIdentifier;

public class RuuviStationFragment extends Fragment implements DataUpdateListener {
    private static final String TAG = "RuuviStationFragment";
    private RuuviTagAdapter adapter;
    private ListView beaconListView;

    public RuuviStationFragment() {
        // Required empty public constructor
    }

    public static RuuviStationFragment newInstance() {
        RuuviStationFragment fragment = new RuuviStationFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ruuvi_station, container, false);

        view.findViewById(R.id.noTags_textView);

        DeviceIdentifier.id(getActivity());

        if (((MainActivity)getActivity()).myRuuviTags.size() > 0) view.findViewById(R.id.noTags_textView).setVisibility(View.GONE);

        beaconListView = view.findViewById(R.id.Tags_listView);
        adapter = new RuuviTagAdapter(getActivity(), ((MainActivity)getActivity()).myRuuviTags);
        beaconListView.setAdapter(adapter);

        adapter.notifyDataSetChanged();

        return view;
    }

    @Override
    public void dataUpdated() {
        adapter.notifyDataSetChanged();
    }
}
