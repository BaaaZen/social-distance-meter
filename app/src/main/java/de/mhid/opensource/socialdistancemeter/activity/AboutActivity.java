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
package de.mhid.opensource.socialdistancemeter.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import de.mhid.opensource.socialdistancemeter.AppInformation;
import de.mhid.opensource.socialdistancemeter.R;
import de.mhid.opensource.socialdistancemeter.utils.Markdown;
import io.noties.markwon.Markwon;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // update version
        TextView version = findViewById(R.id.about_version);
        version.setText(Html.fromHtml(getString(R.string.about_version, "<b>" + AppInformation.VERSION + "</b>")));

        // update url/homepage
        TextView homepage = findViewById(R.id.about_url);
        homepage.setText(Html.fromHtml("<a href=\"" + AppInformation.HOMEPAGE + "\">" + AppInformation.HOMEPAGE + "</a>"));
        homepage.setMovementMethod(LinkMovementMethod.getInstance());

        // MARKDOWN: about.md
        TextView aboutMarkdown = findViewById(R.id.about_about_md);
        Markdown.loadMarkdown(this, aboutMarkdown, "about");
    }
}