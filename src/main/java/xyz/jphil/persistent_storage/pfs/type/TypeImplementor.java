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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import static java.nio.file.StandardOpenOption.WRITE;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import xyz.jphil.persistent_storage.pfs.DefaultValue;
import xyz.jphil.persistent_storage.pfs.map.MapPFs;
import xyz.jphil.persistent_storage.pfs.PFs;
import xyz.jphil.persistent_storage.pfs.PFile;
import static xyz.jphil.persistent_storage.pfs.PFile.UTF8;
import xyz.jphil.persistent_storage.pfs.SeekableByteChannel_wrap;
import xyz.jphil.persistent_storage.pfs.Util;
import xyz.jphil.persistent_storage.pfs.V;
import xyz.jphil.persistent_storage.pfs.VObj;

/**
 *
 * @author 
 */
public final class TypeImplementor<E> implements InvocationHandler{
    private final PFs r; private final TypeHandlerProvider thp;
    private final boolean cachingEnabled;
    private final HashMap cache = new HashMap();//reading each time from disk 
    // is more expensive, un-necessary and weird HOWEVER
    // caching introduces clear possibility of not having the latest value
    // if two separate objects of this are being used in different thread 
    // at the same time.

    public TypeImplementor(PFs r, TypeHandlerProvider thp) {
        this(r, thp, true);
    }
    public TypeImplementor(PFs r, TypeHandlerProvider thp,boolean cachingEnabled) {
        this.r = r; 
        this.cachingEnabled = cachingEnabled; 
        this.thp = thp;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        /*Constructor<MethodHandles.Lookup> constructor;
        Class<?> declaringClass;*/
        

        if (method.isDefault()) {
            /*Object result;
            declaringClass = method.getDeclaringClass();
            constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class,int.class);
            constructor.setAccessible(true);
            result = constructor
                .newInstance(declaringClass)
                //.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE)
                //.newInstance(declaringClass, MethodHandles.Lookup.PUBLIC)
                .in(declaringClass)
                .unreflectSpecial(method, declaringClass)
                .bindTo(proxy)
                .invokeWithArguments(args);
            return(result);*/ 
            throw new IllegalStateException("Please do not use PFs on default methods. As of now the API for jdk is highly discouraging, complex and inconsistent todo such things. Find some other way todo what you are doing.");
        }
        
        if(method.getDeclaringClass().getName().equals("java.lang.Object")){
            return method.invoke(r, args);
        }
        
        /*if(method.getName().equals("equals")){
            return r.equals(args!=null?args[0]:null);
        }else if(method.getName().equals("hashCode")){
            return r.hashCode();
        }*/
        
        if(!cachingEnabled)return invoke2(proxy, method, args);
        Class retType = method.getReturnType();
        synchronized (cache){
            if(Util.isGetter(method, args)){
                Object result = cache.get(method.getName());
                if(result==null){
                    result = handleGettters(proxy, method, retType);
                    cache.put(method.getName(),result);
                }return result;
            }
            if(Util.isSetter(method, args)){
                handleSettters(proxy, method, args[0]);
                cache.put(method.getName(),args[0]);
            }
        }
        return null;
    }
    
    private Object invoke2(Object proxy, Method method, Object[] args) throws Throwable {
        Class retType = method.getReturnType();
        if(Util.isGetter(method, args)){
            return handleGettters(proxy, method, retType);
        }
        if(Util.isSetter(method, args)){
            handleSettters(proxy, method, args[0]);
        }return null;
    }
    
    private Object handleGettters(Object proxy, Method m,Class retType){
        DefaultValue dv = (DefaultValue)m.getAnnotation(DefaultValue.class);
        
        if(r.isDirectory(m.getName())){
            TypeHandler th = thp.provideFor(retType);
            if(th!=null)return th.handle(r.r(m.getName()), dv);
            else {
                PFile r1 = PFile.newInstance();
                r1.r(r.r(m.getName()));
                r1.thp(thp);
                return r1.I(m.getReturnType());
            }
        }
        V v;
        if(r instanceof MapPFs){
            Object val = ((MapPFs)r).get(m.getName());
            v= new VObj(val);
        }else{
            v = PFile.get(r, m.getName(),dv==null?4*1024:dv.maximumDataSize());
        }
        
        if(retType.isAssignableFrom(Double.TYPE)){
            return v.d(dv==null?0d:dv.d());
        }else if(retType.isAssignableFrom(Integer.TYPE)){
            return v.i(dv==null?0:dv.i());
        }else if(retType.isAssignableFrom(Long.TYPE)){
            return v.l(dv==null?0:dv.l());
        }else if(retType.isAssignableFrom(Boolean.TYPE)){
            return v.b(dv==null?false:dv.b());
        }else if(retType.isAssignableFrom(String.class)){
            return v.s(dv==null?"":dv.s());
        }else if(retType.isArray()){
            try (SeekableByteChannel sbc = r.p(m.getName())) {
                InputStream is = SeekableByteChannel_wrap.sbcToIn(sbc);
                return SeekableByteChannel_wrap.fromInputStream(is);
            }catch(Exception a){
                return null;
            }
            //return handleGettersArrays(proxy, m, retType,dv,r);
        }
        ValueHandler vh = thp.provideFor(retType,dv);
        String tempValue; String dval = dv==null?"":dv.s();
        if(v==null){
            return null;
        }
        tempValue = v.s(dval);
        return vh.handle(tempValue, r, m.getName(), dv);
    }
    
