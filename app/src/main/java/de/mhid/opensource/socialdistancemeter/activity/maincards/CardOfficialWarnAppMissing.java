/*
Social Distance Meter - An app to analyze and rate your social distancing behavior
Copyright (C) 2020  Mirko Hansen (baaazen@gmail.com)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package de.mhid.opensource.socialdistancemeter.activity.maincards;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import de.mhid.opensource.socialdistancemeter.R;
import de.mhid.opensource.socialdistancemeter.activity.MainActivity;

public class CardOfficialWarnAppMissing {
    private final MainActivity mainActivity;

    public CardOfficialWarnAppMissing(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        init();
    }

    private void init() {
        String[] warnAppPackageNames = mainActivity.getResources().getStringArray(R.array.official_warn_app_package_names);
        boolean foundWarnApp = false;
        for(String warnAppPackageName : warnAppPackageNames) {
            if(isAppInstalled(warnAppPackageName)) {
                foundWarnApp = true;
                break;
            }
        }

        CardView cardOfficialWarnAppMissing = mainActivity.findViewById(R.id.card_official_warn_app_missing);
        if(foundWarnApp) {
           cardOfficialWarnAppMissing.setVisibility(View.GONE);
        } else {
            String cwaName = mainActivity.getString(R.string.national_warn_app_name);

            TextView description = mainActivity.findViewById(R.id.card_official_warn_app_missing_description);
            description.setText(mainActivity.getString(R.string.card_official_warn_app_missing_description, cwaName));

            TextView command = mainActivity.findViewById(R.id.card_official_warn_app_missing_command);
            command.setText(mainActivity.getString(R.string.card_official_warn_app_missing_command_install, cwaName));

            cardOfficialWarnAppMissing.setOnClickListener(v -> openOfficialWarnAppInPlayStore());
            cardOfficialWarnAppMissing.setVisibility(View.VISIBLE);
        }
    }

    private boolean isAppInstalled(String packageName) {
        try {
            mainActivity.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void openOfficialWarnAppInPlayStore() {
        String packageName = mainActivity.getString(R.string.national_warn_app_package_name);
        try {
            mainActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            mainActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }
}
