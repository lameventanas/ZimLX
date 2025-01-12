package com.google.android.apps.nexuslauncher.qsb;

import android.content.Context;
import android.util.AttributeSet;

import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.Launcher;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.allapps.AllAppsGridAdapter;
import com.android.launcher3.allapps.AllAppsRecyclerView;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.allapps.search.AllAppsSearchBarController;
import com.google.android.apps.nexuslauncher.search.SearchThread;

import java.util.ArrayList;


public class FallbackAppsSearchView extends ExtendedEditText implements AllAppsSearchBarController.Callbacks {
    private final AllAppsSearchBarController mSearchBarController;
    private AllAppsQsbLayout mQsbLayout;
    private AllAppsGridAdapter mAdapter;
    public AlphabeticalAppsList mApps;
    private AllAppsRecyclerView mAppsRecyclerView;
    final AllAppsSearchBarController DI;
    AllAppsQsbLayout DJ;
    AllAppsContainerView mAppsView;

    public FallbackAppsSearchView(Context context) {
        this(context, null);
    }

    public FallbackAppsSearchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FallbackAppsSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mSearchBarController = new AllAppsSearchBarController();

        this.DI = new AllAppsSearchBarController();
    }


    public void bu(AllAppsQsbLayout qsbLayout, AlphabeticalAppsList apps, AllAppsRecyclerView appsRecyclerView) {
        mQsbLayout = qsbLayout;
        mApps = apps;
        mAppsRecyclerView = appsRecyclerView;
        mAdapter = (AllAppsGridAdapter) appsRecyclerView.getAdapter();
        mSearchBarController.initialize(new SearchThread(getContext()), this, Launcher.getLauncher(getContext()), this);
    }

    public void clearSearchResult() {
        if (getParent() != null && mApps.setOrderedFilter(null)) {
            if (mApps.setOrderedFilter(null)) {
                dV();
            }
            x(false);
            DJ.mDoNotRemoveFallback = true;
            mAppsView.onClearSearchResult();
            DJ.mDoNotRemoveFallback = false;
        }
    }

    public void onSearchResult(final String lastSearchQuery, final ArrayList orderedFilter) {
        if (orderedFilter != null && getParent() != null) {
            mApps.setOrderedFilter(orderedFilter);
            mAppsView.setLastSearchQuery(lastSearchQuery);
        }
    }

    public void refreshSearchResult() {
        mSearchBarController.refreshSearchResult();
    }

    private void x(boolean z) {
        //PredictionsFloatingHeader predictionsFloatingHeader = (PredictionsFloatingHeader) mAppsView.getFloatingHeaderView();
        //predictionsFloatingHeader.setCollapsed(z);
    }

    private void dV() {
        this.DJ.setShadowAlpha(0);
        mAppsView.onSearchResultsChanged();
    }

}
