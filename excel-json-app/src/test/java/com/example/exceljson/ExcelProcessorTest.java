package com.example.exceljson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExcelProcessorTest {

    @Test
    void convertRowToJsonCreatesNestedStructure() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        Row header = sheet.createRow(1);
        header.createCell(0).setCellValue("name");
        header.createCell(1).setCellValue("address.pincode");
        Row row = sheet.createRow(2);
        row.createCell(0).setCellValue("John");
        row.createCell(1).setCellValue("12345");

        ExcelProcessor processor = new ExcelProcessor(new RestTemplateBuilder(), new ObjectMapper());
        Map<Integer, String> fields = Map.of(0, "name", 1, "address.pincode");
        ObjectNode json = processor.convertRowToJson(row, fields);

        assertEquals("John", json.get("name").asText());
        assertEquals("12345", json.get("address").get("pincode").asText());
    }
}
