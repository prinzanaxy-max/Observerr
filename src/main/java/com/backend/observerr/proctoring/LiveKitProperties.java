package com.backend.observerr.proctoring;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "livekit")
public class LiveKitProperties {

    private String url;
    private String apiKey;
    private String apiSecret;
    private long tokenTtlSeconds = 300;
}
