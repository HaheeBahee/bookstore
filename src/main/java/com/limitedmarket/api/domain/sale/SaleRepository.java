package com.limitedmarket.api.domain.sale;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sale s WHERE s.id IN :ids ORDER BY s.id ASC")
    List<Sale> findAllByIdWithLock(@Param("ids") List<Long> ids);

    // 스케줄러용 - 상태 전환 대상 조회
    List<Sale> findByVipOpenAtBeforeAndSaleStatus(LocalDateTime now, SaleStatus status);

    List<Sale> findByGeneralOpenAtBeforeAndSaleStatusIn(LocalDateTime now, List<SaleStatus> statuses);

    List<Sale> findByCloseAtBeforeAndSaleStatus(LocalDateTime now, SaleStatus status);

    List<Sale> findByCloseAtBeforeAndSaleStatusIn(LocalDateTime now, List<SaleStatus> statuses);

    List<Sale> findBySaleStatusIn(List<SaleStatus> statuses);
}
