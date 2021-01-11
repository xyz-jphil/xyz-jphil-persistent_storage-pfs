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

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author 
 */
final class PFsImpl implements PathPFs {
    
    final Path p;
    private final ChannelList resources = new ChannelList();

    public PFsImpl(Path p) {
        this.p = p;
    }

    @Override
    public Path getPath() {
        return p;
    }    
    
    @Override
    public long size(String name) {
        try{
            return Files.size(p.resolve(name));
        }catch(Exception a){
            return -1;
        }
    }
    
    @Override public boolean isDirectory(String name) {
        Path relpth = p.resolve(name);
        boolean  ret = Files.isDirectory(relpth);
        return ret;
    }

    @Override
    public boolean exists(String name) {
        try{
            boolean exists = Files.exists(p.resolve(name));
            if(!exists)return false;
            return Files.size(p.resolve(name)) > 0;
        }catch(Exception a){
            
        }return false;
    }

    @Override
    public Iterator<String> iterator() {
        try (final DirectoryStream<Path> d = Files.newDirectoryStream(p)) {
            return new Iterator<String>() {
                private final Iterator<Path> ix = d.iterator();
                @Override public boolean hasNext() {
                    try {
                        return ix.hasNext();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }return false;
                }
                @Override public String next() {
                    try{
                        return ix.next().toString(); 
                    }catch(Exception a){
                        a.printStackTrace();
                    }return null;
                }
                @Override public void remove() {
                    ix.remove();
                }
            };
        }finally {
            return new java.util.LinkedList<String>().iterator();
        }
    }

    @Override
    public boolean isEmpty() {
        boolean rs = false;
        try (DirectoryStream d = Files.newDirectoryStream(p)) {
            rs = d.iterator().hasNext();
        }catch (IOException ex) {
            Logger.getLogger(PFsImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return rs;
    }
    
    @Override
    public PFs r(String name) {
        if(name.contains(File.separator)){
            throw new IllegalArgumentException("name should be simple name, "
                    + "not a relative path. For name give="+name);
        }
        return new PFsImpl(p.resolve(name).toAbsolutePath());
    }

    @Override
    public SeekableByteChannel p(String name, OpenOption... openOptions)
            throws IOException {
        if(!Files.exists(p)){
            try{Files.createDirectories(p);}catch(Exception a){a.printStackTrace();}
        }
        return resources.addAndGet(FileChannel.open(p.resolve(name), openOptions));
    }

    @Override
    public void close() throws IOException {
        synchronized (resources){
            resources.close();
        }
    }

    @Override
    public String toString() {
        return super.toString()+"{"+p.toString()+ "}";
    }
    
}
