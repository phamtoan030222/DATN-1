package com.sd20201.datn.infrastructure.geo;

import com.sd20201.datn.entity.WardShippingMapping;
import com.sd20201.datn.repository.WardShippingMappingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Service
@Slf4j
public class ShippingFeeService {

    @Value("${ghtk.token}")
    private String ghtkToken;

    @Value("${ghtk.pick.province}")
    private String pickProvince;

    @Value("${ghtk.pick.district}")
    private String pickDistrict;

    @Value("${ghtk.pick.ward}")
    private String pickWard;

    @Value("${ghtk.pick.address}")
    private String pickAddress;

    @Autowired
    private WardShippingMappingRepository mappingRepo;

    @Autowired
    private RestTemplate restTemplate;

    public ShippingFeeResponse calculateFee(ShippingFeeRequest req) {
        WardShippingMapping mapping = mappingRepo
                .findByNewProvinceNameAndNewWardName(
                        req.getProvinceName(), req.getWardName())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy mapping: " + req.getWardName()));

        String ghtkProvince = removeAccents(stripProvincePrefix(mapping.getGhtkProvince()));
        String ghtkDistrict = removeAccents(mapping.getGhtkDistrict().trim());

        String cleanPickProvince = removeAccents(pickProvince);
        String cleanPickDistrict = removeAccents(pickDistrict);
        String cleanPickWard = removeAccents(pickWard);
        String cleanPickAddress = removeAccents(pickAddress);

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://services.giaohangtietkiem.vn/services/shipment/fee")
                .queryParam("pick_province", cleanPickProvince)
                .queryParam("pick_district", cleanPickDistrict)
                .queryParam("pick_ward", cleanPickWard)
                .queryParam("pick_address", cleanPickAddress)
                .queryParam("province", ghtkProvince)
                .queryParam("district", ghtkDistrict)
                .queryParam("address", req.getAddress())
                .queryParam("weight", req.getWeight())
                .queryParam("value", req.getOrderValue())
                .build()
                .toUri();

        log.info("=== GHTK URL: {}", uri);
        log.info("=== GHTK TOKEN: {}", ghtkToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghtkToken);

        try {
            ResponseEntity<Map> res = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            Map body = res.getBody();
            log.info("=== GHTK RESPONSE: {}", body);

            if (Boolean.TRUE.equals(body.get("success"))) {
                Map fee = (Map) body.get("fee");
                int shipFee = ((Number) fee.get("ship_fee_only")).intValue();
                log.info("=== GHTK FEE: {}", shipFee);
                return ShippingFeeResponse.builder()
                        .fee(shipFee).success(true).build();
            }

            String errMsg = (String) body.get("message");
            log.error("=== GHTK ERROR: '{}'", errMsg);
            return ShippingFeeResponse.builder()
                    .fee(0).success(false)
                    .message("GHTK: " + errMsg)
                    .build();

        } catch (Exception e) {
            log.error("=== GHTK EXCEPTION: {}", e.getMessage());
            return ShippingFeeResponse.builder()
                    .fee(0).success(false)
                    .message(e.getMessage())
                    .build();
        }
    }

    private String removeAccents(String text) {
        if (text == null) return "";
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("đ", "d")
                .replaceAll("Đ", "D")
                .trim();
    }

    private String stripProvincePrefix(String name) {
        if (name == null) return "";
        return name.replaceAll("^(Thành phố|Tỉnh)\\s+", "").trim();
    }
}