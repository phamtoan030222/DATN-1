package com.sd20201.datn.core.admin.products.productdetail.service.impl;

import com.sd20201.datn.core.admin.products.product.model.request.ADQuickAddProductRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDVariantRequest;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDBatteryRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDBrandRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDCPURepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDColorRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDGPURepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDHardDriveRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDImeiRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDMaterialRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDOperatingSystemRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDProductDetailRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDProductRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDRAMRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDScreenRepository;
import com.sd20201.datn.core.admin.products.productdetail.service.ADProductDetailService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.core.common.cloudinary.model.response.CloudinaryResponse;
import com.sd20201.datn.core.common.cloudinary.service.CloudinaryService;
import com.sd20201.datn.entity.Battery;
import com.sd20201.datn.entity.Brand;
import com.sd20201.datn.entity.CPU;
import com.sd20201.datn.entity.Color;
import com.sd20201.datn.entity.GPU;
import com.sd20201.datn.entity.HardDrive;
import com.sd20201.datn.entity.IMEI;
import com.sd20201.datn.entity.Material;
import com.sd20201.datn.entity.OperatingSystem;
import com.sd20201.datn.entity.Product;
import com.sd20201.datn.entity.ProductDetail;
import com.sd20201.datn.entity.RAM;
import com.sd20201.datn.entity.Screen;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.TechnolyCharging;
import com.sd20201.datn.infrastructure.constant.TypeBattery;
import com.sd20201.datn.infrastructure.constant.TypeScreenResolution;
import com.sd20201.datn.repository.ImageProductRepository;
import com.sd20201.datn.utils.FileUploadUtil;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
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

    private final CloudinaryService cloudinaryService;

    private final ImageProductRepository imageProductRepository;

    private final ADPDOperatingSystemRepository operatingSystemRepository;

    private final ADPDScreenRepository screenRepository;

    private final ADPDBatteryRepository batteryRepository;

    private final ADPDBrandRepository brandRepository;

    @Override
    public ResponseObject<?> getProductDetails(ADPDProductDetailRequest request) {
        return ResponseObject.successForward(
                PageableObject.of(productDetailRepository.getProductDetails(Helper.createPageable(request), request)),
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
        Optional<ProductDetail> productDetailOptional = productDetailRepository.findByCode(request.getCode());
        if (productDetailOptional.isPresent())
            return ResponseObject.errorForward("Product detail code already exist", HttpStatus.CONFLICT);

        Optional<Product> productOptional = productRepository.findById(request.getIdProduct());
        if (productOptional.isEmpty()) return ResponseObject.errorForward("Product not found", HttpStatus.NOT_FOUND);
        {
        }

        Optional<RAM> ramOptional = ramRepository.findById(request.getIdRAM());
        if (ramOptional.isEmpty()) return ResponseObject.errorForward("RAM not found", HttpStatus.NOT_FOUND);

        Optional<Material> materialOptional = materialRepository.findById(request.getIdMaterial());
        if (materialOptional.isEmpty()) return ResponseObject.errorForward("Material not found", HttpStatus.NOT_FOUND);

        Optional<CPU> cpuOptional = cpuRepository.findById(request.getIdCPU());
        if (cpuOptional.isEmpty()) return ResponseObject.errorForward("CPU not found", HttpStatus.NOT_FOUND);

        Optional<GPU> gpuOptional = gpuRepository.findById(request.getIdGPU());
        if (gpuOptional.isEmpty()) return ResponseObject.errorForward("GPU not found", HttpStatus.NOT_FOUND);

        Optional<Color> colorOptional = colorRepository.findById(request.getIdColor());
        if (colorOptional.isEmpty()) return ResponseObject.errorForward("Color not found", HttpStatus.NOT_FOUND);

        Optional<HardDrive> hardDriveOptional = hardDriveRepository.findById(request.getIdHardDrive());
        if (hardDriveOptional.isEmpty())
            return ResponseObject.errorForward("HardDrive not found", HttpStatus.NOT_FOUND);

        ProductDetail productDetail = new ProductDetail();

        productDetail.setProduct(productOptional.get());
        productDetail.setRam(ramOptional.get());
        productDetail.setMaterial(materialOptional.get());
        productDetail.setHardDrive(hardDriveOptional.get());
        productDetail.setCpu(cpuOptional.get());
        productDetail.setGpu(gpuOptional.get());
        productDetail.setPrice(BigDecimal.valueOf(request.getPrice()));
        productDetail.setDescription(request.getDescription());
        productDetail.setColor(colorOptional.get());

        return ResponseObject.successForward(productDetailRepository.save(productDetail), "Create product success");
    }

    @Override
    public ResponseObject<?> update(ADPDProductDetailCreateUpdateRequest request) {
        Optional<ProductDetail> productDetailOptional = productDetailRepository.findById(request.getId());
        if (productDetailOptional.isEmpty())
            return ResponseObject.errorForward("Product detail not found", HttpStatus.NOT_FOUND);

        ProductDetail productDetail = productDetailOptional.get();
        if (!productDetail.getGpu().getId().equals(request.getIdGPU())) {
            Optional<GPU> gpuOptional = gpuRepository.findById(request.getIdGPU());
            if (gpuOptional.isEmpty()) return ResponseObject.errorForward("GPU not found", HttpStatus.NOT_FOUND);

            productDetail.setGpu(gpuOptional.get());
        }

        if (!productDetail.getCpu().getId().equals(request.getIdCPU())) {
            Optional<CPU> cpuOptional = cpuRepository.findById(request.getIdCPU());
            if (cpuOptional.isEmpty()) return ResponseObject.errorForward("CPU not found", HttpStatus.NOT_FOUND);

            productDetail.setCpu(cpuOptional.get());
        }

        if (!productDetail.getRam().getId().equals(request.getIdRAM())) {
            Optional<RAM> ramOptional = ramRepository.findById(request.getIdRAM());
            if (ramOptional.isEmpty()) return ResponseObject.errorForward("RAM not found", HttpStatus.NOT_FOUND);

            productDetail.setRam(ramOptional.get());
        }

        if (!productDetail.getMaterial().getId().equals(request.getIdMaterial())) {
            Optional<Material> materialOptional = materialRepository.findById(request.getIdMaterial());
            if (materialOptional.isEmpty())
                return ResponseObject.errorForward("Material not found", HttpStatus.NOT_FOUND);

            productDetail.setMaterial(materialOptional.get());
        }

        if (!productDetail.getColor().getId().equals(request.getIdMaterial())) {
            Optional<Color> colorOptional = colorRepository.findById(request.getIdColor());
            if (colorOptional.isEmpty()) return ResponseObject.errorForward("Color not found", HttpStatus.NOT_FOUND);

            productDetail.setColor(colorOptional.get());
        }

        if (!productDetail.getHardDrive().getId().equals(request.getIdHardDrive())) {
            Optional<HardDrive> hardDriveOptional = hardDriveRepository.findById(request.getIdHardDrive());
            if (hardDriveOptional.isEmpty())
                return ResponseObject.errorForward("HardDrive not found", HttpStatus.NOT_FOUND);

            productDetail.setHardDrive(hardDriveOptional.get());
        }

        productDetail.setPrice(BigDecimal.valueOf(request.getPrice()));
        productDetail.setDescription(request.getDescription());

        return ResponseObject.successForward(productDetailRepository.save(productDetail), "Update product success");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseObject<?> createVariant(String idProduct, ADPDVariantRequest variant, List<MultipartFile> images) {
        Optional<Product> optionalProduct = productRepository.findById(idProduct);

        if (optionalProduct.isEmpty()) return ResponseObject.errorForward("Product not found", HttpStatus.NOT_FOUND);

        Product product = optionalProduct.get();

        Optional<RAM> ramOptional = ramRepository.findById(variant.getIdRAM());
        if (ramOptional.isEmpty()) return ResponseObject.errorForward("RAM not found", HttpStatus.NOT_FOUND);

        Optional<Material> materialOptional = materialRepository.findById(variant.getIdMaterial());
        if (materialOptional.isEmpty())
            return ResponseObject.errorForward("Material not found", HttpStatus.NOT_FOUND);

        Optional<CPU> cpuOptional = cpuRepository.findById(variant.getIdCPU());
        if (cpuOptional.isEmpty()) return ResponseObject.errorForward("CPU not found", HttpStatus.NOT_FOUND);

        Optional<GPU> gpuOptional = gpuRepository.findById(variant.getIdGPU());
        if (gpuOptional.isEmpty()) return ResponseObject.errorForward("GPU not found", HttpStatus.NOT_FOUND);

        Optional<Color> colorOptional = colorRepository.findById(variant.getIdColor());
        if (colorOptional.isEmpty()) return ResponseObject.errorForward("Color not found", HttpStatus.NOT_FOUND);

        Optional<HardDrive> hardDriveOptional = hardDriveRepository.findById(variant.getIdHardDrive());
        if (hardDriveOptional.isEmpty())
            return ResponseObject.errorForward("HardDrive not found", HttpStatus.NOT_FOUND);

        ProductDetail productDetail = new ProductDetail();

        productDetail.setCode(Helper.generateCodeProductDetail());
        productDetail.setName(productDetail.getCode());
        productDetail.setProduct(product);
        productDetail.setRam(ramOptional.get());
        productDetail.setMaterial(materialOptional.get());
        productDetail.setHardDrive(hardDriveOptional.get());
        productDetail.setCpu(cpuOptional.get());
        productDetail.setGpu(gpuOptional.get());
        productDetail.setPrice(BigDecimal.valueOf(variant.getPrice()));
        productDetail.setColor(colorOptional.get());

        productDetailRepository.save(productDetail);

        List<String> imeiVariant = variant.getImei();

        for (String imeiValue : imeiVariant) {
            Optional<IMEI> imeiOptional = imeiRepository.findByCode(imeiValue);
            if (imeiOptional.isPresent())
                return ResponseObject.errorForward("Imei duplicated", HttpStatus.NOT_FOUND);

            IMEI imei = new IMEI();
            imei.setCode(imeiValue);
            imei.setName(imeiValue);
            imei.setProductDetail(productDetail);

            imeiRepository.save(imei);
        }

        productDetail = productDetailRepository.save(productDetail);

        MultipartFile image = images.get(0);
        final String idProductDetail = productDetail.getId();
        CloudinaryResponse cloudinaryResponse = uploadImage(image);
        log.info("{}", cloudinaryResponse);
        Optional<ProductDetail> optionalProductDetail = productDetailRepository.findById(idProductDetail);

        if (optionalProductDetail.isPresent()) {
            ProductDetail productDetailEntity = optionalProductDetail.get();

            productDetailEntity.setCloudinaryImageId(cloudinaryResponse.getPublicId());
            productDetailEntity.setUrlImage(cloudinaryResponse.getUrl());

            productDetailRepository.save(productDetailEntity);
            log.info("save image product detail");
        }

        return ResponseObject.successForward(idProductDetail, "Create variant success");
    }

    private CloudinaryResponse uploadImage(MultipartFile image) {
        FileUploadUtil.assertAllowed(image, FileUploadUtil.IMAGE_PATTERN);
        String filename = FileUploadUtil.getFilename(image.getOriginalFilename());

        return cloudinaryService.upload(image, filename);
    }

    @Override
    public ResponseObject<?> isIMEIExist(List<String> ids) {
        return ResponseObject.successForward(imeiRepository.findByCode(ids), "OKE");
    }

    @Override
    public ResponseObject<?> quickAddPropertiesProduct(ADQuickAddProductRequest request ) {
        switch (request.getType()) {
            case CPU -> {
                CPU cpu = new CPU();

                cpu.setName(request.getNameProperty());

                cpu = cpuRepository.save(cpu);
                return ResponseObject.successForward(cpu.getId(),"Quick add CPU success");
            }

            case BRAND -> {
                Brand brand = new Brand();
                brand.setName(request.getNameProperty());
                brand = brandRepository.save(brand);
                return ResponseObject.successForward(brand.getId(),"Quick add brand success");
            }

            case BATTERY -> {
                Battery battery = new Battery();

                battery.setName(request.getNameProperty());
                battery.setTechnolyCharging(TechnolyCharging.STANDARD);
                battery.setTypeBattery(TypeBattery.LI_ION);

                battery = batteryRepository.save(battery);
                return ResponseObject.successForward(battery.getId(),"Quick add battery success");
            }

            case SCREEN -> {
                Screen screen = new Screen();

                screen.setName(request.getNameProperty());
                screen.setResolution(TypeScreenResolution.HD);

                screen = screenRepository.save(screen);
                return ResponseObject.successForward(screen.getId(),"Quick add screen success");
            }

            case OPERATING_SYSTEM -> {
                OperatingSystem os = new OperatingSystem();

                os.setName(request.getNameProperty());

                os = operatingSystemRepository.save(os);
                return ResponseObject.successForward(os.getId(),"Quick add operating system success");
            }

            case COLOR -> {
                Color color = new Color();

                color.setName(request.getNameProperty());

                color = colorRepository.save(color);
                return ResponseObject.successForward(color.getId(),"Quick add color success");
            }

            case GPU -> {
                GPU gpu = new GPU();

                gpu.setName(request.getNameProperty());

                gpu = gpuRepository.save(gpu);
                return ResponseObject.successForward(gpu.getId(),"Quick add gpu success");
            }

            case MATERIAL -> {
                Material material = new Material();
                material.setName(request.getNameProperty());
                material = materialRepository.save(material);
                return ResponseObject.successForward(material.getId(),"Quick material CPU success");
            }

            case RAM -> {
                RAM ram = new RAM();
                ram.setName(request.getNameProperty());
                ram = ramRepository.save(ram);
                return ResponseObject.successForward(ram.getId(),"Quick add ram success");
            }

            case HARD_DRIVE -> {
                HardDrive hardDrive = new HardDrive();
                hardDrive.setName(request.getNameProperty());
                hardDrive = hardDriveRepository.save(hardDrive);
                return ResponseObject.successForward(hardDrive.getId(),"Quick add hard drive success");
            }
        }

        return ResponseObject.errorForward("Error", HttpStatus.NOT_FOUND);
    }

    @Override
    public ResponseObject<?> getMinMaxPrice() {
        return ResponseObject.successForward(productDetailRepository.findPriceMinMax(), "OKE");
    }

    @Override
    public ResponseObject<?> getImeiProductDetail(String idProductDetail) {
        return ResponseObject.successForward(imeiRepository.findByIdProductDetail(idProductDetail), "OKE");
    }

    @Override
    public ResponseObject<?> changeStatusImei(String idImei) {
        return imeiRepository.findById(idImei)
                .map(imei -> {
                    imei.setStatus(imei.getStatus() == EntityStatus.ACTIVE ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
                    imeiRepository.save(imei);
                    return ResponseObject.successForward(imei.getId(), "Update imei success");
                })
                .orElse(ResponseObject.errorForward("Update imei failure", HttpStatus.NOT_FOUND));
    }
}
