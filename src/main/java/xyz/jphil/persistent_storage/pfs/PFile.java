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

import xyz.jphil.persistent_storage.pfs.map.MapPFs;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import xyz.jphil.persistent_storage.pfs.type.TypeImplementor;
import xyz.jphil.persistent_storage.pfs.type.TypeHandlerProvider;
import java.lang.reflect.Proxy;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import static java.nio.file.StandardOpenOption.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import xyz.jphil.persistent_storage.pfs.type.DefaultTypeHandlerProvider;

/**
 *
 * @author 
 */
public final class PFile {
    private TypeHandlerProvider thp = new DefaultTypeHandlerProvider();
    private PFs r;private boolean cache;

    private PFile() {}
    
    public static PathPFs createInUserHome(String name){
        Path base = Paths.get(System.getProperty("user.home")).resolve(name);
        return new PFsImpl((Path)base);
    }
    
    public static PFs create(Object base){
        if(base instanceof Path){
            return new PFsImpl((Path)base);
        }if(base instanceof Class){
            return ClassPFs.I((Class)base);
        }if(base instanceof Map){
            return new MapPFs((Map)base,null);
        }
        throw new UnsupportedOperationException("Cannot extract rus out of "+base);
    }
    
    public PFile r(PFs s){
        this.r = s;
        return this;
    }
    
    public PFile thp(TypeHandlerProvider thp){
        this.thp = thp;
        return this;
    }
    
    public TypeHandlerProvider thp(){
        return thp;
    }
    
    public static PFile newInstance(){
        return new PFile();
    }
    
    public PFile cache(boolean cache){
        this.cache = cache;
        return this;
    }
    
    public static <E> SyncMap<E> s(PFs r,Class<E> template){
        return new SyncedMapImpl(r,template);
    }
    
    public <E> E I(Class<E>interfaceDefinition){
        if(!interfaceDefinition.isInterface()){
            throw new IllegalStateException("Only interfaces supported " + interfaceDefinition+ r.toString());
        }
        return (E)Proxy.newProxyInstance(PFile.class.getClassLoader(), new Class[]{interfaceDefinition}, 
                new TypeImplementor(r,thp));
    }
    
    public static <E> E I(PFs r,Class<E>interfaceDefinition){
        return I(r, interfaceDefinition, new DefaultTypeHandlerProvider());
    }
    
    public static <E> E defaultValues(Class<E>interfaceDefinition){
        return I(new FakePFs(), interfaceDefinition);
    }
    
    public static <E> E put(PFs r,Class<E>interfaceDefinition,E value){
        Copier.overwrite(r, interfaceDefinition, value, new DefaultTypeHandlerProvider(),null);
        E e = I(r, interfaceDefinition);
        return e;
    }
    
    public static <E> E cast(Map m, Class<E>interfaceDefinition){
        return I(new MapPFs(m, null), interfaceDefinition);
    }
    
    public static void copy(PFs src, PFs dest,Class interfaceDefinition){
        Copier.copy(src, dest, interfaceDefinition);
    }
    
    public static <E> E I(PFs r,Class<E>interfaceDefinition,TypeHandlerProvider thp){
        return I(r, interfaceDefinition, thp, true);
    }
    
    public static <E> E I(PFs r,Class<E>interfaceDefinition,TypeHandlerProvider thp,boolean cachingEnabled){
        if(!interfaceDefinition.isInterface()){
            throw new IllegalStateException("Only interfaces supported " + interfaceDefinition);
        }
        if(r instanceof MapPFs){cachingEnabled = false;}
        return (E)Proxy.newProxyInstance(PFile.class.getClassLoader(), new Class[]{interfaceDefinition}, 
                new TypeImplementor(r,thp,cachingEnabled));
    }
    
    public static Map<String,V> getMap(PFs r){
        HashMap<String,V> hm = new HashMap();
        try{
            Path p = ((PFsImpl)r).p;
            DirectoryStream<Path> ds = Files.newDirectoryStream(p);
            for(Path pth : ds){
                if(Files.isDirectory(pth))continue;
                String n = pth.getFileName().toString();
                hm.put(n, get(r, n));
            }
        }catch(Exception a){
            a.printStackTrace();
        }
        return hm;
    }
    
    public static List<String> getStrings(PFs r,String nm){
        byte[]b = getByteArray(r, nm);
        String s = new String(b,UTF8);
        String[]ss = s.split("[\\r\\n]+");
        return Arrays.asList(ss);
    }
    
