package com.finsight.store.domain;

import com.finsight.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 매장 소유자의 user.id (연관관계 대신 FK 값만 보관)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 30)
    private BusinessType businessType;

    @Column(length = 255)
    private String address;

    // null 값이 들어갈 수 있음
    @Column(name = "seat_count")
    private Integer seatCount;

    @Column(name = "opened_at")
    private LocalDate openedAt;

    public static Store create(
            Long userId,
            String name,
            BusinessType businessType,
            String address,
            Integer seatCount,
            LocalDate openedAt
    ) {
        Store store = new Store();
        store.userId = userId;
        store.name = name;
        store.businessType = businessType;
        store.address = address;
        store.seatCount = seatCount;
        store.openedAt = openedAt;
        return store;
    }
}