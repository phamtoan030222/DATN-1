package com.sd20201.datn.core.admin.products.productdetail.service.impl;

import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDCPURepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDColorRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDGPURepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDHardDriveRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDImeiRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDMaterialRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDProductDetailRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDProductRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDRAMRepository;
import com.sd20201.datn.core.admin.products.productdetail.service.ADProductDetailService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.CPU;
import com.sd20201.datn.entity.Color;
import com.sd20201.datn.entity.GPU;
import com.sd20201.datn.entity.HardDrive;
import com.sd20201.datn.entity.Material;
import com.sd20201.datn.entity.Product;
import com.sd20201.datn.entity.ProductDetail;
import com.sd20201.datn.entity.RAM;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ADProductDetailServiceImpl implements ADProductDetailService {

    private final ADPDProductDetailRepository productDetailRepository;

    private final ADPDHardDriveRepository hardDriveRepository;

    private final ADPDColorRepository colorRepository;

    private final ADPDImeiRepository imeiRepository;

    private final ADPDRAMRepository ramRepository;

    private final ADPDMaterialRepository materialRepository;

    private final ADPDCPURepository cpuRepository;

    private final ADPDGPURepository gpuRepository;

    private final ADPDProductRepository productRepository;

    @Override
    public ResponseObject<?> getProductDetails(ADPDProductDetailRequest request) {
        if(request.getIdProduct() == null) return ResponseObject.errorForward("Required id product", HttpStatus.BAD_GATEWAY);

        return ResponseObject.successForward(
                PageableObject.of(productDetailRepository.getProducts(Helper.createPageable(request), request)),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getColors() {
        return ResponseObject.successForward(colorRepository.getColors(), "OKE");
    }

    @Override
    public ResponseObject<?> getRAMs() {
        return ResponseObject.successForward(ramRepository.getRAMs(), "OKE");
    }

    @Override
    public ResponseObject<?> getCPUs() {
        return ResponseObject.successForward(cpuRepository.getCPUs(), "OKE");
    }

    @Override
    public ResponseObject<?> getHardDrivers() {
        return ResponseObject.successForward(hardDriveRepository.getHardDrives(), "OKE");
    }

    @Override
    public ResponseObject<?> getMaterials() {
        return ResponseObject.successForward(materialRepository.getMaterials(), "OKE");
    }

    @Override
    public ResponseObject<?> getGPUs() {
        return ResponseObject.successForward(gpuRepository.getGPUs(), "OKE");
    }

    @Override
    public ResponseObject<?> getDetail(String id) {
        return productDetailRepository.getProductById(id)
                .map(data -> ResponseObject.successForward(data, "Fetch product detail success"))
                .orElse(ResponseObject.errorForward("Fetch product detail failure", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> changeStatus(String id) {
        return productDetailRepository.findById(id)
                .map(productDetail -> {
                    productDetail.setStatus(productDetail.getStatus() == EntityStatus.ACTIVE ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
                    productDetailRepository.save(productDetail);
                    return ResponseObject.successForward(productDetail, "Update product detail success");
                })
                .orElse(ResponseObject.errorForward("Update product detail failure", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> modify(ADPDProductDetailCreateUpdateRequest request) {
        return request.getId() == null || request.getId().isEmpty() ? create(request) : update(request);
    }

    private ResponseObject<?> create(ADPDProductDetailCreateUpdateRequest request) {

        Optional<ProductDetail> productDetailOptional = productDetailRepository.findByCode(request.getCode());
        if(productDetailOptional.isPresent()) return ResponseObject.errorForward("Product detail code already exist", HttpStatus.CONFLICT);

        Optional<Product> productOptional = productRepository.findById(request.getIdProduct());
        if(productOptional.isEmpty()) return ResponseObject.errorForward("Product not found", HttpStatus.NOT_FOUND); {}

        Optional<RAM> ramOptional = ramRepository.findById(request.getIdRAM());
        if (ramOptional.isEmpty()) return ResponseObject.errorForward("RAM not found", HttpStatus.NOT_FOUND);

        Optional<Material> materialOptional = materialRepository.findById(request.getIdMaterial());
        if(materialOptional.isEmpty()) return ResponseObject.errorForward("Material not found", HttpStatus.NOT_FOUND);

        Optional<CPU> cpuOptional = cpuRepository.findById(request.getIdCPU());
        if (cpuOptional.isEmpty()) return ResponseObject.errorForward("CPU not found", HttpStatus.NOT_FOUND);

        Optional<GPU> gpuOptional = gpuRepository.findById(request.getIdGPU());
        if (gpuOptional.isEmpty()) return ResponseObject.errorForward("GPU not found", HttpStatus.NOT_FOUND);

        List<Color> colors = new ArrayList<>();
        for(String color : request.getIdColor()) {
           Optional<Color> colorOptional = colorRepository.findById(color);
           if(colorOptional.isEmpty()) return ResponseObject.errorForward("Color not found", HttpStatus.NOT_FOUND);
           colors.add(colorOptional.get());
        }


        Optional<HardDrive> hardDriveOptional = hardDriveRepository.findById(request.getIdHardDrive());
        if(hardDriveOptional.isEmpty()) return ResponseObject.errorForward("HardDrive not found", HttpStatus.NOT_FOUND);

        ProductDetail productDetail = new ProductDetail();

        productDetail.setProduct(productOptional.get());
        productDetail.setRam(ramOptional.get());
        productDetail.setMaterial(materialOptional.get());
        productDetail.setHardDrive(hardDriveOptional.get());
        productDetail.setCpu(cpuOptional.get());
        productDetail.setGpu(gpuOptional.get());
        productDetail.setPrice(BigDecimal.valueOf(request.getPrice()));
        productDetail.setDescription(request.getDescription());

        List<ProductDetail> productDetails = new ArrayList<>();
        colors.forEach(color -> {
            try {
                ProductDetail pd = productDetail.clone();
                pd.setCode(Helper.generateCodeProductDetail());
                pd.setName(pd.getCode());
                pd.setColor(color);
                productDetails.add(pd);
            } catch (CloneNotSupportedException ex) {
                log.info("error clone object", ex);
            }
        });

        return ResponseObject.successForward(productDetailRepository.saveAll(productDetails), "Create product success");
    }

    private ResponseObject<?> update(ADPDProductDetailCreateUpdateRequest request) {
        Optional<ProductDetail> productDetailOptional = productDetailRepository.findById(request.getId());
        if(productDetailOptional.isEmpty()) return ResponseObject.errorForward("Product detail not found", HttpStatus.NOT_FOUND);

        ProductDetail productDetail = productDetailOptional.get();
        if(!productDetail.getGpu().getId().equals(request.getIdGPU())) {
            Optional<GPU> gpuOptional = gpuRepository.findById(request.getIdGPU());
            if (gpuOptional.isEmpty()) return ResponseObject.errorForward("GPU not found", HttpStatus.NOT_FOUND);

            productDetail.setGpu(gpuOptional.get());
        }

        if(!productDetail.getCpu().getId().equals(request.getIdCPU())) {
            Optional<CPU> cpuOptional = cpuRepository.findById(request.getIdCPU());
            if (cpuOptional.isEmpty()) return ResponseObject.errorForward("CPU not found", HttpStatus.NOT_FOUND);

            productDetail.setCpu(cpuOptional.get());
        }

        if(!productDetail.getRam().getId().equals(request.getIdRAM())) {
            Optional<RAM> ramOptional = ramRepository.findById(request.getIdRAM());
            if (ramOptional.isEmpty()) return ResponseObject.errorForward("RAM not found", HttpStatus.NOT_FOUND);

            productDetail.setRam(ramOptional.get());
        }

        if(!productDetail.getMaterial().getId().equals(request.getIdMaterial())) {
            Optional<Material> materialOptional = materialRepository.findById(request.getIdMaterial());
            if (materialOptional.isEmpty()) return ResponseObject.errorForward("Material not found", HttpStatus.NOT_FOUND);

            productDetail.setMaterial(materialOptional.get());
        }

        if(request.getIdColor().size() == 1) {
            Optional<Color> colorOptional = colorRepository.findById(request.getIdColor().get(0));
            if (colorOptional.isEmpty()) return ResponseObject.errorForward("Color not found", HttpStatus.NOT_FOUND);

            productDetail.setColor(colorOptional.get());
        } else return ResponseObject.errorForward("Parameter color is not valid", HttpStatus.CONFLICT);

        if(!productDetail.getHardDrive().getId().equals(request.getIdHardDrive())) {
            Optional<HardDrive> hardDriveOptional = hardDriveRepository.findById(request.getIdHardDrive());
            if (hardDriveOptional.isEmpty()) return ResponseObject.errorForward("HardDrive not found", HttpStatus.NOT_FOUND);

            productDetail.setHardDrive(hardDriveOptional.get());
        }

        productDetail.setPrice(BigDecimal.valueOf(request.getPrice()));
        productDetail.setDescription(request.getDescription());

        return ResponseObject.successForward(productDetailRepository.save(productDetail), "Update product success");
    }
}
