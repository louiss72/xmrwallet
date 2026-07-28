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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

public class FeatherCsvTxNotesTest {
    @Test
    public void parseFeatherHistoryCsv() throws Exception {
        String csv = "blockHeight,timestamp,date,accountIndex,direction,balanceDelta,"
                + "amount,fee,txid,description,paymentID,fiatAmount,fiatCurrency\n"
                + "2317852,1615848678,2021-03-15T23:51:18Z,0,out,-0.2,"
                + "0.2,0.000017,abc123,\"Donation, with comma\",,20.2,USD\n"
                + "2317853,1615848699,2021-03-15T23:51:39Z,0,in,1,"
                + "1,0,def456,\"note with \"\"quotes\"\"\",,?,USD\n";

        FeatherCsvTxNotes.Result result = new FeatherCsvTxNotes().parse(new StringReader(csv));

        assertEquals(2, result.rowsRead);
        assertEquals(0, result.rowsSkipped);
        assertEquals("Donation, with comma", result.notes.get("abc123"));
        assertEquals("note with \"quotes\"", result.notes.get("def456"));
    }

    @Test
    public void skipsRowsWithoutTxidOrDescription() throws Exception {
        String csv = "txid,description\n"
                + "abc123,\n"
                + ",missing txid\n"
                + "def456,keep me\n";

        FeatherCsvTxNotes.Result result = new FeatherCsvTxNotes().parse(new StringReader(csv));

        assertEquals(3, result.rowsRead);
        assertEquals(2, result.rowsSkipped);
        assertEquals(1, result.notes.size());
        assertEquals("keep me", result.notes.get("def456"));
    }

    @Test
    public void parsesMultilineDescriptions() throws Exception {
        String csv = "txid,description\n"
                + "abc123,\"first line\n"
                + "second line\"\n";

        FeatherCsvTxNotes.Result result = new FeatherCsvTxNotes().parse(new StringReader(csv));

        assertEquals(1, result.rowsRead);
        assertEquals("first line\nsecond line", result.notes.get("abc123"));
    }

    @Test(expected = IOException.class)
    public void requiresFeatherColumns() throws Exception {
        new FeatherCsvTxNotes().parse(new StringReader("hash,note\nabc,hello\n"));
    }

    @Test
    public void parsesEmptyFile() throws Exception {
        FeatherCsvTxNotes.Result result = new FeatherCsvTxNotes().parse(new StringReader(""));

        assertTrue(result.notes.isEmpty());
        assertEquals(0, result.rowsRead);
        assertEquals(0, result.rowsSkipped);
    }
}
