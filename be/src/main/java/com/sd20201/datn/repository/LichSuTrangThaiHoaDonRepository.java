package com.sd20201.datn.repository;

import com.sd20201.datn.entity.Invoice;
import com.sd20201.datn.entity.LichSuTrangThaiHoaDon;
import com.sd20201.datn.infrastructure.constant.EntityTrangThaiHoaDon;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LichSuTrangThaiHoaDonRepository extends JpaRepository<LichSuTrangThaiHoaDon, String> {
    List<LichSuTrangThaiHoaDon> findByHoaDonOrderByThoiGianDesc(Invoice hoaDon);

    Optional<LichSuTrangThaiHoaDon> findFirstByHoaDonAndTrangThaiOrderByThoiGianDesc(
            Invoice hoaDon,
            EntityTrangThaiHoaDon trangThai);

    List<LichSuTrangThaiHoaDon> findByHoaDonOrderByThoiGianAsc(Invoice hoaDon);
}
