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
package de.mhid.opensource.socialdistancemeter.utils;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

import de.mhid.opensource.socialdistancemeter.R;
import io.noties.markwon.Markwon;

public class Markdown {
    private static String loadMarkdownFileFromAssets(@NonNull Context ctx, @NonNull String filename) throws IOException {
        InputStream is = ctx.getAssets().open(getMarkdownFile(filename));
        byte[] content = new byte[is.available()];
        int byteCount = is.read(content);
        is.close();

        // end of file
        if(byteCount == -1) return null;

        return new String(content, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean existsMarkdownFileFromAssets(Context ctx, String filename) {
        try {
            return Arrays.asList(ctx.getAssets().list("")).contains(getMarkdownFile(filename));
        } catch (IOException e) {
            return false;
        }
    }

    private static String getMarkdownFile(String filenameWithoutExtension) {
        return filenameWithoutExtension + ".md";
    }

    public static void loadMarkdown(Context ctx, TextView textView, String filenameWithoutExtension) {
        if(filenameWithoutExtension == null) {
            textView.setText(ctx.getString(R.string.markdown_error, "<unknown>", "Missing filename."));
            return;
        }

        String localeExtension = "_" + Locale.getDefault().getLanguage();
        String filename = filenameWithoutExtension + localeExtension;

        // check if locale file exists
        if(!existsMarkdownFileFromAssets(ctx, filename)) filename = filenameWithoutExtension;
        // check if global file exists
        if(!existsMarkdownFileFromAssets(ctx, filename)) {
            // markdown file missing -> show error
            textView.setText(ctx.getString(R.string.markdown_error, filename, "File not found."));
            return;
        }

        try {
            // load file
            String markdownContent = loadMarkdownFileFromAssets(ctx, filename);

            if(markdownContent != null) {
                // parse markdown
                Markwon.create(ctx).setMarkdown(textView, markdownContent);
            } else {
                textView.setText(ctx.getString(R.string.markdown_error, filename, "end of file"));
            }
        } catch (IOException e) {
            // markdown file missing -> show error
            textView.setText(ctx.getString(R.string.markdown_error, filename, e.getMessage()));
        }
    }
}
