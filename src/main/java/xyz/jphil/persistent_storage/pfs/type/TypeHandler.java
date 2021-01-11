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

import xyz.jphil.persistent_storage.pfs.DefaultValue;
import xyz.jphil.persistent_storage.pfs.PFs;

/**
 *
 * @author 
 */
public interface TypeHandler {
    Class type();
    /**
     * Say this is a list. Then default value NOT of the list,
     * but of the elements present in the list.
     * @param r 
     * @param dv may be null
     * @return 
     */
    Object handle(PFs r,DefaultValue dv);
    Object put(PFs r, Object value,DefaultValue dv);
}
