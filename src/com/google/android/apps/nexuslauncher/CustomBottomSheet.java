/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.nexuslauncher;

import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.launcher3.AppInfo;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.ItemInfoWithIcon;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.widget.WidgetsBottomSheet;

import org.zimmob.zimlx.EditableItemInfo;
import org.zimmob.zimlx.ZimLauncher;
import org.zimmob.zimlx.ZimPreferences;
import org.zimmob.zimlx.gestures.BlankGestureHandler;
import org.zimmob.zimlx.gestures.GestureHandler;
import org.zimmob.zimlx.gestures.ui.LauncherGesturePreference;
import org.zimmob.zimlx.override.CustomInfoProvider;

public class CustomBottomSheet extends WidgetsBottomSheet {
    private FragmentManager mFragmentManager;
    private EditText mEditTitle;
    private String mPreviousTitle;
    private ItemInfo mItemInfo;
    private CustomInfoProvider<ItemInfo> mInfoProvider;
    private boolean mForceOpen;

    public CustomBottomSheet(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomBottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mFragmentManager = Launcher.getLauncher(context).getFragmentManager();
    }

    @Override
    public void populateAndShow(ItemInfo itemInfo) {
        super.populateAndShow(itemInfo);
        mItemInfo = itemInfo;

        mInfoProvider = CustomInfoProvider.Companion.forItem(getContext(), mItemInfo);

        TextView title = findViewById(R.id.title);
        title.setText(itemInfo.title);
        ((PrefsFragment) mFragmentManager.findFragmentById(R.id.sheet_prefs)).loadForApp(itemInfo,
                this::setForceOpen, this::unsetForceOpen, this::reopen);

        if (itemInfo instanceof ItemInfoWithIcon || mInfoProvider.supportsIcon()) {
            ImageView icon = findViewById(R.id.icon);
            if (itemInfo instanceof ShortcutInfo && ((ShortcutInfo) itemInfo).customIcon != null) {
                icon.setImageBitmap(((ShortcutInfo) itemInfo).customIcon);
            } else if (itemInfo instanceof ItemInfoWithIcon) {
                icon.setImageBitmap(((ItemInfoWithIcon) itemInfo).iconBitmap);
            } else if (itemInfo instanceof FolderInfo) {
                //icon.setImageDrawable(mLauncher.getDrawable(R.drawable.ic_lawnstep));
                icon.setImageDrawable(((FolderInfo) itemInfo).getIcon(mLauncher));
            }
            if (mInfoProvider != null) {
                ZimLauncher launcher = ZimLauncher.getLauncher(getContext());
                icon.setOnClickListener(v -> launcher.startEditIcon(mItemInfo, mInfoProvider));
            }
        }
        if (mInfoProvider != null) {
            mPreviousTitle = mInfoProvider.getCustomTitle(mItemInfo);
            if (mPreviousTitle == null)
                mPreviousTitle = "";
            mEditTitle = findViewById(R.id.edit_title);
            mEditTitle.setHint(mInfoProvider.getDefaultTitle(mItemInfo));
            mEditTitle.setText(mPreviousTitle);
            mEditTitle.setVisibility(VISIBLE);
            title.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        Fragment pf = mFragmentManager.findFragmentById(R.id.sheet_prefs);
        if (pf != null) {
            mFragmentManager.beginTransaction().remove(pf).commitAllowingStateLoss();
        }
        if (mEditTitle != null) {
            String newTitle = mEditTitle.getText().toString();
            if (!newTitle.equals(mPreviousTitle)) {
                if (newTitle.equals(""))
                    newTitle = null;
                ((EditableItemInfo) mItemInfo).setTitle(getContext(), newTitle);
            }
        }
        super.onDetachedFromWindow();
    }

    private void setForceOpen() {
        mForceOpen = true;
    }

    private void unsetForceOpen() {
        mForceOpen = false;
    }

    private void reopen() {
        mForceOpen = false;
        mIsOpen = true;
        mLauncher.getDragLayer().onViewAdded(this);
    }

    @Override
    protected void onWidgetsBound() {
    }

    public static class PrefsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
        private final static String PREF_PACK = "pref_app_icon_pack";
        private final static String PREF_HIDE = "pref_app_hide";
        private final static String PREF_HIDE_FROM_PREDICTIONS = "pref_app_prediction_hide";

        public final static int requestCode = "swipeUp".hashCode() & 65535;
        private final static boolean HIDE_PREDICTION_OPTION = true;
        private ComponentKey mKey;
        private ItemInfo itemInfo;
        private GestureHandler previousHandler;
        private GestureHandler selectedHandler;
        private Runnable setForceOpen;
        private Runnable unsetForceOpen;
        private Runnable reopen;

        private String previousSwipeUpAction;

        CustomInfoProvider mProvider;

        private SwitchPreference mPrefHide;
        private SwitchPreference mPrefHidePredictions;
        private LauncherGesturePreference mSwipeUpPref;
        //private MultiSelectTabPreference mTabsPref;
        private ZimPreferences prefs;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.app_edit_prefs);

