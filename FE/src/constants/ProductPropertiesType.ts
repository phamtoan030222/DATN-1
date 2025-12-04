export enum ProductPropertiesType {
    Brand = 'Brand',
    Battery = 'Battery',
    Screen = 'Screen',
    OperatorSystem = 'OperatorSystem',
    Color = 'Color',
    CPU = 'CPU',
    GPU = 'GPU',
    Material = 'Material',
    RAM = 'RAM',
    HardDrive = 'HardDrive'
}

export function translateProperty(type: ProductPropertiesType): string {
  switch (type) {
    case ProductPropertiesType.Brand:
      return 'Thương hiệu';

    case ProductPropertiesType.Battery:
      return 'Pin';

    case ProductPropertiesType.Screen:
      return 'Màn hình';

    case ProductPropertiesType.OperatorSystem:
      return 'Hệ điều hành';

    case ProductPropertiesType.Color:
      return 'Màu sắc';

    case ProductPropertiesType.CPU:
      return 'Bộ vi xử lý (CPU)';

    case ProductPropertiesType.GPU:
      return 'Card đồ họa (GPU)';

    case ProductPropertiesType.Material:
      return 'Chất liệu';

    case ProductPropertiesType.RAM:
      return 'Bộ nhớ RAM';

    case ProductPropertiesType.HardDrive:
      return 'Ổ cứng';

    default:
      return 'Không xác định';
  }
}