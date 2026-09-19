package com.tushar.hackathon.service.ingestion;

import com.tushar.hackathon.exception.ValidationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Minimal header-indexed CSV reader; the source exports are plain comma-separated text. */
public final class CsvFile {

    private final Map<String, Integer> columnIndex = new HashMap<>();
    private final List<String[]> rows = new ArrayList<>();

    public CsvFile(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new ValidationException("CSV file is empty");
            }
            String[] headers = headerLine.split(",", -1);
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
}