    public static Object handleGettersArrays( /*Object proxy,*/ String name,Class retType,
            DefaultValue dv,PFs r){    
        if(retType.isAssignableFrom(double[].class)){
            List<String> a = getAsStringList(r, name);
            if(a.isEmpty())return dv==null?new double[0]:dv.da();
            double[]array = (double[]) Array.newInstance(double.class, a.size());
            int i=0; for (String l : a) {
                array[i] = Double.parseDouble(l); i++;
            }
            return array;
        }else if(retType.isAssignableFrom(int[].class)){
            List<String> a = getAsStringList(r, name);
            if(a.isEmpty())return dv==null?new int[0]:dv.ia();
            int[]array = (int[]) Array.newInstance(int.class, a.size());
            int i=0; for (String l : a) {
                array[i] = Integer.parseInt(l); i++;
            }
            return array;
        }else if(retType.isAssignableFrom(long[].class)){
            List<String> a = getAsStringList(r, name);
            if(a.isEmpty())return dv==null?new long[0]:dv.la();
            long[]array = (long[]) Array.newInstance(long.class, a.size());
            int i=0; for (String l : a) {
                array[i] = Long.parseLong(l); i++;
            }
            return array;
        }else if(retType.isAssignableFrom(boolean[].class)){
            List<String> a = getAsStringList(r, name);
            if(a.isEmpty())return dv==null?new boolean[0]:dv.ba();
            boolean[]array = (boolean []) Array.newInstance(boolean.class, a.size());
            int i=0; for (String l : a) {
                array[i] = Boolean.parseBoolean(l); i++;
            }
            return array;
        }else if(retType.isAssignableFrom(String[].class)){
            List<String> a = getAsStringList(r, name);
            if(a.isEmpty())return dv==null?new String[0]:dv.sa();
            return a.toArray(new String[a.size()]);
        }else {
            try (SeekableByteChannel sbc = r.p(name)) {
                InputStream is = SeekableByteChannel_wrap.sbcToIn(sbc);
                return SeekableByteChannel_wrap.fromInputStream(is);
            }catch(Exception a){
                return null;
            }
        }
    }
    
    public static List<String> getAsStringList(PFs r,String name){
        try (SeekableByteChannel sbc = r.p(name)) {
            InputStream in = SeekableByteChannel_wrap.sbcToIn(sbc);
            BufferedReader br = new BufferedReader(new InputStreamReader(in,UTF8));

            LinkedList<String> a = new LinkedList<>();
            String line;
            while((line=br.readLine())!=null){
                a.add(line);
            } return a;
        }catch(Exception a){
            a.printStackTrace();
        }
        return Collections.EMPTY_LIST;
    }
    
    
    
    static void handleSettersArrays(Object proxy, Method m,Object arg,PFs r){
        Class inputParamType = m.getParameters()[0].getType();
        try{
            PFile.setAsPureJavaObject(r, m.getName(), arg);
        }catch(Exception a){
            a.printStackTrace();
        }
        //handleSettersArrays(r, m.getName(), arg, inputParamType);
    }
    
    public static void handleSettersArrays(PFs r, String name, Object arg, Class inputParamType){
        try( SeekableByteChannel sbc = r.p(name, WRITE)  ){
            if(inputParamType.isAssignableFrom(double[].class)){
                double[]array = (double[]) arg;
                if(array.length>0){
                    for (int i = 0; i < array.length - 1; i++) {
                        ByteBuffer bb = ByteBuffer.wrap(new byte[8]).putDouble(array[i]);
                        sbc.write(bb); sbc.write(ByteBuffer.wrap(PFile.systemNewLine));
                    }
                } return;
            }else if(inputParamType.isAssignableFrom(int[].class)){
                int[]array = (int[]) arg;
                if(array.length>0){
                    for (int i = 0; i < array.length - 1; i++) {
                        ByteBuffer bb = ByteBuffer.wrap(new byte[4]).putInt(array[i]);
                        sbc.write(bb); sbc.write(ByteBuffer.wrap(PFile.systemNewLine));
                    }
                } return;
            }else if(inputParamType.isAssignableFrom(long[].class)){
                long[]array = (long[]) arg;
                if(array.length>0){
                    for (int i = 0; i < array.length - 1; i++) {
                        ByteBuffer bb = ByteBuffer.wrap(new byte[8]).putLong(array[i]);
                        sbc.write(bb); sbc.write(ByteBuffer.wrap(PFile.systemNewLine));
                    }
                } return;
            }else if(inputParamType.isAssignableFrom(boolean[].class)){
                boolean[]array = (boolean[]) arg;
                if(array.length>0){
                    for (int i = 0; i < array.length - 1; i++) {
                        ByteBuffer bb = ByteBuffer.wrap(Boolean.toString(array[i]).getBytes(UTF8));
                        sbc.write(bb); sbc.write(ByteBuffer.wrap(PFile.systemNewLine));
                    }
                } return;
            }else if(inputParamType.isAssignableFrom(String[].class)){
                String[]array = (String[]) arg;
                if(array.length>0){
                    for (int i = 0; i < array.length - 1; i++) {        
                        ByteBuffer bb = ByteBuffer.wrap(array[i].getBytes(UTF8));
                        sbc.write(bb); sbc.write(ByteBuffer.wrap(PFile.systemNewLine));
                    }
                } return;
            }else {
                PFile.setAsPureJavaObject(r, name, arg);
                return;
            }
        }catch(Exception a){
            a.printStackTrace();
        }
    }
    
    
    private void handleSettters(Object proxy, Method m, Object arg)throws Exception{
        if(r instanceof MapPFs){
            ((MapPFs)r).put(m.getName(),arg);
            return;
        }else if(arg.getClass().isArray()){
            handleSettersArrays(proxy, m, arg, r);
        }else {
            PFile.set(r, m.getName(), arg);
        }
    }
}
