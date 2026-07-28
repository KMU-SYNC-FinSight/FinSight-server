package com.finsight.sales.service;

import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import com.finsight.global.storage.FileStorage;
import com.finsight.sales.domain.SalesMetric;
import com.finsight.sales.dto.SalesUploadResponse;
import com.finsight.sales.repository.SalesMetricRepository;
import com.finsight.sales.service.SalesCsvParser.ParseResult;
import com.finsight.sales.service.SalesCsvParser.SalesRow;
import com.finsight.score.service.ScoreService;
import com.finsight.store.domain.Store;
import com.finsight.store.repository.StoreRepository;
import com.finsight.upload.domain.DataUpload;
import com.finsight.upload.domain.UploadType;
import com.finsight.upload.repository.DataUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final StoreRepository storeRepository;
    private final DataUploadRepository dataUploadRepository;
    private final SalesMetricRepository salesMetricRepository;
    private final SalesCsvParser salesCsvParser;
    private final FileStorage fileStorage;
    private final ScoreService scoreService;  // 필드 추가

    @Transactional
    public SalesUploadResponse uploadSalesCsv(
            Long userId,
            Long storeId,
            MultipartFile file
    ) {
        // 1. 매장 소유권 확인
        Store store = storeRepository.findByIdAndUserId(storeId, userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 2. 파일 기본 검증 (확장자)
        validateCsvFile(file);

        // 3. 파싱 (헤더/행 검증 포함)
        ParseResult parseResult = salesCsvParser.parse(file);

        // 4. 원본 저장 + 업로드 이력 (COMPLETED)
        String storedPath = fileStorage.store(file, "sales");
        DataUpload upload = DataUpload.createCsvUpload(
                store.getId(),
                UploadType.SALES_CSV,
                file.getOriginalFilename(),
                storedPath
        );
        DataUpload savedUpload = dataUploadRepository.save(upload);

        // 5. 행별 upsert (덮어쓰기)
        int inserted = 0;
        int updated = 0;
        for (SalesRow row : parseResult.rows()) {
            Optional<SalesMetric> existing =
                    salesMetricRepository.findByStoreIdAndMetricDate(
                            store.getId(), row.date());

            if (existing.isPresent()) {
                existing.get().update(
                        savedUpload.getId(),
                        row.salesAmount(),
                        row.transactionCount()
                );
                updated++;
            } else {
                salesMetricRepository.save(SalesMetric.create(
                        store.getId(),
                        savedUpload.getId(),
                        row.date(),
                        row.salesAmount(),
                        row.transactionCount()
                ));
                inserted++;
            }
        }
        // ... 행별 upsert 끝난 뒤, return 직전 ...
        scoreService.recalculate(store.getId());

        return new SalesUploadResponse(
                savedUpload.getId(),
                parseResult.rows().size() + parseResult.skippedRows(),
                inserted,
                updated,
                parseResult.skippedRows()
        );
    }

    private void validateCsvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }
        String name = file.getOriginalFilename();
        if (name == null
                || !name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }
}