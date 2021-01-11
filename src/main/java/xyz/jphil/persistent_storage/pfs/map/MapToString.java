/*
 * Copyright 2019 Ivan Velikanova
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Ivan Velikanova
 */
public class MapToString {
    public static interface ObjectHandler {
        public String s(Object s,int intend);
    }
    public static final HashMap<Class,ObjectHandler> handlers = new HashMap<>();
    
    public static void register(Class type, ObjectHandler oh){
        handlers.put(type,oh);
    }
    
    public final static AtomicBoolean init = new AtomicBoolean(false);
    
    public static void init(){
        if(!init.compareAndSet(false, true)){
            MapToString.register(Set.class, (s,intend) -> {
                String ind = intend(intend);
                String ret="[\n";
                Set ds = (Set)s;
                for (Object d : ds) {
                    ret=ret+" "+ind+"\t"+renderedValue(d, intend+1)+",\n";
                }
                ret = ret+intend(intend-1)+"]\n";
                return ret;
            });
        }
    }
    
    public static String intend(int intendation){
        String suffix = "";
        for (int i = 0; i < intendation; i++) {
            suffix = suffix+"\t";
        }
        return suffix;
    }
    
    public static String c(Map p,int intendation){
        init();
        String ret = "{";
        String suffix = intend(intendation+1);
        for (Object key : p.keySet()) {
            Object v = p.get(key);
            
            ret=ret+suffix+" \""+key+"\" : "+renderedValue(v, intendation+1)+", \n";
        }
        ret=ret+intend(intendation-1)+"}";
        return ret;
    }
    
    public static String renderedValue(Object v,int intend){
        String renderedValue = null;
        if(v==null){
            renderedValue = null;
        }else if(v instanceof String){
            renderedValue = (String)v;
        }else {
            for (Map.Entry<Class, ObjectHandler> entry : handlers.entrySet()) {
                if( entry.getKey().isInstance(v) ){ 
                    renderedValue = entry.getValue().s(v, intend+1);
                    break;
                }
            }
            if(renderedValue==null){
                 renderedValue = v.toString();
            }
        }
        return renderedValue;
    }
    
}
