/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.datagator.api.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Memory-based row buffer implementation
 *
 * @author LIU Yu <liuyu@opencps.net>
 * @date 2015/09/09
 */
public class SimpleRowBuffer
    implements RowBuffer
{

    private final ArrayList<Object[]> model = new ArrayList<Object[]>();

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
