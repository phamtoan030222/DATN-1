package com.sd20201.datn.infrastructure.constant;

public class MappingConstants {

    /* API BASE ROLE */
    public static final String MANAGE = "/manage";
    public static final String STAFF = "/staff";
    public static final String CUSTOMER = "/customer";

    /* API VERSION PREFIX */
    public static final String API_VERSION_PREFIX = "/api/v1";

    /* AUTHENTICATION */
    public static final String API_AUTH_PREFIX = API_VERSION_PREFIX + "/auth";
    public static final String API_AUTH_REGISTER = API_AUTH_PREFIX + "/register";

    /* API COMMON */
    public static final String API_COMMON = API_VERSION_PREFIX + "/common";
    public static final String API_LOGIN = API_VERSION_PREFIX + "/login";

    /* API FOR MANAGE */

    public static final String ADMIN = "/admin";

    /* API PRODUCTS */
    public static final String API_ADMIN_PREFIX = API_VERSION_PREFIX + ADMIN;
    public static final String API_STAFF_PREFIX = API_VERSION_PREFIX + STAFF;
    public static final String API_CUSTOMER_PREFIX = API_VERSION_PREFIX + CUSTOMER;

    public static final String API_ADMIN_PREFIX_GEO = API_VERSION_PREFIX + "/geo";
    public static final String API_ADMIN_PREFIX_PRODUCTS = API_ADMIN_PREFIX + "/products";
    public static final String API_ADMIN_PREFIX_PRODUCTS_COLOR = API_ADMIN_PREFIX_PRODUCTS + "/color";

    public static final String API_ADMIN_PREFIX_DISCOUNT = API_ADMIN_PREFIX + "/discounts";
    public static final String API_ADMIN_PREFIX_DISCOUNT_DISCOUNT = API_ADMIN_PREFIX_DISCOUNT + "/discount";
    public static final String API_ADMIN_PREFIX_DISCOUNT_DETAIL = API_ADMIN_PREFIX_DISCOUNT + "/detail";

    public static final String API_ADMIN_PREFIX_STATISTICS = API_ADMIN_PREFIX + "/statistics";

    public static final String API_ADMIN_PREFIX_DISCOUNT_VOUCHER = API_ADMIN_PREFIX_DISCOUNT + "/voucher";

    public static final String API_ADMIN_PREFIX_PRODUCTS_RAM = API_ADMIN_PREFIX_PRODUCTS + "/ram";
    public static final String API_ADMIN_PREFIX_PRODUCTS_HARDDRIVE = API_ADMIN_PREFIX_PRODUCTS + "/harddrive";
    public static final String API_ADMIN_PREFIX_PRODUCTS_MATERIAL = API_ADMIN_PREFIX_PRODUCTS + "/material";
    public static final String API_ADMIN_PREFIX_PRODUCTS_CPU = API_ADMIN_PREFIX_PRODUCTS + "/cpu";
    public static final String API_ADMIN_PREFIX_PRODUCTS_GPU = API_ADMIN_PREFIX_PRODUCTS + "/gpu";
    public static final String API_ADMIN_PREFIX_PRODUCTS_SCREEN = API_ADMIN_PREFIX_PRODUCTS + "/screen";
    public static final String API_ADMIN_PREFIX_PRODUCTS_DETAIL = API_ADMIN_PREFIX_PRODUCTS + "/product-detail";
    public static final String API_ADMIN_PREFIX_PRODUCTS_BRAND = API_ADMIN_PREFIX_PRODUCTS + "/brand";
    public static final String API_ADMIN_PREFIX_PRODUCTS_OPERATING = API_ADMIN_PREFIX_PRODUCTS + "/operating";
    public static final String API_ADMIN_PREFIX_PRODUCTS_BATTERY = API_ADMIN_PREFIX_PRODUCTS + "/battery";

    public static final String API_ADMIN_PREFIX_STAFF = API_ADMIN_PREFIX + "/staff";
    public static final String API_ADMIN_PREFIX_CUSTOMERS = API_ADMIN_PREFIX + "/customers";
    public static final String API_ADMIN_PREFIX_CUSTOMER_ADDRESSES = API_ADMIN_PREFIX_CUSTOMERS + "/{customerId}/addresses";

    public static final String API_ADMIN_BAN_HANG = API_ADMIN_PREFIX + "/ban-hang";
    public static final String API_ADMIN_HOA_DON = API_ADMIN_PREFIX + "/hoa-don";


    // CLIENT
    public static final String API_CUSTOMER_PREFIX_PRODUCTS = API_CUSTOMER_PREFIX + "/products";

    public static final String API_CUSTOMER_PREFIX_CUSTOMERS = API_CUSTOMER_PREFIX + "/customers";
    public static final String API_CUSTOMER_PREFIX_CUSTOMER_ADDRESSES = API_CUSTOMER_PREFIX_CUSTOMERS + "/{customerId}/addresses";

    public static final String API_CUSTOMER_BAN_HANG = API_CUSTOMER_PREFIX + "/ban-hang";
    public static final String API_CUSTOMER_HOA_DON = API_CUSTOMER_PREFIX + "/hoa-don";

    public static final String API_CUSTOMER_PREFIX_DISCOUNT = API_CUSTOMER_PREFIX + "/discounts";
    public static final String API_CUSTOMER_PREFIX_DISCOUNT_DISCOUNT = API_CUSTOMER_PREFIX_DISCOUNT + "/discount";
    public static final String API_CUSTOMER_PREFIX_DISCOUNT_DETAIL = API_CUSTOMER_PREFIX_DISCOUNT + "/detail";
    public static final String API_CUSTOMER_PREFIX_DISCOUNT_VOUCHER = API_CUSTOMER_PREFIX_DISCOUNT + "/voucher";

    public static final String API_CUSTOMER_PREFIX_PRODUCTS_DETAIL = API_CUSTOMER_PREFIX_PRODUCTS + "/product-detail";

    public static final String API_ORDER_ONLINE = API_VERSION_PREFIX + "/order-online";
    public static final String API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL = API_ORDER_ONLINE + "/product-detail";
    public static final String API_PREFIX_ORDER_ONLINE_PRODUCT = API_ORDER_ONLINE + "/product";
    public static final String API_PREFIX_ORDER_ONLINE_VOUCHER = API_ORDER_ONLINE + "/voucher";

    public static final String API_ADMIN_PREFIX_SCHEDULES = API_ADMIN_PREFIX + "/schedules"; // /api/v1/admin/schedules
    public static final String API_ADMIN_PREFIX_SHIFT_HISTORY = API_ADMIN_PREFIX + "/shift-history"; // /api/v1/admin/shift-history

    public static final String API_STAFF_PREFIX_SHIFTS = API_STAFF_PREFIX + "/shifts";
    // Thêm dòng này để định nghĩa đường dẫn /api/v1/shifts
    public static final String API_SHIFTS = API_VERSION_PREFIX + "/shifts";
    //AI
    public static final String API_CHAT_AI = API_VERSION_PREFIX + "/chat";

}
