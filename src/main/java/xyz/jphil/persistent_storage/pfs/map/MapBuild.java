/*
 * Copyright 2019 Ivan Velikanova.
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
package xyz.jphil.persistent_storage.pfs.map;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Ivan Velikanova
 */
public class MapBuild<S> {
    public static final <S> MapBuild<S> New(){
        return new MapBuild<S>();
    }
    private final HashMap<S,Object> map = new HashMap<>();

    public MapBuild put(S key, Object value) {
        map.put(key, value); return this;
    }

    public MapBuild putAll(Map<S, Object> status) {
        map. putAll(status);
        return this;
    }

    public Map<S,Object> buildMap(){
        return Collections.unmodifiableMap(map);
    }
            
}