package com.backend.observerr.notification;

import com.backend.observerr.notification.model.DeviceToken;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.security.Security;

@Slf4j
@Component
@ConditionalOnExpression(
        "'${webpush.vapid.public-key:}'.trim().length() > 0 && '${webpush.vapid.private-key:}'.trim().length() > 0"
)
public class JavaWebPushClient implements WebPushClient {

    private final String publicKey;
    private final String privateKey;
    private final String subject;
    private PushService pushService;

    public JavaWebPushClient(
            @Value("${webpush.vapid.public-key}") String publicKey,
            @Value("${webpush.vapid.private-key}") String privateKey,
            @Value("${webpush.vapid.subject:mailto:support@observerr.app}") String subject) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
    }

    @PostConstruct
    void init() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        pushService = new PushService(publicKey, privateKey, subject);
        log.info("Web Push (VAPID) client initialized");
    }

    @Override
    public boolean isEnabled() {
        return pushService != null;
    }

    @Override
    public int send(DeviceToken subscription, String payloadJson) throws Exception {
        Subscription target = new Subscription(
                subscription.getEndpoint(),
                new Subscription.Keys(subscription.getP256dh(), subscription.getAuth())
        );
        var response = pushService.send(new Notification(target, payloadJson));
        return response.getStatusLine().getStatusCode();
    }
}
