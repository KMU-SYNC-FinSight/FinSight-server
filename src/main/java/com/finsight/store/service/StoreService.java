package com.finsight.store.service;

import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import com.finsight.store.domain.Store;
import com.finsight.store.dto.StoreCreateRequest;
import com.finsight.store.dto.StoreResponse;
import com.finsight.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    @Transactional
    public StoreResponse createStore(Long userId, StoreCreateRequest request) {
        Store store = Store.create(
                userId,
                request.name(),
                request.businessType(),
                request.address(),
                request.seatCount(),
                request.openedAt()
        );

        Store savedStore = storeRepository.save(store);

        return StoreResponse.from(savedStore);
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> getMyStores(Long userId) {
        return storeRepository.findByUserId(userId)
                .stream()
                .map(StoreResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoreResponse getStore(Long userId, Long storeId) {
        Store store = storeRepository.findByIdAndUserId(storeId, userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.STORE_NOT_FOUND));

        return StoreResponse.from(store);
    }
}