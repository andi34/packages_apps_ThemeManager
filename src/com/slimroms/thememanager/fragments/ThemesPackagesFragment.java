package com.slimroms.thememanager.fragments;

import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.slimroms.themecore.IThemeService;
import com.slimroms.themecore.Theme;
import com.slimroms.thememanager.App;
import com.slimroms.thememanager.R;
import com.slimroms.thememanager.adapters.ThemesPackagesAdapter;
import com.slimroms.thememanager.helpers.BroadcastHelper;
import com.slimroms.thememanager.views.LineDividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class ThemesPackagesFragment extends Fragment {
    public static final String TAG = ThemesPackagesFragment.class.getSimpleName();

    public static ThemesPackagesFragment newInstance() {
        return new ThemesPackagesFragment();
    }

    private TextView mEmptyView;
    private RecyclerView mRecycler;
    private ThemesPackagesAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle(R.string.nav_themes);
        mRecycler = (RecyclerView) view.findViewById(R.id.list);
        mEmptyView = (TextView) view.findViewById(R.id.empty_view);

        mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecycler.addItemDecoration(new LineDividerItemDecoration(getContext()));
        mRecycler.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new ThemesPackagesAdapter(getContext());
        mRecycler.setAdapter(mAdapter);
    }

    private BroadcastReceiver mConnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final ComponentName backendName = intent.getParcelableExtra(BroadcastHelper.EXTRA_BACKEND_NAME);
            switch (intent.getAction()) {
                case BroadcastHelper.ACTION_BACKEND_CONNECTED:
                    new AsyncTask<ComponentName, Void, List<Theme>>() {
                        @Override
                        protected List<Theme> doInBackground(ComponentName... componentNames) {
                            if (componentNames[0] != null) {
                                final IThemeService backend = App.getInstance().getBackend(componentNames[0]);
                                try {
                                    final List<Theme> result = new ArrayList<>();
                                    final int count = backend.getThemePackages(result);
                                    return (count > 0) ? result : null;
                                }
                                catch (RemoteException ex) {
                                    ex.printStackTrace();
                                    return null;
                                }
                            }
                            else
                                return null;
                        }

                        @Override
                        protected void onPostExecute(List<Theme> themes) {
                            if (themes != null) {
                                mAdapter.addThemes(themes);
                            }
                            mEmptyView.setVisibility(mAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
                        }
                    }.execute(backendName);
                    break;
                case BroadcastHelper.ACTION_BACKEND_DISCONNECTED:
                    mAdapter.removeThemes(backendName);
                    mEmptyView.setVisibility(mAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
                    break;
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mConnectReceiver,
                BroadcastHelper.getBackendConnectFilter());
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mConnectReceiver);
        super.onStop();
    }
}
