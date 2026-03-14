package com.sd20201.datn.infrastructure.geo;

import com.sd20201.datn.entity.WardShippingMapping;
import com.sd20201.datn.repository.WardShippingMappingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class AddressMappingInitService {

    @Value("${ghtk.token}")
    private String ghtkToken;

    @Autowired
    private WardShippingMappingRepository mappingRepo;

    @Autowired
    private RestTemplate restTemplate;

    @PostConstruct
    public void buildMappingIfEmpty() {
        long count = mappingRepo.count();
        if (count > 0) {
            log.info("[MAPPING] Đã tồn tại {} bản ghi, bỏ qua build.", count);
            return;
        }
        log.info("[MAPPING] Bắt đầu build mapping địa chỉ 34 tỉnh mới → 63 tỉnh cũ bằng MÃ XÃ (CODE)...");
        buildMapping();
    }

    // =========================================================
    //  CORE LOGIC
    // =========================================================

    private void buildMapping() {
        // STEP 1: Lấy 34 tỉnh mới
        log.info("[STEP 1] Đang fetch 34 tỉnh mới từ API v2...");
        List<Map> newProvinces = fetchNew34Provinces();
        log.info("[STEP 1] Lấy được {} tỉnh mới.", newProvinces != null ? newProvinces.size() : 0);

        if (newProvinces == null || newProvinces.isEmpty()) {
            log.error("[STEP 1] Không lấy được dữ liệu 34 tỉnh mới. Dừng tiến trình!");
            return;
        }

        // STEP 2: Lấy 63 tỉnh cũ (API v1)
        log.info("[STEP 2] Đang fetch 63 tỉnh cũ từ API v1 (depth=3)...");
        List<Map> oldProvinces = fetchOldProvinces();
        log.info("[STEP 2] Lấy được {} tỉnh cũ.", oldProvinces != null ? oldProvinces.size() : 0);

        if (oldProvinces == null || oldProvinces.isEmpty()) {
            log.error("[STEP 2] Không lấy được dữ liệu 63 tỉnh cũ. Dừng tiến trình!");
            return;
        }

        // STEP 3: Build index ward từ dữ liệu 63 tỉnh cũ (Sử dụng MÃ XÃ làm Key)
        log.info("[STEP 3] Đang build ward index từ 63 tỉnh cũ...");
        Map<Integer, Map<String, String>> oldWardIndexByCode = buildOldWardIndex(oldProvinces);
        log.info("[STEP 3] Ward index có {} entries (Mã Xã).", oldWardIndexByCode.size());

        // Chuẩn bị danh sách 11 xã/đặc khu ngoại lệ cần map thủ công
        Map<Integer, String[]> manualFixes = getManualFixes();

        // STEP 4: Map từng ward của 34 tỉnh mới → tìm trong index cũ
        log.info("[STEP 4] Đang map wards từ 34 tỉnh mới bằng MÃ XÃ...");
        List<WardShippingMapping> result = new ArrayList<>();
        List<String> notFoundList = new ArrayList<>();

        // Bộ đếm tổng
        int totalWardCount  = 0;
        int totalFound      = 0;
        int totalNotFound   = 0;

        // Bộ đếm theo từng tỉnh
        int provIndex = 0;
        int totalProvinces = newProvinces.size();

        for (Map newProv : newProvinces) {
            provIndex++;
            String newProvName = (String) newProv.get("name");
            Integer provCode   = (Integer) newProv.get("code");

            log.info("[STEP 4] [{}/{}] Đang xử lý tỉnh: '{}' (code={})",
                    provIndex, totalProvinces, newProvName, provCode);

            List<Map> newWards = fetchNew34Wards(provCode);

            if (newWards == null || newWards.isEmpty()) {
                log.warn("[STEP 4]   ⚠ Tỉnh '{}' không có ward nào trả về từ API!", newProvName);
                continue;
            }

            // Bộ đếm riêng cho tỉnh này
            int provTotal    = newWards.size();
            int provFound    = 0;
            int provNotFound = 0;

            for (Map newWard : newWards) {
                String newWardName = (String) newWard.get("name");
                Integer wardCode   = (Integer) newWard.get("code"); // Lấy mã xã (Code)

                if (wardCode == null) {
                    log.warn("[STEP 4]   ⚠ Xã '{}' không có mã code. Bỏ qua!", newWardName);
                    continue;
                }

                // TÌM KIẾM BẰNG MÃ XÃ (CODE)
                Map<String, String> oldInfo = oldWardIndexByCode.get(wardCode);
                String[] manualFix = manualFixes.get(wardCode);

                if (oldInfo != null) {
                    // Trúng data từ API 63 tỉnh cũ
                    result.add(WardShippingMapping.builder()
                            .newProvinceName(newProvName)
                            .newWardName(newWardName)
                            .ghtkProvince(oldInfo.get("province"))
                            .ghtkDistrict(oldInfo.get("district"))
                            .build());
                    provFound++;
                } else if (manualFix != null) {
                    // Trúng data 11 xã/đặc khu ngoại lệ
                    result.add(WardShippingMapping.builder()
                            .newProvinceName(newProvName)
                            .newWardName(newWardName)
                            .ghtkProvince(manualFix[0]) // Tỉnh cũ
                            .ghtkDistrict(manualFix[1]) // Huyện cũ
                            .build());
                    provFound++;
                    log.info("[STEP 4]   ★ MANUAL FIX: Code {} | '{}' → GHTK: province='{}', district='{}'",
                            wardCode, newWardName, manualFix[0], manualFix[1]);
                } else {
                    // Hoàn toàn không tìm thấy
                    notFoundList.add(String.format("Tỉnh: %s | Xã: %s | Mã Xã: %d", newProvName, newWardName, wardCode));
                    provNotFound++;
                    log.warn("[STEP 4]   ✗ NOT FOUND: Code {} - '{}' thuộc tỉnh '{}'", wardCode, newWardName, newProvName);
                }
            }

            // Log tổng kết từng tỉnh
            totalWardCount += provTotal;
            totalFound     += provFound;
            totalNotFound  += provNotFound;

            log.info("[STEP 4]   └─ Kết quả tỉnh '{}': tổng xã={} | ✓ mapped={} | ✗ thiếu={} | tỷ lệ={}%",
                    newProvName, provTotal, provFound, provNotFound,
                    provTotal > 0 ? String.format("%.1f", provFound * 100.0 / provTotal) : "0");
        }

        // STEP 5: Lưu DB
        if (!result.isEmpty()) {
            log.info("[STEP 5] Đang lưu {} bản ghi mapping vào Database...", result.size());
            mappingRepo.saveAll(result);
            log.info("[STEP 5] Lưu thành công!");
        } else {
            log.warn("[STEP 5] Không có bản ghi nào được map thành công để lưu vào Database.");
        }

        // STEP 6: Summary tổng
        log.info("============================================");
        log.info("          BUILD MAPPING SUMMARY            ");
        log.info("============================================");
        log.info("  Tổng số tỉnh xử lý  : {}", totalProvinces);
        log.info("  Tổng số xã (mới)    : {}", totalWardCount);
        log.info("  ✓ Mapped thành công : {} / {} ({}%)",
                totalFound, totalWardCount,
                totalWardCount > 0 ? String.format("%.1f", totalFound * 100.0 / totalWardCount) : "0");
        log.info("  ✗ Không tìm thấy    : {} / {} ({}%)",
                totalNotFound, totalWardCount,
                totalWardCount > 0 ? String.format("%.1f", totalNotFound * 100.0 / totalWardCount) : "0");
        log.info("  Đã lưu vào DB       : {}", result.size());
        log.info("============================================");

        if (!notFoundList.isEmpty()) {
            log.warn("  Danh sách KHÔNG TÌM THẤY ({} xã):", notFoundList.size());
            notFoundList.forEach(s -> log.warn("    - {}", s));
        }
    }

    // =========================================================
    //  FETCH API
    // =========================================================

    private List<Map> fetchNew34Provinces() {
        try {
            String url = "https://provinces.open-api.vn/api/v2/p/?depth=1";
            ResponseEntity<List> res = restTemplate.getForEntity(url, List.class);
            return res.getBody();
        } catch (Exception e) {
            log.error("[FETCH] Lỗi khi gọi API v2 lấy 34 tỉnh: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Map> fetchNew34Wards(Integer provinceCode) {
        try {
            String url = "https://provinces.open-api.vn/api/v2/p/" + provinceCode + "?depth=2";
            ResponseEntity<Map> res = restTemplate.getForEntity(url, Map.class);
            Map body = res.getBody();
            if (body == null) return Collections.emptyList();

            Object wards = body.get("wards");
            if (wards == null) return Collections.emptyList();

            return (List<Map>) wards;
        } catch (Exception e) {
            log.error("[FETCH] Lỗi khi gọi API v2 lấy xã của tỉnh {}: {}", provinceCode, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Map> fetchOldProvinces() {
        try {
            String url = "https://provinces.open-api.vn/api/v1/?depth=3";
            ResponseEntity<List> res = restTemplate.getForEntity(url, List.class);
            return res.getBody();
        } catch (Exception e) {
            log.error("[FETCH] Lỗi khi gọi API v1 lấy 63 tỉnh: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // =========================================================
    //  BUILD INDEX & HELPERS
    // =========================================================

    private Map<Integer, Map<String, String>> buildOldWardIndex(List<Map> provinces) {
        Map<Integer, Map<String, String>> index = new HashMap<>();
        int totalWards = 0;
        int duplicateKeys = 0;

        for (Map province : provinces) {
            String provinceName = (String) province.get("name");

            List<Map> districts = (List<Map>) province.get("districts");
            if (districts == null) continue;

            for (Map district : districts) {
                String districtName = (String) district.get("name");
                List<Map> wards = (List<Map>) district.get("wards");
                if (wards == null) continue;

                for (Map ward : wards) {
                    Integer wardCode = (Integer) ward.get("code");
                    String wardName = (String) ward.get("name");

                    if (wardCode == null) continue;

                    if (index.containsKey(wardCode)) {
                        duplicateKeys++;
                        log.warn("[INDEX] ⚠ DUPLICATE WARD CODE: {} | Cũ: {} | Mới: {} - {} - {}",
                                wardCode, index.get(wardCode).get("wardName"), wardName, districtName, provinceName);
                    }

                    Map<String, String> info = new HashMap<>();
                    info.put("province", provinceName);
                    info.put("district", districtName);
                    info.put("wardName", wardName); // Lưu lại để log nếu cần

                    index.put(wardCode, info);
                    totalWards++;
                }
            }
        }

        log.info("[INDEX] Tổng ward đã index bằng MÃ XÃ: {}, duplicate keys: {}", totalWards, duplicateKeys);
        return index;
    }

    /**
     * Helper chứa 11 xã / đặc khu khó lấy do hệ thống GHTK / Địa giới cũ không có cấp phường xã.
     * Cấu trúc: Key (Mã xã) -> Value [Tỉnh GHTK, Huyện GHTK]
     */
    private Map<Integer, String[]> getManualFixes() {
        Map<Integer, String[]> fixes = new HashMap<>();
        // 5 Huyện đảo / Đặc khu
        fixes.put(11948, new String[]{"Hải Phòng", "Huyện Bạch Long Vĩ"});
        fixes.put(19742, new String[]{"Quảng Trị", "Huyện Cồn Cỏ"});
        fixes.put(20333, new String[]{"Đà Nẵng", "Huyện Hoàng Sa"});
        fixes.put(21548, new String[]{"Quảng Ngãi", "Huyện Lý Sơn"});
        fixes.put(26732, new String[]{"Bà Rịa - Vũng Tàu", "Huyện Côn Đảo"}); // Côn Đảo cũ thuộc BR-VT

        // Các phường sáp nhập mới lên
        fixes.put(3980, new String[]{"Sơn La", "Huyện Mộc Châu"}); // Phường Mộc Châu -> Huyện Mộc Châu
        fixes.put(26929, new String[]{"TP. Hồ Chí Minh", "Quận Bình Thạnh"}); // Phường Bình Thạnh
        fixes.put(27226, new String[]{"TP. Hồ Chí Minh", "Quận 11"}); // Phường Phú Thọ
        fixes.put(27232, new String[]{"TP. Hồ Chí Minh", "Quận 11"}); // Phường Bình Thới
        fixes.put(27367, new String[]{"TP. Hồ Chí Minh", "Quận 6"}); // Phường Bình Tây
        fixes.put(27373, new String[]{"TP. Hồ Chí Minh", "Quận 6"}); // Phường Bình Tiên

        return fixes;
    }
}