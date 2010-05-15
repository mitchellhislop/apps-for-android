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
package com.google.android.divideandconquer;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

/**
 * When the game starts, the user is welcomed with a message, and buttons for
 * starting a new game, or getting instructions about the game.
 */
public class WelcomeDialog extends Dialog implements View.OnClickListener {

    private final NewGameCallback mCallback;

    private View mNewGame;

    public WelcomeDialog(Context context, NewGameCallback callback) {
        super(context);
        mCallback = callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.app_name);

        setContentView(R.layout.welcome_dialog);

        mNewGame = findViewById(R.id.newGame);
        mNewGame.setOnClickListener(this);

    }

    /** {@inheritDoc} */
    public void onClick(View v) {
        if (v == mNewGame) {
            mCallback.onNewGame();
            dismiss();
        }
    }
}
