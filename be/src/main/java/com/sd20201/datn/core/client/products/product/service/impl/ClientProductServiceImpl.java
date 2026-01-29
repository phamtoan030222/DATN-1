package com.sd20201.datn.core.client.products.product.service.impl;

import com.sd20201.datn.core.client.products.product.model.request.ClientProductCreateUpdateRequest;
import com.sd20201.datn.core.client.products.product.model.request.ClientProductRequest;
import com.sd20201.datn.core.client.products.product.repository.ClientPRBatteryRepository;
import com.sd20201.datn.core.client.products.product.repository.ClientPRBrandRepository;
import com.sd20201.datn.core.client.products.product.repository.ClientPROperatingSystemRepository;
import com.sd20201.datn.core.client.products.product.repository.ClientPRScreenRepository;
import com.sd20201.datn.core.client.products.product.repository.ClientProductRepository;
import com.sd20201.datn.core.client.products.product.service.ClientProductService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.core.common.cloudinary.model.response.CloudinaryResponse;
import com.sd20201.datn.core.common.cloudinary.service.CloudinaryService;
import com.sd20201.datn.entity.Battery;
import com.sd20201.datn.entity.Brand;
import com.sd20201.datn.entity.ImageProduct;
import com.sd20201.datn.entity.OperatingSystem;
import com.sd20201.datn.entity.Product;
import com.sd20201.datn.entity.Screen;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.repository.ImageProductRepository;
import com.sd20201.datn.utils.FileUploadUtil;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientProductServiceImpl implements ClientProductService {

    private final ClientProductRepository productRepository;

    private final ClientPRScreenRepository screenRepository;

    private final ClientPRBrandRepository brandRepository;

    private final ClientPROperatingSystemRepository operatingSystemRepository;

    private final ClientPRBatteryRepository batteryRepository;

    private final CloudinaryService cloudinaryService;

    private final ImageProductRepository imageProductRepository;

    @Override
    public ResponseObject<?> getProducts(ClientProductRequest request) {
        Long currentTime = System.currentTimeMillis();

        return ResponseObject.successForward(
                PageableObject.of(productRepository.getProducts(Helper.createPageable(request), request, currentTime)),
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
    public ResponseObject<?> modify(ClientProductCreateUpdateRequest request, List<MultipartFile> images) {
        return request.getId() == null || request.getId().isEmpty() ? create(request, images) : update(request);
    }

    public ResponseObject<?> create(ClientProductCreateUpdateRequest request, List<MultipartFile> images) {

        Optional<Product> optionalProduct = productRepository.findByCode(request.getCode());
        if (optionalProduct.isPresent())
            return ResponseObject.errorForward("Create product failure !!! Duplicate code", HttpStatus.CONFLICT);

        Optional<Screen> optionalScreen = screenRepository.findById(request.getIdScreen());
        if (optionalScreen.isEmpty()) return ResponseObject.errorForward("Screen not found", HttpStatus.NOT_FOUND);

        Optional<Battery> optionalBattery = batteryRepository.findById(request.getIdBattery());
        if (optionalBattery.isEmpty()) return ResponseObject.errorForward("Battery not found", HttpStatus.NOT_FOUND);

        Optional<OperatingSystem> optionalOS = operatingSystemRepository.findById(request.getIdOperatingSystem());
        if (optionalOS.isEmpty()) return ResponseObject.errorForward("OperatingSystem not found", HttpStatus.NOT_FOUND);

        Optional<Brand> optionalBrand = brandRepository.findById(request.getIdBrand());
        if (optionalBrand.isEmpty()) return ResponseObject.errorForward("Brand not found", HttpStatus.NOT_FOUND);

        Product product = new Product();

        product.setCode(request.getCode());
        product.setName(request.getName());
        product.setScreen(optionalScreen.get());
        product.setBrand(optionalBrand.get());
        product.setOperatingSystem(optionalOS.get());
        product.setBattery(optionalBattery.get());

        final Product productEntity = productRepository.save(product);

        AtomicInteger count = new AtomicInteger(1);
        for (MultipartFile imageProduct : images) {
            CloudinaryResponse uploadImageMain = uploadImage(imageProduct);

            ImageProduct imageMainProduct = new ImageProduct();

            imageMainProduct.setProduct(productEntity);
            imageMainProduct.setIndex(count.getAndIncrement());
            imageMainProduct.setCloudinaryImageId(uploadImageMain.getPublicId());
            imageMainProduct.setUrl(uploadImageMain.getUrl());

            imageProductRepository.save(imageMainProduct);
            log.info("save image product: {}", uploadImageMain);
        }

        return ResponseObject.successForward(product.getId(), "Create product success");
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

    private CloudinaryResponse uploadImage(MultipartFile image) {
        FileUploadUtil.assertAllowed(image, FileUploadUtil.IMAGE_PATTERN);
        String filename = FileUploadUtil.getFilename(image.getOriginalFilename());

        return cloudinaryService.upload(image, filename);
    }

    @Override
    public ResponseObject<?> update(ClientProductCreateUpdateRequest request) {
        Optional<Product> optionalProduct = productRepository.findById(request.getId());
        if (optionalProduct.isEmpty()) return ResponseObject.errorForward("Product not found", HttpStatus.NOT_FOUND);

        Product product = optionalProduct.get();

        if (!product.getScreen().getId().equals(request.getIdScreen())) {
            Optional<Screen> optionalScreen = screenRepository.findById(request.getIdScreen());
            if (optionalScreen.isEmpty()) return ResponseObject.errorForward("Screen not found", HttpStatus.NOT_FOUND);

            product.setScreen(optionalScreen.get());
        }

        if (!product.getBattery().getId().equals(request.getIdBattery())) {
            Optional<Battery> optionalBattery = batteryRepository.findById(request.getIdBattery());
            if (optionalBattery.isEmpty())
                return ResponseObject.errorForward("Battery not found", HttpStatus.NOT_FOUND);

            product.setBattery(optionalBattery.get());
        }

        if (!product.getOperatingSystem().getId().equals(request.getIdOperatingSystem())) {
            Optional<OperatingSystem> optionalOS = operatingSystemRepository.findById(request.getIdOperatingSystem());
            if (optionalOS.isEmpty())
                return ResponseObject.errorForward("OperatingSystem not found", HttpStatus.NOT_FOUND);

            product.setOperatingSystem(optionalOS.get());
        }

        if (!product.getBrand().getId().equals(request.getIdBrand())) {
            Optional<Brand> optionalBrand = brandRepository.findById(request.getIdBrand());
            if (optionalBrand.isEmpty()) return ResponseObject.errorForward("Brand not found", HttpStatus.NOT_FOUND);

            product.setBrand(optionalBrand.get());
        }

        product.setName(request.getName());

        return ResponseObject.successForward(productRepository.save(product).getId(), "Update product success");
    }

    @Override
    public ResponseObject<?> getProductsCombobox() {
        return ResponseObject.successForward(productRepository.getPropertyCombobox(), "OKE");
    }
}
