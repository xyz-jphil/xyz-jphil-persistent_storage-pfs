/*
 * Copyright 2015   .
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

package xyz.jphil.persistent_storage.pfs;

import java.util.HashMap;

/**
 *
 * @author   
 */
public class SyncedMapImpl<E> implements /*Map<String,V>,*/ SyncMap<E>{
    private final PFs r;
    private final Class<E> template;
    
    SyncedMapImpl(PFs r,Class<E> template) {
        this.r = r; this.template = template;
    }
    
    private final HashMap<String,E> hm = new HashMap<>();

    @Override public boolean isEmpty() {
        synchronized (hm){
            boolean x = hm.isEmpty();
            if(x)return true;
            return r.isEmpty();
        }
    }
    
    private String asStr(Object key){
        String s;
        if(key instanceof String)s=(String)key;
        else s = key.toString();
        return s;
    }
    
    @Override public E get(String key) {
        synchronized (hm){
            if(key==null)return null;//String s = asStr(key);

            E e = hm.get(key);
            if(e!=null)return e;

            e = PFile.I(r.r(key), template);
            hm.put(key, e);
            return e;
        }
    }
    @Override public boolean containsKey(String key,NullChecker<E> nc) {
        synchronized (hm){
            if(key==null)return false;//String s = asStr(key);

            boolean x = hm.containsKey(key);
            if(x)return true;

            E e = PFile.I(r.r(key), template);
            hm.put(key, e);
            return !nc.isNull(e);
        }
    }
    @Override public E put(String key, E value) {
        synchronized (hm){
            E x = hm.put(key, value);
            try{
                x = PFile.put(r.r(key), template,value);
                //PFile.set(r, key, value);
            }
            catch(Exception a){
                a.printStackTrace();
            }
            x = hm.put(key, x);
            return x;
        }
    }

    /*@Override public int size() {throw new UnsupportedOperationException();}
    @Override public void putAll(Map<? extends String, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override public Set<String> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override public Set<Entry<String, V>> entrySet() {
        throw new UnsupportedOperationException();
    }*/
    
}
