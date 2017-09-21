package com.ruuvi.tag.feature.main;


import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.ruuvi.tag.R;
import com.ruuvi.tag.adapters.RuuviTagAdapter;
import com.ruuvi.tag.feature.list.ListActivity;
import com.ruuvi.tag.model.RuuviTag;
import com.ruuvi.tag.util.DeviceIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RuuviStationFragment extends Fragment {
    private RuuviTagAdapter adapter;
    private ListView beaconListView;
    private Timer timer;
    private View text;
    private List<RuuviTag> tags = new ArrayList<>();

    private void setTimerForAdvertise() {
        timer = new Timer();
        TimerTask updateProfile = new CustomTimerTask();
        timer.scheduleAtFixedRate(updateProfile, 0, 1000);
    }

    private class CustomTimerTask extends TimerTask {
        private Handler mHandler = new Handler();

        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            adapter.clear();
                            adapter.addAll(RuuviTag.getAll());
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            }).start();
        }
    }


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

        text = view.findViewById(R.id.noTags_textView);

        DeviceIdentifier.id(getActivity());

        tags = RuuviTag.getAll();
        if (tags.size() > 0) view.findViewById(R.id.noTags_textView).setVisibility(View.GONE);

        beaconListView = view.findViewById(R.id.Tags_listView);
        adapter = new RuuviTagAdapter(getActivity(), tags);
        beaconListView.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fab_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ListActivity.class);
                startActivity(intent);
            }
        });

        setTimerForAdvertise();
        adapter.notifyDataSetChanged();

        return view;
    }

}
