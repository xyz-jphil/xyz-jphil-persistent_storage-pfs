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
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author 
 */
public class FakePFs implements PFs {
    private final String name;
    private final FakePFs parent;

    public FakePFs() {
        name = null; parent = null;
    }
    
    public FakePFs(String name, FakePFs parent) {
        this.name = name;
        this.parent = parent;
    }
    
    @Override
    public PFs r(String name) {
        return new FakePFs(name, this);
    }

    @Override
    public SeekableByteChannel p(String name, OpenOption... openOptions) throws IOException {
        throw (UnsupportedOperationException)(new FakePFsException());
    }
    
    
    @Override
    public long size(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isDirectory(String name) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean exists(String name) {
        return false;
    }
    
    @Override public void close() throws IOException {}

    @Override
    public Iterator<String> iterator() {
        return new LinkedList<String>().iterator();
    }
}
