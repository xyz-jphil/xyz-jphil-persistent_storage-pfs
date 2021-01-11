/*
 * Copyright 2018 .
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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author 
 */
public class PFsS<E> implements Iterable<Map.Entry<String,E>> {
    private final PFs r;
    private final Class<E> clz;

    public PFsS(PFs r,Class<E> clz) {
        this.r = r; this.clz = clz;
    }

    @Override
    public Iterator<Map.Entry<String,E>> iterator() {
        Iterable<Path> it = null;
        try{
            it = Files.newDirectoryStream(((PathPFs)r).getPath(), new DirectoryStream.Filter<Path>() {
                @Override public boolean accept(Path entry) throws IOException {
                    return (Files.isDirectory(entry));
                }
            });
        }catch(Exception a){
            throw new RuntimeException(a);
        }
        Iterator<Path> it2 = it.iterator();
        
        return new Iterator() {
            @Override
            public boolean hasNext() {
                return it2.hasNext();
            }

            @Override
            public Map.Entry<String,E> next() {
                Path p = it2.next();
                if(p==null) throw new NullPointerException("Empty");
                PFs r = PFile.create(p);
                E v = PFile.I(r, clz);
                Map.Entry<String,E> entry = new Map.Entry<String, E>() {
                    @Override public String getKey() {
                        return p.getFileName().toString();
                    }
                    @Override public E getValue() {
                        return v;
                    }
                    @Override public E setValue(E value) {
                        throw new UnsupportedOperationException();
                    }
                };
                return entry;
            }
        };
    }
    
    
    
}
