/*
 * Copyright 2015 by University of Denver <http://pardee.du.edu/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.datagator.api.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Memory-based row buffer implementation.
 *
 * @author LIU Yu <liuyu@opencps.net>
 * @date 2015/09/09
 */
class SimpleRowBuffer
    implements RowBuffer
{

    private final ArrayList<Object[]> model = new ArrayList<Object[]>();

    @Override
    public void put(Object[] row)
    {
        model.add(row);
    }

    @Override
    public void clear()
    {
        model.clear();
    }

    @Override
    public int size()
    {
        return model.size();
    }
    
    @Override
    public Iterator<Object[]> iterator()
    {
        return new Iterator<Object[]>() {
            
            private int rowIndex = 0;
            
            @Override
            public boolean hasNext()
            {
                return rowIndex < model.size();
            }

            @Override
            public Object[] next()
            {
                if (!hasNext()) {
                    throw new NoSuchElementException("No such elememnt.");
                }
                return model.get(rowIndex++);
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            
        };
    }
}
