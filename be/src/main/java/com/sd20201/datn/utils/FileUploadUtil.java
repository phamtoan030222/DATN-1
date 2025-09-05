package com.sd20201.datn.utils;

import com.sd20201.datn.infrastructure.exception.CloudinaryException;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

@UtilityClass
public class FileUploadUtil {

    public static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    public static final String IMAGE_PATTERN = "([^\\s]+(\\.(?i)(jpg|png|gif|bmp))$)";

    public static final String DATE_FORMAT = "yyyyMMddHHmmss";

    public static final String FILE_NAME_FORMAT = "%s_%s";

    public static boolean isAllowedExtension(final String fileName,final String pattern) {
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(fileName).matches();
    }

    public static void assertAllowed(MultipartFile file,String pattern) {
        final long size = file.getSize();
        if(size > MAX_FILE_SIZE) {
            throw new CloudinaryException("Max size file is 2MB");
        }

        final String fileName = file.getOriginalFilename();
        final String extension = FilenameUtils.getExtension(fileName);

        if(!isAllowedExtension(fileName,extension)) {
            throw new CloudinaryException("Only jpg, png, gif, bmp files are allowed");
        }
    }

    public static String getFilename(final String name) {
        final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        final String date = dateFormat.format(DateTimeUtil.getCurrentTimeMillisecondsStamp());

        return String.format(FILE_NAME_FORMAT, date, name);
    }
}
