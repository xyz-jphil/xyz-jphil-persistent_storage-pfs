/*
 * Copyright 2015  .
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

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import xyz.jphil.persistent_storage.pfs.PFs;
import xyz.jphil.persistent_storage.pfs.SeekableByteChannel_wrap;

/**
 *
 * @author 
 */
public class MapPFs implements PFs{
    private final Map m;
    private final MapFactory mf;

    public MapPFs(Map m,MapFactory factory) {
        this.m = m; mf = factory;
    }
    
    public static MapPFs wrap(Map m){
        return new MapPFs(m, null);
    }
    
    @Override
    public long size(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean exists(String name) {
        Object s = m.get(name);
        return s!=null;
    }
    
    @Override
    public boolean isEmpty() {
        return m.isEmpty();
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            private final Iterator<Path> ix = m.values().iterator();
            @Override
            public boolean hasNext() {
                return ix.hasNext();
            }

            @Override
            public void remove() {
                ix.remove();
            }
            
            @Override
            public String next() {
                return ix.next().toString();
            }
        };
    }
    
    public static MapPFs wrap(Map m,MapFactory mf1){
        return new MapPFs(m, mf1);
    }
    
    public Object get(String name){
        return m.get(name);
    }
    
    public Object put(String name,Object v){
        return m.put(name,v);
    }

    public Map getMap() {
        return m;
    }
    
    @Override public PFs r(String name) {
        Object o  = m.get(name);
        if(o instanceof Map){
            return new MapPFs((Map)o,mf);
        }
        if(mf==null){
            throw new UnsupportedOperationException("Cannot create directory."
                    + "No map factory provided for internal map creation.");
        }
        Map m2 = mf.make();
        m.put(name, m2);
        return new MapPFs(m2,mf);
    }

    @Override public SeekableByteChannel p(String name, OpenOption... openOptions) throws IOException {
        Object o  = m.get(name);
        if(o == null)return null;
        SeekableByteChannel_wrap sbc = new SeekableByteChannel_wrap(o);
        return sbc;
    }

    @Override public boolean isDirectory(String name) {
        Object o  = m.get(name);
        if(o==null)return false;
        return o instanceof Map;
    }

    @Override public void close() throws IOException {}
    
    public interface MapFactory {
        Map make();
    }
    
}
