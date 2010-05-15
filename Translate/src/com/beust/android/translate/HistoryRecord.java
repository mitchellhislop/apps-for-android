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

package com.beust.android.translate;

import static com.beust.android.translate.Languages.Language;

/**
 * This class describes one entry in the history
 */
public class HistoryRecord {
    private static final String SEPARATOR = "@";

    public Language from;
    public Language to;
    public String input;
    public String output;
    public long when;

    public HistoryRecord(Language from, Language to, String input, String output, long when) {
        super();
        this.from = from;
        this.to = to;
        this.input = input;
        this.output = output;
        this.when = when;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        try {
            HistoryRecord other = (HistoryRecord) o;
            return other.from.equals(from) && other.to.equals(to) &&
                    other.input.equals(input) && other.output.equals(output);
        } catch(Exception ex) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return from.hashCode() ^ to.hashCode() ^ input.hashCode() ^ output.hashCode();
    }

    public String encode() {
        return from.name() + SEPARATOR + to.name() + SEPARATOR + input
                + SEPARATOR + output + SEPARATOR + new Long(when);
    }
    
    @Override
    public String toString() {
        return encode();
    }
    
    public static HistoryRecord decode(String s) {
        Object[] o = s.split(SEPARATOR);
        int i = 0;
        Language from = Language.valueOf((String) o[i++]);
        Language to = Language.valueOf((String) o[i++]);
        String input = (String) o[i++];
        String output = (String) o[i++];
        Long when = Long.valueOf((String) o[i++]);
        HistoryRecord result = new HistoryRecord(from, to, input, output, when);
        return result;
    }
    
}

