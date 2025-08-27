package com.sd20201.datn.core.admin.products.brand.service.impl;

import com.sd20201.datn.core.admin.products.brand.model.request.ADBrandRequest;
import com.sd20201.datn.core.admin.products.brand.model.request.ADCreateBrandRequest;
import com.sd20201.datn.core.admin.products.brand.repository.ADBrandRepository;
import com.sd20201.datn.core.admin.products.brand.service.ADBrandService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Brand;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ADBrandServiceImpl implements ADBrandService {

    private final ADBrandRepository adBrandRepository;

    @Override
    public ResponseObject<?> getAllBrands(ADBrandRequest request) {
        Pageable pageable = Helper.createPageable(request, "createdDate");
        return new ResponseObject<>(
                PageableObject.of(adBrandRepository.getAllBrands(
                        pageable,
                        request.getName()
                )),
                HttpStatus.OK,
                "Lấy thành công danh sách thương hiệu"
        );
    }

    @Override
    public ResponseObject<?> createBrand(ADCreateBrandRequest request) {
        Brand brand = new Brand();

        brand.setName(request.getName());
        brand.setCode(request.getCode());
        brand.setCreatedDate(System.currentTimeMillis());
        brand.setStatus(EntityStatus.ACTIVE);

        adBrandRepository.save(brand);

        return new ResponseObject<>(
                null,
                HttpStatus.OK,
                "Thêm mới thương hiệu thành công",
                true,
                null
        );
    }

    @Override
    public ResponseObject<?> updateBrandStatus(String id) {
        Optional<Brand> optionalBrand = adBrandRepository.findById(id);
        if (optionalBrand.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Thương hiệu không tồn tại", false, null);
        }

        Brand brand = optionalBrand.get();
        brand.setStatus(brand.getStatus().equals(EntityStatus.ACTIVE) ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
        adBrandRepository.save(brand);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật trạng thái thương hiệu thành công", true, null);
    }

    @Override
    public ResponseObject<?> updateBrand(String id, ADCreateBrandRequest request) {
        Optional<Brand> optionalBrand = adBrandRepository.findById(id);
        if (optionalBrand.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Thương hiệu không tồn tại", false, null);
        }

        Brand brand = optionalBrand.get();
        brand.setCode(request.getCode());
        brand.setName(request.getName());

        adBrandRepository.save(brand);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật thương hiệu thành công", true, null);
    }

}
