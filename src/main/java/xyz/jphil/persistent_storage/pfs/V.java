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

import xyz.jphil.persistent_storage.pfs.type.TypeHandlerProvider;

/**
 *
 * @author 
 */
public interface V {

    int i(int defaultValue);
    
    long l(long defaultValue);

    double d(double defaultValue);

    float f(float defaultValue);

    String s(String defaultValue);

    boolean b(boolean defaultValue);
    
    String[] sa(String[]defaultValue);
    
    boolean isNull();
    
    Object o(DefaultValue dv,PFs r,String name,TypeHandlerProvider thp);
    
    <X> X o(Class<X> cast,X defaultValue);
    
    byte[]raw();
    
    public static final String NOT_FOUND = "NOT_FOUND";
}
