package com.example.exceljson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.ss.usermodel.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Component
public class ExcelProcessor {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ExcelProcessor(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.restTemplate = builder.build();
        this.objectMapper = objectMapper;
    }

    public void process(String metadataPath) throws IOException {
        Metadata metadata = objectMapper.readValue(new File(metadataPath), Metadata.class);
        byte[] fileBytes = restTemplate.getForObject(metadata.getFileUrl(), byte[].class);
        if (fileBytes == null) {
            throw new IOException("Failed to download file");
        }
        try (InputStream is = new ByteArrayInputStream(fileBytes); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row fieldRow = sheet.getRow(1);
            Map<Integer, String> fields = new HashMap<>();
            for (Cell cell : fieldRow) {
                fields.put(cell.getColumnIndex(), cell.getStringCellValue());
            }
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row dataRow = sheet.getRow(i);
                ObjectNode json = convertRowToJson(dataRow, fields);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (metadata.getHeaders() != null) {
                    metadata.getHeaders().forEach(headers::add);
                }
                HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(json), headers);
                restTemplate.postForEntity(metadata.getApiUrl(), entity, String.class);
            }
        }
    }

    public ObjectNode convertRowToJson(Row dataRow, Map<Integer, String> fields) {
        ObjectNode root = objectMapper.createObjectNode();
        fields.forEach((idx, name) -> {
            Cell cell = dataRow.getCell(idx);
            if (cell != null) {
                String value = getCellString(cell);
                setJsonValue(root, name, value);
            }
        });
        return root;
    }

    private String getCellString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private void setJsonValue(ObjectNode node, String path, String value) {
        String[] parts = path.split("\\.");
        ObjectNode current = node;
        for (int i = 0; i < parts.length - 1; i++) {
            current = current.with(parts[i]);
        }
        current.put(parts[parts.length - 1], value);
    }
}
