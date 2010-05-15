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

package com.google.android.opengles.triangle;

import javax.microedition.khronos.opengles.GL;

import android.app.Activity;
import android.opengl.GLDebugHelper;
import android.os.Bundle;

public class TriangleActivity extends Activity {

    /** Set to true to enable checking of the OpenGL error code after every OpenGL call. Set to
     * false for faster code.
     *
     */
    private final static boolean DEBUG_CHECK_GL_ERROR = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mGLView = (GLView) findViewById(R.id.glview);

        if (DEBUG_CHECK_GL_ERROR) {
            mGLView.setGLWrapper(new GLView.GLWrapper() {
                public GL wrap(GL gl) {
                    return GLDebugHelper.wrap(gl, GLDebugHelper.CONFIG_CHECK_GL_ERROR, null);
                }});
        }
        mGLView.setRenderer(new TriangleRenderer(this));
        mGLView.requestFocus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
    }

    private GLView mGLView;
}
