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
import android.widget.TextView;

import de.mhid.opensource.socialdistancemeter.R;
import de.mhid.opensource.socialdistancemeter.utils.Markdown;

public class MarkdownActivity extends AppCompatActivity {
    public final static String INTENT_START_ACTIVITY__MARKDOWN_FILE = "markdown_file";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_markdown);

        // load markdown
        String markdownFile = getIntent().getStringExtra(INTENT_START_ACTIVITY__MARKDOWN_FILE);
        TextView markdown = findViewById(R.id.markdown_content);
        Markdown.loadMarkdown(this, markdown, markdownFile);
    }
}