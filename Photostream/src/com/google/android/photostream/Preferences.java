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


package com.google.android.photostream;

final class Preferences {
    static final String NAME = "Photostream";

    static final String KEY_ALARM_SCHEDULED = "photostream.scheduled";
    static final String KEY_ENABLE_NOTIFICATIONS = "photostream.enable-notifications";
    static final String KEY_VIBRATE = "photostream.vibrate";
    static final String KEY_RINGTONE = "photostream.ringtone";

    Preferences() {
    }
}
