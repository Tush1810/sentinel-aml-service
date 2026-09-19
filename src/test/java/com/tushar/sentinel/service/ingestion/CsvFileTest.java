package com.tushar.sentinel.service.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tushar.sentinel.model.response.ingestion.BatchResult;
import com.tushar.sentinel.model.response.ingestion.RowError;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The shared ingestion loop: good rows accepted, bad rows reported against the right line. */
class CsvFileTest {

    private static CsvFile csv(String content) {
        return new CsvFile(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void reportsRejectedRowsAgainstTheirLineNumberAndKeepsLoading() {
        CsvFile csv = csv("""
                customer_id,name
                CUST_1,Ada
                CUST_2,Bad
                CUST_3,Grace
                """);
        List<String> loaded = new ArrayList<>();

        BatchResult result = csv.load("CUSTOMER", "customer_id", row -> {
            String ref = csv.get(row, "customer_id");
            if ("CUST_2".equals(ref)) {
                throw new IllegalStateException("rejected");
            }
            loaded.add(ref);
        });

        assertEquals(List.of("CUST_1", "CUST_3"), loaded);
        assertEquals(3, result.received());
        assertEquals(2, result.accepted());
        assertEquals(1, result.rejected());
        RowError error = result.errors().getFirst();
        assertEquals(3, error.row());
        assertEquals("CUST_2", error.reference());
        assertEquals("rejected", error.reason());
    }

    /** An unquoted comma would shift every later column, so the row is rejected, not parsed. */
    @Test
    void rejectsRowsWhoseColumnCountDoesNotMatchTheHeader() {
        CsvFile csv = csv("""
                customer_id,city
                CUST_1,Pune,extra
                """);

        BatchResult result = csv.load("CUSTOMER", "customer_id", row -> {
            throw new AssertionError("malformed row must not reach the handler");
        });

        assertEquals(0, result.accepted());
        assertEquals(1, result.rejected());
        assertEquals(2, result.errors().getFirst().row());
    }
}
