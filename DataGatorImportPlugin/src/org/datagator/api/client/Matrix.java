package org.datagator.api.client;

import java.util.Iterator;

/**
 * Four-square model of Matrix object
 *
 * @author LIU Yu <liuyu@opencps.net>
 * @date 2015/09/03
 */
public interface Matrix
{
    public int getRowsCount();
    public int getColumnsCount();
    public Matrix columnHeaders();
    public Iterator<Object[]> rows();
}
