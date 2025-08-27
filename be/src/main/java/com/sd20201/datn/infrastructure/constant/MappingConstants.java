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

    /* API COMMON */
    public static final String API_COMMON = API_VERSION_PREFIX + "/common";
    public static final String API_LOGIN = API_VERSION_PREFIX + "/login";

    /* API FOR MANAGE */

    public static final String ADMIN = "/admin";


    /* API PRODUCTS */
    public static final String API_ADMIN_PREFIX = API_VERSION_PREFIX + ADMIN;
    public static final String API_ADMIN_PREFIX_PRODUCTS = API_ADMIN_PREFIX + "/products";
    public static final String API_ADMIN_PREFIX_PRODUCTS_COLOR = API_ADMIN_PREFIX_PRODUCTS + "/color";

    public static final String API_ADMIN_PREFIX_PRODUCTS_RAM = API_ADMIN_PREFIX_PRODUCTS + "/ram";
    public static final String API_ADMIN_PREFIX_PRODUCTS_HARDDRIVE = API_ADMIN_PREFIX_PRODUCTS + "/harddrive";
    public static final String API_ADMIN_PREFIX_PRODUCTS_MATERIAL =  API_ADMIN_PREFIX_PRODUCTS + "/material";
    public static final String API_ADMIN_PREFIX_PRODUCTS_CPU = API_ADMIN_PREFIX_PRODUCTS + "/cpu";
    public static final String API_ADMIN_PREFIX_PRODUCTS_GPU = API_ADMIN_PREFIX_PRODUCTS + "/gpu";
    public static final String API_ADMIN_PREFIX_PRODUCTS_SCREEN = API_ADMIN_PREFIX_PRODUCTS + "/screen";
    public static final String API_ADMIN_PREFIX_PRODUCTS_DETAIL = API_ADMIN_PREFIX_PRODUCTS + "/product-detail";
    public static final String API_ADMIN_PREFIX_PRODUCTS_BRAND =  API_ADMIN_PREFIX_PRODUCTS + "/brand";
    public static final String API_ADMIN_PREFIX_PRODUCTS_OPERATING =  API_ADMIN_PREFIX_PRODUCTS + "/operating";
    public static final String API_ADMIN_PREFIX_PRODUCTS_BATTERY =   API_ADMIN_PREFIX_PRODUCTS + "/battery";
}
