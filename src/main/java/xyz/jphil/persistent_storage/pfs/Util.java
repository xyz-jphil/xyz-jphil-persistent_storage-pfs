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

package xyz.jphil.persistent_storage.pfs;

import java.lang.reflect.InvocationTargetException;
import xyz.jphil.persistent_storage.pfs.map.MapPFs;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author 
 */
public class Util {
    public static boolean isGetter(Method method,Object[] args){
        Class retType = method.getReturnType();
        if(args==null || args.length==0){
            return (retType != Void.TYPE);
        }return false;
    }
    
    public static boolean isSetter(Method method,Object... args){
        UnsupportedOperationException ns = new UnsupportedOperationException("Not supported yet.");
        Class retType = method.getReturnType();
        if(args.length != method.getParameterTypes().length)
            throw new IllegalStateException("Params don't match");
        if(args.length>1)throw ns;
            
        return (retType == Void.TYPE);
        //return true;
    }
    
    public static Map<String,Object> toMap(Object o,Class template){
        Method[]m = template.getDeclaredMethods();
        HashMap<String,Object> cache = new HashMap<>();
        for (Method method : m) {
            if (method.isDefault()) continue; // do not touch default methods
            if(Util.isGetter(method, null)){
                try{
                    Object result = method.invoke(o);
                    cache.put(method.getName(),result);
                }catch(IllegalAccessException | InvocationTargetException iae){
                    cache.put(method.getName(),null);
                }
            }
        }
        return cache;
    }
    
    public static <I> ObjectFactory<I> newObject(Class<I> template){
        return new ObjectFactory<>(template);
    }
    
    public static final class ObjectFactory<I>  {
        private final Class<I> template;
        private final HashMap<String,Object> m = new HashMap<>();
        public ObjectFactory(Class<I> template) {
            this.template = template;
        }
        
        public ObjectFactory put(String name,Object value){
            m.put(name, value);
            return this;
        }
        
        public I build(){
            return PFile.I(MapPFs.wrap(m), template);
        }
        
    }
    
}
