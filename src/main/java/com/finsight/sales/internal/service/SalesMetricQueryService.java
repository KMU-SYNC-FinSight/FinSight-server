package com.finsight.sales.internal.service;

import com.finsight.sales.DailySales;
import com.finsight.sales.SalesMetricQuery;
import com.finsight.sales.internal.repository.SalesMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesMetricQueryService implements SalesMetricQuery {

    private final SalesMetricRepository salesMetricRepository;

    @Override
    public List<DailySales> findAllByStoreId(Long storeId) {
        return salesMetricRepository.findByStoreIdOrderByMetricDateAsc(storeId)
                .stream()
                .map(metric -> new DailySales(
                        metric.getMetricDate(),
                        metric.getSalesAmount(),
                        metric.getTransactionCount()
                ))
                .toList();
    }
}