            if (!Utilities.getZimPrefs(getActivity()).getShowDebugInfo()) {
                getPreferenceScreen().removePreference(getPreferenceScreen().findPreference("debug"));
            } else {
                getPreferenceScreen().findPreference("componentName").setOnPreferenceClickListener(this);
            }
        }

        public void loadForApp(ItemInfo info, Runnable setForceOpen, Runnable unsetForceOpen, Runnable reopen) {
            itemInfo = info;
            this.setForceOpen = setForceOpen;
            this.unsetForceOpen = unsetForceOpen;
            this.reopen = reopen;

            mProvider = CustomInfoProvider.Companion.forItem(getActivity(), info);

            Context context = getActivity();
            boolean isApp = itemInfo instanceof AppInfo || itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;

            PreferenceScreen screen = getPreferenceScreen();
            prefs = Utilities.getZimPrefs(getActivity());
            mSwipeUpPref = (LauncherGesturePreference) screen.findPreference("pref_swipe_up_gesture");
            //mTabsPref = (MultiSelectTabPreference) screen.findPreference("pref_show_in_tabs");
            mKey = new ComponentKey(itemInfo.getTargetComponent(), itemInfo.user);
            mPrefHide = (SwitchPreference) findPreference(PREF_HIDE);

            if (isApp) {
                mPrefHide.setChecked(CustomAppFilter.isHiddenApp(context, mKey));
                mPrefHide.setOnPreferenceChangeListener(this);
            } else {
                screen.removePreference(mPrefHide);
            }

            /*if (!isApp || prefs.getDrawerTabs().getGroups().isEmpty()) {
                screen.removePreference(mTabsPref);
            } else {
                mTabsPref.setComponentKey(mKey);
                mTabsPref.loadSummary();
            }*/

            if (mProvider != null && mProvider.supportsSwipeUp()) {
                previousSwipeUpAction = mProvider.getSwipeUpAction(itemInfo);
                mSwipeUpPref.setValue(previousSwipeUpAction);
                mSwipeUpPref.setOnSelectHandler(gestureHandler -> {
                    onSelectHandler(gestureHandler);
                    return null;
                });
            } else {
                getPreferenceScreen().removePreference(mSwipeUpPref);
            }

            if (mPrefHidePredictions != null) {
                mPrefHidePredictions.setChecked(CustomAppPredictor.isHiddenApp(context, mKey));
                mPrefHidePredictions.setOnPreferenceChangeListener(this);
            }

            if (prefs.getShowDebugInfo() && mKey.componentName != null) {
                Preference componentPref = getPreferenceScreen().findPreference("componentName");
                componentPref.setOnPreferenceClickListener(this);
                componentPref.setSummary(mKey.toString());
            } else {
                getPreferenceScreen().removePreference(getPreferenceScreen().findPreference("debug"));
            }

            mPrefHidePredictions = (SwitchPreference) getPreferenceScreen()
                    .findPreference(PREF_HIDE_FROM_PREDICTIONS);
            if ((!prefs.getShowPredictions() || HIDE_PREDICTION_OPTION)
                    && mPrefHidePredictions != null) {
                getPreferenceScreen().removePreference(mPrefHidePredictions);
            }
        }

        private void onSelectHandler(GestureHandler handler) {
            previousHandler = selectedHandler;
            selectedHandler = handler;
            if (handler.getConfigIntent() != null) {
                setForceOpen.run();
                startActivityForResult(handler.getConfigIntent(), PrefsFragment.requestCode);
            } else {
                updatePref();
            }
        }

        private void updatePref() {
            if (mProvider != null && selectedHandler != null) {
                setForceOpen.run();
                String stringValue;
                if (selectedHandler instanceof BlankGestureHandler) {
                    stringValue = null;
                } else {
                    stringValue = selectedHandler.toString();
                }

                Dialog dialog = mSwipeUpPref.getDialog();
                if (dialog != null) {
                    dialog.dismiss();
                }
                mSwipeUpPref.setValue(stringValue);
                unsetForceOpen.run();
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean enabled = (boolean) newValue;
            Launcher launcher = Launcher.getLauncher(getActivity());
            switch (preference.getKey()) {
                case PREF_HIDE:
                    CustomAppFilter.setComponentNameState(launcher, mKey, enabled);
                    break;

                case PREF_HIDE_FROM_PREDICTIONS:
                    CustomAppPredictor.setComponentNameState(launcher, mKey, enabled);
            }
            return true;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.debug_component_name), mKey.componentName.flattenToString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getActivity(), R.string.debug_component_name_copied, Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    public static void show(Launcher launcher, ItemInfo itemInfo) {
        CustomBottomSheet cbs = (CustomBottomSheet) launcher.getLayoutInflater()
                .inflate(R.layout.app_edit_bottom_sheet, launcher.getDragLayer(), false);
        cbs.populateAndShow(itemInfo);
    }
}
