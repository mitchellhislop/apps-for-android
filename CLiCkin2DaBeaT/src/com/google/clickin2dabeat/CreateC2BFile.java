/*
 * Copyright (C) 2008 Google Inc.
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
package com.google.clickin2dabeat;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Creates a skeleton C2B file when an appropriate media object is opened.
 */
public class CreateC2BFile extends Activity {
  private String dataSource;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    dataSource = this.getIntent().getData().toString();
    setContentView(R.layout.c2b_creator_form);

    Button create = ((Button) findViewById(R.id.CreateButton));
    create.setOnClickListener(new OnClickListener() {
      public void onClick(View arg0) {
        String title = ((EditText) findViewById(R.id.TitleEditText)).getText().toString();
        String author = ((EditText) findViewById(R.id.AuthorEditText)).getText().toString();
        if (!title.equals("") && !author.equals("")) {
          createC2BSkeleton(title, author, dataSource);
          finish();
        }
      }
    });

    Button cancel = ((Button) findViewById(R.id.CancelButton));
    cancel.setOnClickListener(new OnClickListener() {
      public void onClick(View arg0) {
        finish();
      }
    });

  }

  private void createC2BSkeleton(String title, String author, String media) {
    String c2bDirStr = "/sdcard/c2b/";
    String sanitizedTitle = title.replaceAll("'", " ");
    String sanitizedAuthor = author.replaceAll("'", " ");
    String filename = sanitizedTitle.replaceAll("[^a-zA-Z0-9,\\s]", "");
    filename = c2bDirStr + filename + ".c2b";
    String contents =
        "<c2b title='" + title + "' level='1' author='" + author + "' media='" + media + "'></c2b>";

    File c2bDir = new File(c2bDirStr);
    boolean directoryExists = c2bDir.isDirectory();
    if (!directoryExists) {
      c2bDir.mkdir();
    }

    try {
      FileWriter writer = new FileWriter(filename);
      writer.write(contents);
      writer.close();
      Toast.makeText(CreateC2BFile.this, getString(R.string.STAGE_CREATED), 5000).show();
    } catch (IOException e) {
      Toast.makeText(CreateC2BFile.this, getString(R.string.NEED_SD_CARD), 30000).show();
    }
  }



}
