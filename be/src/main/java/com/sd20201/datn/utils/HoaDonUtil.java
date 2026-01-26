package com.sd20201.datn.utils;

import java.util.Random;

public class HoaDonUtil {

    private static final String PREFIX = "HD";
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int LENGTH = 6;
    private static final Random RANDOM = new Random();

    public static String genMaHoaDon() {
        StringBuilder sb = new StringBuilder(PREFIX);

        for (int i = 0; i < LENGTH; i++) {
            int index = RANDOM.nextInt(CHARS.length());
            sb.append(CHARS.charAt(index));
        }

        return sb.toString();
    }
}