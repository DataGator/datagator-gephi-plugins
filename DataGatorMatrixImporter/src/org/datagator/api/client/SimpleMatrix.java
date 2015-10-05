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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Iterator;

/**
 * Immutable Matrix object implementation.
 *
 * @author LIU Yu <liuyu@opencps.net>
 * @date 2015/09/07
 */
@JsonDeserialize(using = SimpleMatrixDeserializer.class)
class SimpleMatrix
    extends Entity
    implements Matrix
{

    private final int rowsCount;
    private final int columnsCount;
    private final RowBuffer rows;
    private final int bodyRow;
    private final int bodyColumn;

    protected SimpleMatrix(int columnHeaders, int rowHeaders,
        RowBuffer rows, int rowsCount, int columnsCount)
    {
        super("datagator#Matrix");
        this.bodyRow = columnHeaders;
        this.bodyColumn = rowHeaders;
        this.rows = rows;
        this.rowsCount = rowsCount;
        this.columnsCount = columnsCount;
    }

    @Override
    public int getRowsCount()
    {
        return rowsCount;
    }

    @Override
    public int getColumnsCount()
    {
        return columnsCount;
    }

    @Override
    public Matrix columnHeaders()
    {
        RowBuffer slice = new SimpleRowBuffer();
        Iterator<Object[]> it = rows();
        for (int r = 0; r < bodyRow; r++) {
            slice.put(it.next());
        }
        return new SimpleMatrix(0, bodyColumn, slice, bodyRow, columnsCount);
    }

    @Override
    public Iterator<Object[]> rows()
    {
        return rows.iterator();
    }
}
