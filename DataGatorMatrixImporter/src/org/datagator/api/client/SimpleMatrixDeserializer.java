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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.ArrayList;

/**
 * JSON-decoder for simple Matrix object.
 *
 * @author LIU Yu <liuyu@opencps.net>
 * @date 2015/09/07
 */
class SimpleMatrixDeserializer
    extends JsonDeserializer<Matrix>
{

    private static void parseRows(JsonParser jp,
        int bodyRow, int bodyColumn, RowBuffer columnHeaders)
        throws IOException, JsonProcessingException
    {
        int rowIndex = 0;

        JsonToken token = jp.getCurrentToken(); // START_ARRAY
        if (!token.equals(JsonToken.START_ARRAY)) {
            throw new RuntimeException(
                String.format("Unexpected token %s", token));
        }

        token = jp.nextToken(); // START_ARRAY
        while (token.equals(JsonToken.START_ARRAY)) {
            int columnIndex = 0;
            if (rowIndex < bodyRow) {
                ArrayList<Object> buffer = new ArrayList<Object>();
                token = jp.nextToken();
                while (!token.equals(JsonToken.END_ARRAY)) {
                    buffer.add(jp.getText());
                    columnIndex += 1;
                    token = jp.nextToken();
                }
                if (columnHeaders != null) {
                    columnHeaders.put(buffer.toArray());
                }
            } else {
                token = jp.nextToken();
                while (!token.equals(JsonToken.END_ARRAY)) {
                    columnIndex += 1;
                    token = jp.nextToken();
                }
            }
            rowIndex += 1;
            token = jp.nextToken(); // START_ARRAY
        }
    }

    public Matrix deserialize(JsonParser jp, DeserializationContext cntx)
        throws IOException, JsonProcessingException
    {
        int rowsCount = -1;
        int columnsCount = -1;
        int bodyRow = -1;
        int bodyColumn = -1;

        SimpleRowBuffer columnHeaders = new SimpleRowBuffer();

        JsonToken token = jp.getCurrentToken(); // FIELD_NAME
        if (!token.equals(JsonToken.FIELD_NAME)) {
            throw new RuntimeException(
                String.format("Unexpected token %s", token));
        }
        while (token.equals(JsonToken.FIELD_NAME)) {
            String name = jp.getText();
            token = jp.nextToken();
            if (name.equals("columnHeaders")) {
                if (!token.equals(JsonToken.VALUE_NUMBER_INT)) {
                    throw new RuntimeException(
                        String.format("Unexpected token %s", token));
                }
                bodyRow = jp.getIntValue();
            } else if (name.equals("rowHeaders")) {
                if (!token.equals(JsonToken.VALUE_NUMBER_INT)) {
                    throw new RuntimeException(
                        String.format("Unexpected token %s", token));
                }
                bodyColumn = jp.getIntValue();
            } else if (name.equals("rows")) {
                if (bodyRow < 0 || bodyColumn < 0) {
                    throw new RuntimeException(
                        "Unexpected property order 'columnHeaders' and 'rowHeaders' should precede 'rows'.");
                }
                parseRows(jp, bodyRow, bodyColumn, columnHeaders);
            } else if (name.equals("rowsCount")) {
                if (!token.equals(JsonToken.VALUE_NUMBER_INT)) {
                    throw new RuntimeException(
                        String.format("Unexpected token %s", token));
                }
                rowsCount = jp.getIntValue();
            } else if (name.equals("columnsCount")) {
                if (!token.equals(JsonToken.VALUE_NUMBER_INT)) {
                    throw new RuntimeException(
                        String.format("Unexpected token %s", token));
                }
                columnsCount = jp.getIntValue();
            } else {
                throw new RuntimeException(
                    String.format("Unexpected property '%s'", name));
            }
            token = jp.nextToken(); // FIELD_NAME
        }

        if (!(0 <= bodyRow && bodyRow <= rowsCount)) {
            throw new RuntimeException("Invalid Matrix shape");
        }

        if (!(0 <= bodyColumn && bodyColumn <= columnsCount)) {
            throw new RuntimeException("Invalid Matrix shape");
        }

        // special case: size of empty matrix is 1 x 0
        if ((columnsCount == 0) && (rowsCount != 1)) {
            throw new RuntimeException("Invalid Matrix shape");
        }

        return new SimpleMatrix(bodyRow, bodyColumn, columnHeaders, rowsCount,
            columnsCount);
    }

};
