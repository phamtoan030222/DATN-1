package com.sd20201.datn.core.admin.staff.controller;

import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.infrastructure.geo.GeoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_GEO)
public class GeoController {

    @Autowired
    private GeoService geoService;

    @GetMapping("/provinces")
    public List<Map<String, Object>> getProvinces() {
        return geoService.getProvinces();
    }

    // Lấy danh sách huyện theo mã tỉnh (dùng path param)
    @GetMapping("/districts/{provinceCode}")
    public List<Map<String, Object>> getDistricts(@PathVariable String provinceCode) {
        return geoService.getDistricts(provinceCode);
    }

    // Lấy danh sách xã theo mã huyện (dùng path param)
    @GetMapping("/communes/{districtCode}")
    public List<Map<String, Object>> getCommunes(@PathVariable String districtCode) {
        return geoService.getCommunes(districtCode);
    }
}


