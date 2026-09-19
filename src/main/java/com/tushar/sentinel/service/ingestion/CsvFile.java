package com.tushar.sentinel.service.ingestion;

import com.tushar.sentinel.exception.ValidationException;
import com.tushar.sentinel.model.response.ingestion.BatchResult;
import com.tushar.sentinel.model.response.ingestion.RowError;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Minimal header-indexed CSV reader; the source exports are plain comma-separated text. */
public final class CsvFile {

    /** Data starts on line 2, so a row index becomes its line number by adding this. */
    private static final int HEADER_OFFSET = 2;
    private static final int BATCH_ID_LENGTH = 8;

    private final Map<String, Integer> columnIndex = new HashMap<>();
    private final List<String[]> rows = new ArrayList<>();
    private final int columnCount;

    public CsvFile(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new ValidationException("CSV file is empty");
            }
            String[] headers = headerLine.split(",", -1);
            columnCount = headers.length;
            for (int i = 0; i < headers.length; i++) {
                columnIndex.put(headers[i].trim().toLowerCase(), i);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    rows.add(line.split(",", -1));
                }
            }
        } catch (IOException e) {
            throw new ValidationException("Could not read CSV file: " + e.getMessage());
        }
    }

    public List<String[]> rows() {
        return rows;
    }

    public String get(String[] row, String column) {
        Integer index = columnIndex.get(column);
        if (index == null || index >= row.length) {
            return null;
        }
        String value = row[index].trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Runs {@code handler} over every row and reports the ones it rejected. Ingestion is
     * best-effort, so a row that throws is recorded and the load carries on: partial
     * monitoring beats none. A row whose column count does not match the header is rejected
     * rather than parsed, because an unquoted comma would otherwise shift every later column
     * and load wrong values silently.
     *
     * <p>ponytail: no quoted-field support, so a legitimately quoted comma is rejected too.
     * Swap in a real CSV parser if the source exports start quoting.
     */
    public BatchResult load(String entity, String refColumn, Consumer<String[]> handler) {
        long startedAt = System.currentTimeMillis();
        String batchId = "ING-" + entity + "-" + UUID.randomUUID().toString().substring(0, BATCH_ID_LENGTH);
        List<RowError> errors = new ArrayList<>();
        int accepted = 0;

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int line = i + HEADER_OFFSET;
            if (row.length != columnCount) {
                errors.add(new RowError(line, get(row, refColumn), refColumn,
                        "Expected " + columnCount + " columns but found " + row.length));
                continue;
            }
            try {
                handler.accept(row);
                accepted++;
            } catch (RuntimeException e) {
                errors.add(new RowError(line, get(row, refColumn), refColumn, e.getMessage()));
            }
        }

        return new BatchResult(batchId, entity, rows.size(), accepted, errors.size(),
                System.currentTimeMillis() - startedAt, errors);
    }
}
