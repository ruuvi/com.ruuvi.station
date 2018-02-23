package com.ruuvi.station.feature.main;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.ruuvi.station.R;
import com.ruuvi.station.adapters.RuuviTagAdapter;
import com.ruuvi.station.feature.TagDetails;
import com.ruuvi.station.model.RuuviTag;
import com.ruuvi.station.util.DataUpdateListener;
import com.ruuvi.station.util.DeviceIdentifier;

public class DashboardFragment extends Fragment implements DataUpdateListener {
    private static final String TAG = "DashboardFragment";
    private RuuviTagAdapter adapter;
    private ListView beaconListView;
    private View noTagsFound;

    public DashboardFragment() {
        // Required empty public constructor
    }

    public static DashboardFragment newInstance() {
        DashboardFragment fragment = new DashboardFragment();
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
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        noTagsFound = view.findViewById(R.id.noTags_textView);

        DeviceIdentifier.id(getActivity());

        beaconListView = view.findViewById(R.id.Tags_listView);
        adapter = new RuuviTagAdapter(getActivity(), ((MainActivity)getActivity()).myRuuviTags);
        beaconListView.setAdapter(adapter);

        beaconListView.setOnItemClickListener(tagClick);

        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null && ((MainActivity)getActivity()).myRuuviTags != null &&
                        ((MainActivity)getActivity()).myRuuviTags.size() > 0) {
                    noTagsFound.setVisibility(View.GONE);
                }
                else noTagsFound.setVisibility(View.VISIBLE);
                if (adapter != null)  adapter.notifyDataSetChanged();
                handler.postDelayed(this, 1000);
            }
        });

        return view;
    }

    private AdapterView.OnItemClickListener tagClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final RuuviTag tag = (RuuviTag)view.getTag();
            Intent intent = new Intent(getActivity(), TagDetails.class);
            intent.putExtra("id", tag.id);
            startActivity(intent);
        }
    };

    public void delete(final RuuviTag tag) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getActivity().getString(R.string.tag_delete_title));
        builder.setMessage(getActivity().getString(R.string.tag_delete_message));
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ((MainActivity)getActivity()).myRuuviTags.remove(tag);
                tag.deleteTagAndRelatives();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        builder.show();
    }

    @Override
    public void dataUpdated() {
    }
}
