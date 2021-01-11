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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import static java.nio.file.StandardOpenOption.*;
import java.util.Iterator;


/**
 *
 * @author 
 */
public  final class ClassPFs implements PFs {
    private final Class c; private final String relPth;

    private ClassPFs(Class c, String relPth) {
        this.c = c; 
        if(relPth==null)relPth = "";
        this.relPth = relPth;
    }

    public static final ClassPFs I(Class c){
        return new ClassPFs(c,"");
    }

    @Override public PFs r(String name) {
        return new ClassPFs(c, name+"/");
    }

    @Override
    public Iterator<String> iterator() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean isEmpty() {
        return false; // there is no way to check and also the class is here
        // so very likely that this path has that class atleast
    }

    @Override
    public boolean exists(String name) {
        try{
            InputStream is = c.getResource(relPth+name).openStream();
            is.close();
            return true;
        }catch(Exception a){
            return false;
        }
    }
    
    @Override public SeekableByteChannel p(String name, OpenOption... openOptions) throws IOException {        OpenOption[]whiteList = {READ};//create is ignored
        for (OpenOption openOption : openOptions) {
            boolean clean = false;
            INNER:
            for (OpenOption whiteOpt : whiteList) {
                if(openOption==whiteOpt){clean = true;break INNER;}
            }
            if(!clean)throw new IOException("Not supported "+openOption);
        }
        final String v = v(name);
        SeekableByteChannel_wrap sbc = new SeekableByteChannel_wrap(v);
        return sbc;
    }
    
    public final String v(String name)throws IOException{
        InputStream is = c.getResource(relPth+name).openStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String ret="",s;
        while((s=br.readLine())!=null){
            ret+=s;
        }
        return ret;
    }

    @Override
    public long size(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override public boolean isDirectory(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override public void close() throws IOException {}
    
}
