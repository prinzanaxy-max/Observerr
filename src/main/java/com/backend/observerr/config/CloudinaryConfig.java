package com.backend.observerr.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Bean
    @ConditionalOnExpression(
            "!'${cloudinary.cloud-name:}'.trim().isEmpty()"
                    + " && !'${cloudinary.api-key:}'.trim().isEmpty()"
                    + " && !'${cloudinary.api-secret:}'.trim().isEmpty()"
    )
    Cloudinary cloudinary(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        Map<String, String> config = ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        );
        return new Cloudinary(config);
    }
}
