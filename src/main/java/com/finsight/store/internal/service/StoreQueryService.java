package com.finsight.store.internal.service;

import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import com.finsight.store.StoreQuery;
import com.finsight.store.StoreSummary;
import com.finsight.store.internal.domain.Store;
import com.finsight.store.internal.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreQueryService implements StoreQuery {

    private final StoreRepository storeRepository;

    @Override
    public StoreSummary getOwnedStore(Long userId, Long storeId) {
        Store store = storeRepository.findByIdAndUserId(storeId, userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.STORE_NOT_FOUND));

        return new StoreSummary(store.getId(), store.getName());
    }

    @Override
    public boolean isOwnedBy(Long storeId, Long userId) {
        return storeRepository.findByIdAndUserId(storeId, userId).isPresent();
    }
}
