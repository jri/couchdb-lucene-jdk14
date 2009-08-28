package com.github.rnewson.couchdb.lucene;

/**
 * Copyright 2009 Robert Newson
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.HashMap;
import java.util.Map;

final class StopWatch {

    private Map elapsed = new HashMap();

    private long start = System.currentTimeMillis();

    public void lap(final String name) {
        final long now = System.currentTimeMillis();
        elapsed.put(name, new Long(now - start));
        start = now;
    }

    public long getElapsed(final String name) {
        return ((Long) elapsed.get(name)).longValue();
    }

}
