package com.sd20201.datn.core.admin.products.product.service.impl;

import com.sd20201.datn.core.admin.products.product.model.request.ADProductCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.product.model.request.ADProductRequest;
import com.sd20201.datn.core.admin.products.product.repository.ADPRBatteryRepository;
import com.sd20201.datn.core.admin.products.product.repository.ADPRBrandRepository;
import com.sd20201.datn.core.admin.products.product.repository.ADPROperatingSystemRepository;
import com.sd20201.datn.core.admin.products.product.repository.ADPRScreenRepository;
import com.sd20201.datn.core.admin.products.product.repository.ADProductRepository;
import com.sd20201.datn.core.admin.products.product.service.ADProductService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Battery;
import com.sd20201.datn.entity.Brand;
import com.sd20201.datn.entity.OperatingSystem;
import com.sd20201.datn.entity.Product;
import com.sd20201.datn.entity.Screen;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ADProductServiceImpl implements ADProductService {

    private final ADProductRepository productRepository;

    private final ADPRScreenRepository screenRepository;

    private final ADPRBrandRepository brandRepository;

    private final ADPROperatingSystemRepository operatingSystemRepository;

    private final ADPRBatteryRepository batteryRepository;

    @Override
    public ResponseObject<?> getProducts(ADProductRequest request) {
        return ResponseObject.successForward(
                PageableObject.of(productRepository.getProducts(Helper.createPageable(request), request)),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getDetail(String id) {
        return productRepository.getProductById(id)
                .map(product -> ResponseObject.successForward(product, "Get product detail success"))
                .orElseGet(() -> ResponseObject.errorForward("Get product detail failed", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> changeStatus(String id) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setStatus(product.getStatus() == EntityStatus.ACTIVE ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
                    productRepository.save(product);
                    return ResponseObject.successForward(product, "Update product status success");
                })
                .orElseGet(() -> ResponseObject.errorForward("Get product detail failed", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> modify(ADProductCreateUpdateRequest request) {
        return request.getId() == null || request.getId().isEmpty()   ? create(request) : update(request);
    }

    public ResponseObject<?> create(ADProductCreateUpdateRequest request) {

        Optional<Product> optionalProduct = productRepository.findByCode(request.getCode());
        if (optionalProduct.isPresent()) return ResponseObject.errorForward("Create product failure !!! Duplicate code", HttpStatus.CONFLICT);

        Optional<Screen> optionalScreen = screenRepository.findById(request.getIdScreen());
        if(optionalScreen.isEmpty()) return ResponseObject.errorForward("Screen not found", HttpStatus.NOT_FOUND);

        Optional<Battery> optionalBattery = batteryRepository.findById(request.getIdBattery());
        if(optionalBattery.isEmpty()) return ResponseObject.errorForward("Battery not found", HttpStatus.NOT_FOUND);

        Optional<OperatingSystem> optionalOS = operatingSystemRepository.findById(request.getIdOperatingSystem());
        if(optionalOS.isEmpty()) return ResponseObject.errorForward("OperatingSystem not found", HttpStatus.NOT_FOUND);

        Optional<Brand> optionalBrand = brandRepository.findById(request.getIdBrand());
        if(optionalBrand.isEmpty()) return ResponseObject.errorForward("Brand not found", HttpStatus.NOT_FOUND);

        Product product = new Product();

        product.setCode(request.getCode());
        product.setName(request.getName());
        product.setScreen(optionalScreen.get());
        product.setBrand(optionalBrand.get());
        product.setOperatingSystem(optionalOS.get());
        product.setBattery(optionalBattery.get());

        return ResponseObject.successForward(productRepository.save(product), "Create product success");
    }

    public ResponseObject<?> update(ADProductCreateUpdateRequest request) {

        Optional<Product> optionalProduct = productRepository.findById(request.getId());
        if(optionalProduct.isEmpty()) return ResponseObject.errorForward("Product not found", HttpStatus.NOT_FOUND);

        Product product = optionalProduct.get();

        if(!product.getScreen().getId().equals(request.getIdScreen())) {
            Optional<Screen> optionalScreen = screenRepository.findById(request.getIdScreen());
            if (optionalScreen.isEmpty()) return ResponseObject.errorForward("Screen not found", HttpStatus.NOT_FOUND);

            product.setScreen(optionalScreen.get());
        }

        if(!product.getBattery().getId().equals(request.getIdBattery())) {
            Optional<Battery> optionalBattery = batteryRepository.findById(request.getIdBattery());
            if (optionalBattery.isEmpty())
                return ResponseObject.errorForward("Battery not found", HttpStatus.NOT_FOUND);

            product.setBattery(optionalBattery.get());
        }

        if(!product.getOperatingSystem().getId().equals(request.getIdOperatingSystem())) {
            Optional<OperatingSystem> optionalOS = operatingSystemRepository.findById(request.getIdOperatingSystem());
            if (optionalOS.isEmpty())
                return ResponseObject.errorForward("OperatingSystem not found", HttpStatus.NOT_FOUND);

            product.setOperatingSystem(optionalOS.get());
        }

        if(!product.getBrand().getId().equals(request.getIdBrand())) {
            Optional<Brand> optionalBrand = brandRepository.findById(request.getIdBrand());
            if (optionalBrand.isEmpty()) return ResponseObject.errorForward("Brand not found", HttpStatus.NOT_FOUND);

            product.setBrand(optionalBrand.get());
        }

        return ResponseObject.successForward(productRepository.save(product), "Update product success");
    }

    @Override
    public ResponseObject<?> getScreens() {
        return ResponseObject.successForward(screenRepository.getScreenComboboxResponse(), "Get screens success");
    }

    @Override
    public ResponseObject<?> getBrands() {
        return ResponseObject.successForward(brandRepository.getBrandComboboxResponse(), "Get brands success");
    }

    @Override
    public ResponseObject<?> getBatteries() {
        return ResponseObject.successForward(batteryRepository.getBatteryComboboxResponse(), "Get batteries success");
    }

    @Override
    public ResponseObject<?> getOperatingSystems() {
        return ResponseObject.successForward(operatingSystemRepository.getOperatingSystemComboboxResponse(), "Get operating_systems success");
    }
}
