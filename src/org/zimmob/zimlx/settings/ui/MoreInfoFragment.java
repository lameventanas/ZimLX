package org.zimmob.zimlx.settings.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.android.launcher3.R;

import net.gsantner.opoc.format.markdown.SimpleMarkdownParser;
import net.gsantner.opoc.preference.GsPreferenceFragmentCompat;
import net.gsantner.opoc.util.ActivityUtils;
import net.gsantner.opoc.util.ShareUtil;

import org.zimmob.zimlx.settings.AppSettings;

import java.io.IOException;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

/**
 * Created by saul on 04-25-18.
 * Project ZimLX
 * henriquez.saul@gmail.com
 */
public class MoreInfoFragment extends GsPreferenceFragmentCompat<AppSettings> {
    public static final String TAG = "MoreInfoFragment";

    @NonNull
    public static MoreInfoFragment newInstance() {
        return new MoreInfoFragment();
    }

    @Override
    public int getPreferenceResourceForInflation() {
        return R.xml.prefactions__more_information;
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }

    @Override
    protected AppSettings getAppSettings(Context context) {
        return _appSettings != null ? _appSettings : new AppSettings(context);
    }

    @Override
    @SuppressWarnings({"ConstantConditions", "ConstantIfStatement", "StatementWithEmptyBody"})
    public Boolean onPreferenceClicked(Preference preference) {
        ActivityUtils au = new ActivityUtils(getActivity());
        if (isAdded() && preference.hasKey()) {
            switch (keyToStringResId(preference)) {
                case R.string.pref_key__more_info__settings: {
                    au.animateToActivity(SettingsActivity.class, false, 124);
                    return true;
                }
                case R.string.pref_key__more_info__rate_app: {
                    au.showGooglePlayEntryForThisApp();
                    return true;
                }

                case R.string.pref_key__more_info__donate: {
                    _cu.openWebpageInExternalBrowser(getString(R.string.app_donate_url));
                    return true;
                }
                case R.string.pref_key__more_info__bug_reports: {
                    _cu.openWebpageInExternalBrowser(getString(R.string.app_bugreport_url));
                    return true;
                }
                case R.string.pref_key__more_info__translate: {
                    _cu.openWebpageInExternalBrowser(getString(R.string.app_translate_url));
                    return true;
                }
                case R.string.pref_key__more_info__project_contribution_info: {
                    _cu.openWebpageInExternalBrowser(getString(R.string.app_contribution_info_url));
                    return true;
                }
                case R.string.pref_key__more_info__android_contribution_guide: {
                    _cu.openWebpageInExternalBrowser(
                            String.format("https://paypal.me/saulhenriquez",
                                    _cu.context().getPackageName(), getString(R.string.app_name), getString(R.string.app_web_url).replace("=", "%3D")));
                    return true;
                }
                case R.string.pref_key__more_info__source_code: {
                    _cu.openWebpageInExternalBrowser(getString(R.string.app_source_code_url));
                    return true;
                }
                case R.string.pref_key__more_info__project_license: {
                    try {
                        au.showDialogWithHtmlTextView(R.string.licenses, new SimpleMarkdownParser().parse(
                                getResources().openRawResource(R.raw.license),
                                "", SimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW).getHtml());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                case R.string.pref_key__more_info__open_source_licenses: {
                    try {
                        au.showDialogWithHtmlTextView(R.string.licenses, new SimpleMarkdownParser().parse(
                                getResources().openRawResource(R.raw.licenses_3rd_party),
                                "", SimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW).getHtml());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                case R.string.pref_key__more_info__contributors_public_info: {
                    try {
                        au.showDialogWithHtmlTextView(R.string.contributors, new SimpleMarkdownParser().parse(
                                getResources().openRawResource(R.raw.contributors),
                                "", SimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW).getHtml());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                case R.string.pref_key__more_info__copy_build_information: {
                    new ShareUtil(getContext()).setClipboard(preference.getSummary());
                    SimpleMarkdownParser smp = new SimpleMarkdownParser();
                    try {
                        String html = smp.parse(getResources().openRawResource(R.raw.changelog), "", SimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW, SimpleMarkdownParser.FILTER_CHANGELOG).getHtml();
                        au.showDialogWithHtmlTextView(R.string.changelog, html);
                    } catch (Exception ignored) {

                    }
                    return true;
                }
            }
        }
        return null;
    }

    @Override
    protected boolean isAllowedToTint(Preference pref) {
        return !getString(R.string.pref_key__more_info__app).equals(pref.getKey());
    }

    @Override
    public synchronized void doUpdatePreferences() {
        super.doUpdatePreferences();
        Context context = getContext();
        if (context == null) {
            return;
        }
        Locale locale = Locale.getDefault();
        String tmp;
        Preference pref;
        updateSummary(R.string.pref_key__more_info__project_license, getString(R.string.app_license_name));

        // Basic app info
        if ((pref = findPreference(R.string.pref_key__more_info__app)) != null && pref.getSummary() == null) {
            pref.setIcon(R.drawable.ic_launcher);
            pref.setSummary(String.format(locale, "%s\nVersion v%s (build %d)", _cu.getPackageName(), _cu.getAppVersionName(), _cu.getAppVersionCode()));
        }

        // Extract some build information and publish in summary
        if ((pref = findPreference(R.string.pref_key__more_info__copy_build_information)) != null && pref.getSummary() == null) {

            String summary = String.format(locale, "\n<b>Package:</b> %s\n<b>Version:</b> v%s (build %s)", _cu.getPackageName(), _cu.getAppVersionName(), _cu.getAppVersionCode());
            summary += (tmp = _cu.bcstr("FLAVOR", "")).isEmpty() ? "" : ("\n<b>Flavor:</b> " + tmp.replace("flavor", ""));
            summary += (tmp = _cu.bcstr("BUILD_TYPE", "")).isEmpty() ? "" : (" (" + tmp + ")");
            summary += (tmp = _cu.bcstr("BUILD_DATE", "")).isEmpty() ? "" : ("\n<b>Build date:</b> " + tmp);
            summary += (tmp = _cu.getAppInstallationSource()).isEmpty() ? "" : ("\n<b>ISource:</b> " + tmp);
            summary += (tmp = _cu.bcstr("GITHASH", "")).isEmpty() ? "" : ("\n<b>VCS Hash:</b> " + tmp);
            pref.setSummary(_cu.htmlToSpanned(summary.trim().replace("\n", "<br/>")));

        }

        // Extract project team from raw resource, where 1 person = 4 lines
        // 1) Name/Title, 2) Description/Summary, 3) Link/View-Intent, 4) Empty line
        if ((pref = findPreference(R.string.pref_key__more_info__project_team)) != null && ((PreferenceGroup) pref).getPreferenceCount() == 0) {
            String[] data = (_cu.readTextfileFromRawRes(R.raw.project_team, "", "").trim() + "\n\n").split("\n");
            for (int i = 0; i + 2 < data.length; i += 4) {
                Preference person = new Preference(context);
                person.setTitle(data[i]);
                person.setSummary(data[i + 1]);
                person.setIcon(R.drawable.ic_person_black_24dp);
                try {
                    Uri uri = Uri.parse(data[i + 2]);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    person.setIntent(intent);
                } catch (Exception ignored) {
                }
                appendPreference(person, (PreferenceGroup) pref);
            }

        }
    }
}