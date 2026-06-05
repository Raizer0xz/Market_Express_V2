package com.example.MS_sucursales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MsSucursalesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsSucursalesApplication.class, args);
	}

}
