package com.ruuvi.station.feature.main;


import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import com.ruuvi.station.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AddTagFragment extends Fragment implements DataUpdateListener {
    private AddTagAdapter adapter;
    private ListView beaconListView;
    private TextView noTagsTextView;
    private List<RuuviTag> tags;

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
        tags = new ArrayList<>();
        adapter = new AddTagAdapter(getActivity(), tags);
        beaconListView.setAdapter(adapter);

        beaconListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                RuuviTag tag = (RuuviTag)beaconListView.getItemAtPosition(i);
                if (RuuviTag.get(tag.id).favorite) {
                    Toast.makeText(getActivity(), getActivity().getString(R.string.tag_already_added), Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                tag.defaultBackground = (int)(Math.random() * 9.0);
                tag.favorite = true;
                tag.update();
                ScannerService.logTag(tag);
                Intent settingsIntent = new Intent(getActivity(), TagSettings.class);
                settingsIntent.putExtra(TagSettings.TAG_ID, tag.id);
                startActivityForResult(settingsIntent, 1);
            }
        });

        adapter.notifyDataSetChanged();

        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                tags.clear();
                tags.addAll(RuuviTag.getAll(false));
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.SECOND, -5);
                for (int i = 0; i < tags.size(); i++) {
                    if (tags.get(i).updateAt.getTime() < calendar.getTime().getTime()) {
                        tags.remove(i);
                        i--;
                    }
                }
                if (tags.size() > 0) {
                    Utils.sortTagsByRssi(tags);
                    noTagsTextView.setVisibility(View.INVISIBLE);
                }
                else noTagsTextView.setVisibility(View.VISIBLE);
                if (adapter != null)  adapter.notifyDataSetChanged();
                handler.postDelayed(this, 1000);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void dataUpdated() {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ((MainActivity)getActivity()).openFragment(1);
    }
}
