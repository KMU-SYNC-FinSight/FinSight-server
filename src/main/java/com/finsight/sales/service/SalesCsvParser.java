package com.finsight.sales.service;

import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SalesCsvParser {

    // 파싱 결과 한 줄 (정상 행)
    public record SalesRow(
            LocalDate date,
            long salesAmount,
            int transactionCount
    ) {}

    // 파싱 전체 결과: 정상 행들 + 스킵된(오류) 행 수
    public record ParseResult(
            List<SalesRow> rows,
            int skippedRows
    ) {}

    public ParseResult parse(MultipartFile file) {
        List<SalesRow> rows = new ArrayList<>();
        int skipped = 0;

        try (CSVReader reader = new CSVReader(
                new BufferedReader(new InputStreamReader(
                        file.getInputStream(), StandardCharsets.UTF_8)))) {

            String[] header = reader.readNext();
            if (header == null) {
                throw new BusinessException(ErrorCode.EMPTY_FILE);
            }
            // 헤더 검증: date,salesAmount,transactionCount
            validateHeader(header);

            String[] line;
            while ((line = reader.readNext()) != null) {
                // 빈 줄 skip
                if (line.length == 0
                        || (line.length == 1 && line[0].isBlank())) {
                    continue;
                }
                try {
                    rows.add(toRow(line));
                } catch (Exception e) {
                    // 행 하나가 깨져도 전체 실패시키지 않고 스킵 카운트만 증가
                    skipped++;
                }
            }

        } catch (IOException | CsvValidationException e) {
            throw new BusinessException(ErrorCode.CSV_PARSE_ERROR);
        }

        if (rows.isEmpty()) {
            // 정상 행이 하나도 없으면 잘못된 파일로 간주
            throw new BusinessException(ErrorCode.CSV_NO_VALID_ROWS);
        }

        return new ParseResult(rows, skipped);
    }

    private void validateHeader(String[] header) {
        if (header.length < 3
                || !header[0].trim().equalsIgnoreCase("date")
                || !header[1].trim().equalsIgnoreCase("salesAmount")
                || !header[2].trim().equalsIgnoreCase("transactionCount")) {
            throw new BusinessException(ErrorCode.CSV_INVALID_HEADER);
        }
    }

    private SalesRow toRow(String[] line) {
        LocalDate date = LocalDate.parse(line[0].trim());  // yyyy-MM-dd
        long salesAmount = Long.parseLong(line[1].trim());
        int transactionCount = Integer.parseInt(line[2].trim());

        if (salesAmount < 0 || transactionCount < 0) {
            throw new IllegalArgumentException("음수 값");
        }
        return new SalesRow(date, salesAmount, transactionCount);
    }
}