package com.ruuvi.tag.feature.main;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ruuvi.tag.BuildConfig;
import com.ruuvi.tag.R;

public class AboutFragment extends Fragment {

    public AboutFragment() {
    }

    public static AboutFragment newInstance() {
        AboutFragment fragment = new AboutFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        ((TextView)view.findViewById(R.id.versionInfo))
                .setText("Version: " + BuildConfig.VERSION_NAME
                        + " (" + BuildConfig.VERSION_CODE + ")");

        return view;
    }
}