    public static byte[]getByteArray(PFs r,String nm){
        if(!r.exists(nm))return new byte[0];
        byte[]b = new byte[(int)(r.size(nm))];
        ByteBuffer bb = ByteBuffer.wrap(b);
        try(SeekableByteChannel sbc = r.p(nm,READ)){
            int read = sbc.read(bb);
            if(read < b.length){
                throw new IllegalStateException("could not read whole");
            }
            sbc.close();
        }catch(Exception a){
            return new byte[0];
        }
        return b;
    }
    
    public static Println println(PFs r,String n){
        Path p = ((PFsImpl)r).p;
        if(!Files.exists(p)){
            try{Files.createDirectories(p);}catch(Exception a){a.printStackTrace();}
        }
        p = p.resolve(n);
        Println p1=null;
        try{
            final PrintWriter pw = new PrintWriter(p.toFile());
            p1 = new Println() {
                @Override public void println() {pw.println();}
                @Override public void println(String s) { pw.println(s);}
                @Override public void flush() { pw.flush(); }
                @Override public void close() { pw.close(); }
            };
        }catch(Exception a){
            p1 = new Println() {
                private final UnsupportedOperationException ex = new UnsupportedOperationException("Not supported yet.");
                @Override public void println() { throw ex; }
                @Override public void println(String s) { throw ex; }
                @Override public void flush() {throw ex; }
                @Override public void close() { throw ex; }
            };
        }
        return p1;
    }
    
    public static V[] getArray(PFs r){
        List<V> l = getList(r);
        return l.toArray(new V[l.size()]);
    }
    
    public static RIterable i(final PFs r,final DefaultValue dv,final TypeHandlerProvider thp){
        return new RIterable(r, dv, thp);
    }
    
    public static List<V> getList(PFs r){
        List<V> l = new LinkedList<>();
        try{
            Path p = ((PFsImpl)r).p;
            
            DirectoryStream<Path> ds = Files.newDirectoryStream(p);
            for(Path pth : ds){
                if(Files.isDirectory(pth))continue;
                String n = pth.getFileName().toString();
                l.add(get(r, n));
            }
        }catch(Exception a){
            a.printStackTrace();
        }
        return Collections.unmodifiableList(l);
    }
    
    public static Stream<V> getAsStream(PFs r,String n)throws IOException{
        Path p = ((PFsImpl)r).p;

        if(!Files.exists(p)){
            try{Files.createDirectories(p);}catch(Exception a){a.printStackTrace();}
        }
        
        p = p.resolve(n);
        return Files.lines(p).map( (String sx)->{ return new VImpl(sx); } );
    }
    
    public static List<String> getAsStringList(PFs r,String n)throws IOException{
        Path p = ((PFsImpl)r).p;
        
        if(!Files.exists(p)){
            try{Files.createDirectories(p);}catch(Exception a){a.printStackTrace();}
        }
        
        p = p.resolve(n);
        if(!Files.exists(p)){
            return Collections.EMPTY_LIST;
        }
        return Files.readAllLines(p);
    }
    
    public static final byte[]systemNewLine=System.lineSeparator().getBytes();
    public static final byte[]nixNewLine={(byte)'\n'};
    public static final Charset UTF8 = java.nio.charset.StandardCharsets.UTF_8;
    
    public static ByteBuffer systemNewLine(){
        return ByteBuffer.wrap(systemNewLine);
    }
    
    public static V get(PFs r,String n){
        return get(r, n, -1); //4*1024
    }
        
    public static V get(PFs r,String n, int maxDataSize){
        try{
            return new VImpl(getVStr(r, n, maxDataSize));
        }catch(java.nio.file.NoSuchFileException| FakePFsException nsfe){
            // ignore
        }catch(Exception a){
            a.printStackTrace();
        }
        return new VImpl(null);
    }
    
    public static V v(Object o){
        return new VObj(o);
    }
    
    public static int writeln(SeekableByteChannel sbc, String s)throws IOException{
        int r;
        r = sbc.write(ByteBuffer.wrap(s.getBytes()));
        r = r + sbc.write(systemNewLine());
        force(sbc, false);
        return r;
    }
    
    public static void force(SeekableByteChannel sbc, boolean metaData)throws IOException{
        try{
            ((SBCRef)sbc).force(metaData);
        }catch(Exception a){
            throw new IOException(a);
        }
    }
    
    
    private static String getVStr(PFs r,String n, int maxDataSize)throws IOException{
        String v;
        if(r instanceof ClassPFs){
            return ((ClassPFs)r).v(n);
        }else {
            SeekableByteChannel dp = r.p(n, StandardOpenOption.READ,StandardOpenOption.CREATE);
            if(maxDataSize < 0) {
                v = getVStrF(dp);
            }else{
                ByteBuffer bb=ByteBuffer.allocate(Math.min((int)dp.size(),maxDataSize));
                dp.read(bb);
                v = new String(bb.array(),Charset.forName("UTF-8"));
            }
            try{dp.close();}catch(Exception a){a.printStackTrace();}
        }
        return v;
    }
    
