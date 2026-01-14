package com.sd20201.datn.core.admin.products.productdetail.service.impl;

import com.sd20201.datn.core.admin.products.productdetail.model.request.ADQuickAddColorProductRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADQuickAddProductRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDVariantRequest;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDBatteryRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDBrandRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDCPURepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDColorRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDProductDetailDiscountRepository;
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
import com.sd20201.datn.infrastructure.constant.ProductPropertiesType;
import com.sd20201.datn.infrastructure.constant.TechnolyCharging;
import com.sd20201.datn.infrastructure.constant.TypeBattery;
import com.sd20201.datn.infrastructure.constant.TypeScreenResolution;
import com.sd20201.datn.infrastructure.exception.BusinessException;
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
import java.util.Map;
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

    private final ADPDProductDetailDiscountRepository productDetailDiscountRepository;

    @Override
    public ResponseObject<?> getProductDetails(ADPDProductDetailRequest request) {
        Long currentTime = System.currentTimeMillis();
        List<String> idCurrentDiscounts = productDetailDiscountRepository.getIdByDate(currentTime);

        if (!idCurrentDiscounts.isEmpty()) {
            return ResponseObject.successForward(
                    PageableObject.of(productDetailRepository.getProductDetailsDiscount(Helper.createPageable(request), request, idCurrentDiscounts)),
                    "OKE"
            );
        }

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
    public ResponseObject<?> createVariant(String idProduct, ADPDVariantRequest variant) {
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
        productDetail.setUrlImage(variant.getUrlImage());

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

        final String idProductDetail = productDetail.getId();

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
    public ResponseObject<?> quickAddPropertiesProduct(Map<String, ?> request) {
        String nameProperty = String.valueOf(request.get("nameProperty")).trim();

        if (nameProperty.isBlank()) return ResponseObject.errorForward("Name property must be not blank", HttpStatus.CONFLICT);

        ProductPropertiesType type = ProductPropertiesType.valueOf((String) request.get("type"));
        switch (type) {
            case CPU -> {
                Optional<CPU> cpuOptional = cpuRepository.findByName(nameProperty);
                if (cpuOptional.isPresent()) return ResponseObject.errorForward("CPU is exist", HttpStatus.CONFLICT);

                CPU cpu = new CPU();

                cpu.setName(nameProperty);

                cpu = cpuRepository.save(cpu);
                return ResponseObject.successForward(cpu.getId(), "Quick add CPU success");
            }

            case BRAND -> {
                Optional<Brand> brandOptional = brandRepository.findByName(nameProperty);
                if (brandOptional.isPresent())
                    return ResponseObject.errorForward("Brand is exist", HttpStatus.CONFLICT);

                Brand brand = new Brand();
                brand.setName(nameProperty);
                brand = brandRepository.save(brand);

                return ResponseObject.successForward(brand.getId(), "Quick add brand success");
            }

            case BATTERY -> {
                Optional<Battery> batteryOptional = batteryRepository.findByName(nameProperty);
                if (batteryOptional.isPresent())
                    return ResponseObject.errorForward("Battery is exist", HttpStatus.CONFLICT);

                Battery battery = new Battery();

                battery.setName(nameProperty);
                battery.setTechnolyCharging(TechnolyCharging.STANDARD);
                battery.setTypeBattery(TypeBattery.LI_ION);

                battery = batteryRepository.save(battery);
                return ResponseObject.successForward(battery.getId(), "Quick add battery success");
            }

            case SCREEN -> {
                Optional<Screen> screenOptional = screenRepository.findByName(nameProperty);
                if (screenOptional.isPresent())
                    return ResponseObject.errorForward("Screen is exist", HttpStatus.CONFLICT);

                Screen screen = new Screen();

                screen.setName(nameProperty);
                screen.setResolution(TypeScreenResolution.HD);

                screen = screenRepository.save(screen);
                return ResponseObject.successForward(screen.getId(), "Quick add screen success");
            }

            case OPERATING_SYSTEM -> {
                Optional<OperatingSystem> operatingSystemOptional = operatingSystemRepository.findByName(nameProperty);
                if (operatingSystemOptional.isPresent())
                    return ResponseObject.errorForward("OperatingSystem is exist", HttpStatus.CONFLICT);

                OperatingSystem os = new OperatingSystem();

                os.setName(nameProperty);

                os = operatingSystemRepository.save(os);
                return ResponseObject.successForward(os.getId(), "Quick add operating system success");
            }

            case COLOR -> {
                Optional<Color> colorOptional = colorRepository.findByName(nameProperty);
                if (colorOptional.isPresent())
                    return ResponseObject.errorForward("Color is exist", HttpStatus.CONFLICT);

                String hexCode = String.valueOf(request.get("hex")).trim().toUpperCase();
                Optional<Color> colorCodeOptional = colorRepository.findByCode(hexCode);
                if (colorCodeOptional.isPresent())
                    return ResponseObject.errorForward("Color hex is exist", HttpStatus.CONFLICT);

                Color color = new Color();

                color.setName(nameProperty);
                color.setCode(hexCode);

                color = colorRepository.save(color);
                return ResponseObject.successForward(color.getId(), "Quick add color success");
            }

            case GPU -> {
                Optional<GPU> gpuOptional = gpuRepository.findByName(nameProperty.toLowerCase());
                if (gpuOptional.isPresent()) return ResponseObject.errorForward("GPU is exist", HttpStatus.CONFLICT);

                GPU gpu = new GPU();

                gpu.setName(nameProperty);

                gpu = gpuRepository.save(gpu);
                return ResponseObject.successForward(gpu.getId(), "Quick add gpu success");
            }

            case MATERIAL -> {
                Optional<Material> materialOptional = materialRepository.findByName(nameProperty.toLowerCase());
                if (materialOptional.isPresent())
                    return ResponseObject.errorForward("Material is exist", HttpStatus.CONFLICT);

                Material material = new Material();
                material.setName(nameProperty);
                material = materialRepository.save(material);
                return ResponseObject.successForward(material.getId(), "Quick material CPU success");
            }

            case RAM -> {
                Optional<RAM> ramOptional = ramRepository.findByName(nameProperty.toLowerCase());
                if (ramOptional.isPresent()) return ResponseObject.errorForward("RAM is exist", HttpStatus.CONFLICT);

                RAM ram = new RAM();
                ram.setName(nameProperty);
                ram = ramRepository.save(ram);
                return ResponseObject.successForward(ram.getId(), "Quick add ram success");
            }

            case HARD_DRIVE -> {
                Optional<HardDrive> hardDriveOptional = hardDriveRepository.findByName(nameProperty.toLowerCase());
                if (hardDriveOptional.isPresent())
                    return ResponseObject.errorForward("HardDrive is exist", HttpStatus.CONFLICT);

                HardDrive hardDrive = new HardDrive();
                hardDrive.setName(nameProperty);
                hardDrive = hardDriveRepository.save(hardDrive);
                return ResponseObject.successForward(hardDrive.getId(), "Quick add hard drive success");
            }

            default -> {
                return ResponseObject.errorForward("Unknown operation", HttpStatus.CONFLICT);
            }
        }
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

    @Override
    public ResponseObject<?> saveImage(MultipartFile file) {
        CloudinaryResponse cloudinaryResponse = uploadImage(file);

        return ResponseObject.successForward(cloudinaryResponse, "Save image success");
    }
}
