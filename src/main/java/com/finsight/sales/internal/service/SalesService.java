package com.finsight.sales.internal.service;

import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import com.finsight.global.storage.FileStorage;
import com.finsight.sales.SalesDataChangedEvent;
import com.finsight.sales.internal.domain.SalesMetric;
import com.finsight.sales.internal.dto.SalesUploadResponse;
import com.finsight.sales.internal.repository.SalesMetricRepository;
import com.finsight.sales.internal.service.SalesCsvParser.ParseResult;
import com.finsight.sales.internal.service.SalesCsvParser.SalesRow;
import com.finsight.store.StoreQuery;
import com.finsight.store.StoreSummary;
import com.finsight.upload.UploadTracker;
import com.finsight.upload.UploadType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final StoreQuery storeQuery;
    private final UploadTracker uploadTracker;
    private final SalesMetricRepository salesMetricRepository;
    private final SalesCsvParser salesCsvParser;
    private final FileStorage fileStorage;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SalesUploadResponse uploadSalesCsv(
            Long userId,
            Long storeId,
            MultipartFile file
    ) {
        // 1. 매장 소유권 확인
        StoreSummary store = storeQuery.getOwnedStore(userId, storeId);

        // 2. 파일 기본 검증 (확장자)
        validateCsvFile(file);

        // 3. 파싱 (헤더/행 검증 포함)
        ParseResult parseResult = salesCsvParser.parse(file);

        // 4. 원본 저장 + 업로드 이력 (COMPLETED)
        String storedPath = fileStorage.store(file, "sales");
        Long uploadId = uploadTracker.recordCompletedCsvUpload(
                store.storeId(),
                UploadType.SALES_CSV,
                file.getOriginalFilename(),
                storedPath
        );

        // 5. 행별 upsert (덮어쓰기)
        int inserted = 0;
        int updated = 0;
        for (SalesRow row : parseResult.rows()) {
            Optional<SalesMetric> existing =
                    salesMetricRepository.findByStoreIdAndMetricDate(
                            store.storeId(), row.date());

            if (existing.isPresent()) {
                existing.get().update(
                        uploadId,
                        row.salesAmount(),
                        row.transactionCount()
                );
                updated++;
            } else {
                salesMetricRepository.save(SalesMetric.create(
                        store.storeId(),
                        uploadId,
                        row.date(),
                        row.salesAmount(),
                        row.transactionCount()
                ));
                inserted++;
            }
        }

        // 6. 커밋 후 score 모듈이 운영 점수를 다시 계산한다
        eventPublisher.publishEvent(new SalesDataChangedEvent(store.storeId()));

        return new SalesUploadResponse(
                uploadId,
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