    private static String getVStrF(SeekableByteChannel sbc)throws IOException{
        StringBuilder sb = new StringBuilder();
        ByteBuffer bf = ByteBuffer.allocate(4*1024);
        int i = 0;
        while ((i = sbc.read(bf)) > 0) {
            ((Buffer)bf).flip();
            sb.append(Charset.forName("UTF-8").decode(bf));
            ((Buffer)bf).clear();
        } return sb.toString();
    }
    
    public static void set(PFs r,String n,V v)throws Exception{
        SeekableByteChannel dp = r.p(
                        n, 
                        StandardOpenOption.WRITE,StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
        dp.write(ByteBuffer.wrap(v.raw()));
        try{dp.close();}catch(Exception a){a.printStackTrace();}
    }
    
    public static <T> void setJavaArrayWithNewlineOrPure(PFs r,String n,T[]v)throws Exception{
        TypeImplementor.handleSettersArrays(r, n, v, v.getClass());
    }
    
    public static void setAsPureJavaObject(PFs r,String n,Object v)throws Exception{
        //TypeImplementor.handleSettersArrays(r, n, v, inputParamType);
        try( SeekableByteChannel sbc = r.p(n, WRITE,CREATE,TRUNCATE_EXISTING)  ){
            ByteBuffer bb=SeekableByteChannel_wrap.toByteBuffer(v);
            int x = sbc.write(bb);
            sbc.close();
            if(x < bb.capacity()){
                throw new IllegalStateException("Entire content was not written. "
                        + "Written="+x+" expected="+bb.capacity() );
            }
        }catch(Exception a){
            throw a;
        }
    }
    
    public static <T> T[] getAsJavaArrayNewLine(PFs r,String n,Class<T[]> retType){
        return (T[])TypeImplementor.handleGettersArrays(n, retType, null,r);
    }
    
    public static Object getAsPureJavaObject(PFs r,String n){
        try (SeekableByteChannel sbc = r.p(n)) {
            InputStream is = SeekableByteChannel_wrap.sbcToIn(sbc);
            return SeekableByteChannel_wrap.fromInputStream(is);
        }catch(Exception a){
            a.printStackTrace();
            return null;
        }
    }
    
    public static void set(PFs r,String n,Object v)throws Exception{
        try(SeekableByteChannel dp = r.p(
                        n, 
                        StandardOpenOption.WRITE,StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING)){
            ByteBuffer bb;
            if(v instanceof Integer||v instanceof Long||v instanceof Double||
                v instanceof Float||v instanceof Boolean||v instanceof Character){
                /*explicitly specifying charset to avoid localization*/
                bb=ByteBuffer.wrap(v.toString().getBytes(UTF8));
            }else if(v instanceof String){
                bb=ByteBuffer.wrap(((String)v).getBytes(UTF8));
            }else {
                bb=SeekableByteChannel_wrap.toByteBuffer(v);
            }
            int c = dp.write(bb);
            if(c!=bb.capacity()){
                throw new RuntimeException("written c="+c+" should have been "
                    +bb.capacity());
            }
            dp.close();
        }catch(Exception a){a.printStackTrace();}
    }
    
    
    public static <E> Iterable<Map.Entry<String,E>> iterable(PFs r,Class<E> clz){
        PFsS<E> r2 = new PFsS(r, clz);
        return r2;
    }
    
    public static boolean isWhiteList(Class x){
        return checkContains(x, whiteList);
    }
    
    public static boolean isWhiteListAr(Class x){
        return checkContains(x, whiteListAr);
    }
    
    private static boolean checkContains(Class x, Class[]s){
        if(x==null)return false;
        for (Class cz : s) {
            if(cz == x || cz.equals(x) || x.isAssignableFrom(cz) ){
                return true;
            }
        }
        return false;
    }
    
    static final Class[] whiteList = 
        new Class[]{int.class,long.class,double.class,
            float.class,char.class,boolean.class,String.class};
    
    static final Class[] whiteListAr = 
        new Class[]{int[].class,long[].class,double[].class,
            float[].class,char[].class,boolean[].class,String[].class};
}
