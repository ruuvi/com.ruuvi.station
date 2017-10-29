package com.ruuvi.tag.feature.main;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.ruuvi.tag.R;
import com.ruuvi.tag.adapters.RuuviTagAdapter;
import com.ruuvi.tag.feature.TagSettings;
import com.ruuvi.tag.feature.edit.AlarmEditActivity;
import com.ruuvi.tag.feature.edit.EditActivity;
import com.ruuvi.tag.feature.plot.PlotActivity;
import com.ruuvi.tag.model.RuuviTag;
import com.ruuvi.tag.util.DataUpdateListener;
import com.ruuvi.tag.util.DeviceIdentifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DashboardFragment extends Fragment implements DataUpdateListener {
    private static final String TAG = "DashboardFragment";
    private RuuviTagAdapter adapter;
    private ListView beaconListView;

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

        view.findViewById(R.id.noTags_textView);

        DeviceIdentifier.id(getActivity());

        if (((MainActivity)getActivity()).myRuuviTags.size() > 0) view.findViewById(R.id.noTags_textView).setVisibility(View.GONE);

        beaconListView = view.findViewById(R.id.Tags_listView);
        adapter = new RuuviTagAdapter(getActivity(), ((MainActivity)getActivity()).myRuuviTags);
        beaconListView.setAdapter(adapter);

        beaconListView.setOnItemClickListener(tagClick);

        adapter.notifyDataSetChanged();

        return view;
    }

    private AdapterView.OnItemClickListener tagClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final RuuviTag tag = (RuuviTag)view.getTag();

            final BottomSheetDialog dialog = new BottomSheetDialog(getActivity());

            ListView listView = new ListView(getActivity());

            List<String> menu = new ArrayList<>(Arrays.asList(getActivity().getResources().getStringArray(R.array.station_tag_menu)));

            if (tag.url != null && !tag.url.isEmpty()) {
                menu.add(getActivity().getResources().getString(R.string.share));
            }

            listView.setAdapter(
                    new ArrayAdapter<>(
                            getActivity(),
                            android.R.layout.simple_list_item_1,
                            menu
                    )
            );

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (i == 0) {
                        Toast.makeText(getActivity(), "Not yet implemented", Toast.LENGTH_SHORT).show();
                    } else if (i == 1) {
                        Intent intent = new Intent(getActivity(), AlarmEditActivity.class);
                        intent.putExtra("tagId", tag.id);
                        getActivity().startActivity(intent);
                    } else if (i == 2) {
                        Intent intent = new Intent(getActivity(), TagSettings.class);
                        intent.putExtra(TagSettings.TAG_ID, tag.id);
                        getActivity().startActivity(intent);
                    } else if (i == 3) {
                        delete(tag);
                    } else if (i == 4) {
                        Toast.makeText(getActivity(), "Not working in this build", Toast.LENGTH_SHORT).show();
                        /*
                        Intent intent = new Intent(getActivity(), PlotActivity.class);
                        intent.putExtra("id", tag.id);
                        getActivity().startActivity(intent);
                        */
                    } else if (i == 5) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL");
                        intent.putExtra(Intent.EXTRA_TEXT, tag.url);
                        getActivity().startActivity(Intent.createChooser(intent, "Share URL"));
                    }

                    dialog.dismiss();
                }
            });

            dialog.setContentView(listView);
            dialog.show();
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
        if (adapter != null) adapter.notifyDataSetChanged();
    }
}
