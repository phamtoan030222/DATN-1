package com.sd20201.datn;

import com.sd20201.datn.core.payment.config.VnPayConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(VnPayConfig.class)
public class DatnApplication {

	public static void main(String[] args) {
		SpringApplication.run(DatnApplication.class, args);
	}

}
