export enum ProductPropertiesType {
    BRAND = 'BRAND',
    BATTERY = 'BATTERY',
    SCREEN = 'SCREEN',
    OPERATING_SYSTEM = 'OPERATING_SYSTEM',
    COLOR = 'COLOR',
    CPU = 'CPU',
    GPU = 'GPU',
    MATERIAL = 'MATERIAL',
    RAM = 'RAM',
    HARD_DRIVE = 'HARD_DRIVE'

}

export function translateProperty(type: ProductPropertiesType): string {
    switch (type) {
        case ProductPropertiesType.BRAND:
            return 'Thương hiệu';

        case ProductPropertiesType.BATTERY:
            return 'Pin';

        case ProductPropertiesType.SCREEN:
            return 'Màn hình';

        case ProductPropertiesType.OPERATING_SYSTEM:
            return 'Hệ điều hành';

        case ProductPropertiesType.COLOR:
            return 'Màu sắc';

        case ProductPropertiesType.CPU:
            return 'Bộ vi xử lý (CPU)';

        case ProductPropertiesType.GPU:
            return 'Card đồ họa (GPU)';

        case ProductPropertiesType.MATERIAL:
            return 'Chất liệu';

        case ProductPropertiesType.RAM:
            return 'Bộ nhớ RAM';

        case ProductPropertiesType.HARD_DRIVE:
            return 'Ổ cứng';

        default:
            return 'Không xác định';
    }
}

export function withCallbackOnEachProperty(
    type: ProductPropertiesType,
    callback: (type?: ProductPropertiesType) => void,
) {
    switch (type) {
        case ProductPropertiesType.BRAND:
            callback(type);
            break;
        case ProductPropertiesType.BATTERY:
            callback(type);
            break;
        case ProductPropertiesType.SCREEN:
            callback(type);
            break;
        case ProductPropertiesType.OPERATING_SYSTEM:
            callback(type);
            break;
        case ProductPropertiesType.COLOR:
            callback(type);
            break;
        case ProductPropertiesType.CPU:
            callback(type);
            break;
        case ProductPropertiesType.GPU:
            callback(type);
            break;
        case ProductPropertiesType.MATERIAL:
            callback(type);
            break;
        case ProductPropertiesType.RAM:
            callback(type);
            break;
        case ProductPropertiesType.HARD_DRIVE:
            callback(type);
            break;
        default:
            return
    }
}