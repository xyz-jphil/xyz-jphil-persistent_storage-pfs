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

package xyz.jphil.persistent_storage.pfs.type;

import java.util.Iterator;

import xyz.jphil.persistent_storage.pfs.DefaultValue;
import xyz.jphil.persistent_storage.pfs.PFs;
import xyz.jphil.persistent_storage.pfs.PFile;

/**
 *
 * @author 
 */
public class IterableHandler implements TypeHandler{
    private final TypeHandlerProvider thp;

    public IterableHandler(TypeHandlerProvider thp) {
        this.thp = thp;
    }
    
    @Override public Class type() {
        return Iterable.class;
    }

    @Override public Object handle(PFs r,DefaultValue dv) {
        return PFile.i(r,dv,thp);
    }

    @Override
    public Object put(PFs r, Object value,DefaultValue dv) {
        if(!(value instanceof Iterable)){
            throw new IllegalStateException(value+" type not "+Iterable.class);
        }
        
        Iterable i = (Iterable)value;
        Iterator it = i.iterator();
        
        Class subeletype = dv==null?String.class:dv.subElementType();
        
        
        if(subeletype.isInstance(value)){
            throw new IllegalStateException("Weird element type "+value+ " "+subeletype);
        }
        
        while(it.hasNext()){
            Object n = it.next();
            boolean whitelist = PFile.isWhiteList
                    (subeletype);
            if(whitelist){
                try{
                    PFile.set(r, 
                            String.valueOf(n.hashCode()),
                            n);
                }catch(Exception a){
                    a.printStackTrace();
                }
                return value;
            }
            PFile.put(r, subeletype, value);
        }
        
        return handle(r, null);
    }    
}
