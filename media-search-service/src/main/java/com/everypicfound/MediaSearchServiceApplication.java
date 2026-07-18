package com.everypicfound;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.everypicfound.imageasset.infrastructure.mapper.ImageAssetMapper;
import com.everypicfound.vectorindex.infrastructure.qdrant.QdrantVectorMapper;

@SpringBootApplication
@MapperScan(basePackageClasses = {
		ImageAssetMapper.class,
		QdrantVectorMapper.class
})
@ConfigurationPropertiesScan
@EnableScheduling
public class MediaSearchServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediaSearchServiceApplication.class, args);
	}

}
