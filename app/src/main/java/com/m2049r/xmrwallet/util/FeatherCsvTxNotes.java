/*
 * Copyright (c) 2026 m2049r
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.xmrwallet.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FeatherCsvTxNotes {
    private static final String TXID = "txid";
    private static final String DESCRIPTION = "description";

    public static class Result {
        public final Map<String, String> notes;
        public final int rowsRead;
        public final int rowsSkipped;

        Result(Map<String, String> notes, int rowsRead, int rowsSkipped) {
            this.notes = notes;
            this.rowsRead = rowsRead;
            this.rowsSkipped = rowsSkipped;
        }
    }

    public Result parse(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        String headerLine = readRecord(bufferedReader);
        if (headerLine == null) {
            return new Result(new LinkedHashMap<>(), 0, 0);
        }

        List<String> headers = parseLine(stripBom(headerLine));
        int txidColumn = columnIndex(headers, TXID);
        int descriptionColumn = columnIndex(headers, DESCRIPTION);
        if (txidColumn < 0 || descriptionColumn < 0) {
            throw new IOException("CSV must include txid and description columns");
        }

        Map<String, String> notes = new LinkedHashMap<>();
        int rowsRead = 0;
        int rowsSkipped = 0;
        String line;
        while ((line = readRecord(bufferedReader)) != null) {
            rowsRead++;
            List<String> columns = parseLine(line);
            String txid = getColumn(columns, txidColumn).trim();
            String description = getColumn(columns, descriptionColumn).trim();
            if (txid.isEmpty() || description.isEmpty()) {
                rowsSkipped++;
                continue;
            }
            notes.put(txid, description);
        }
        return new Result(notes, rowsRead, rowsSkipped);
    }

    private String readRecord(BufferedReader reader) throws IOException {
        StringBuilder record = new StringBuilder();
        boolean quoted = false;
        String line;

        while ((line = reader.readLine()) != null) {
            if (record.length() > 0) {
                record.append('\n');
            }
            record.append(line);

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"') {
                    if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        i++;
                    } else {
                        quoted = !quoted;
                    }
                }
            }

            if (!quoted) {
                return record.toString();
            }
        }

        if (record.length() == 0) {
            return null;
        }
        throw new IOException("Unclosed quoted CSV field");
    }

    private int columnIndex(List<String> headers, String name) {
        for (int i = 0; i < headers.size(); i++) {
            if (name.equals(headers.get(i).trim().toLowerCase(Locale.US))) {
                return i;
            }
        }
        return -1;
    }

    private static String getColumn(List<String> columns, int index) {
        if (index >= columns.size()) {
            return "";
        }
        return columns.get(index);
    }

    private static String stripBom(String value) {
        if (!value.isEmpty() && value.charAt(0) == '\ufeff') {
            return value.substring(1);
        }
        return value;
    }

    private static List<String> parseLine(String line) throws IOException {
        List<String> result = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                result.add(value.toString());
                value.setLength(0);
            } else {
                value.append(c);
            }
        }

        if (quoted) {
            throw new IOException("Unclosed quoted CSV field");
        }

        result.add(value.toString());
        return result;
    }
}
