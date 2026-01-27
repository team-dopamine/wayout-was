package kr.wayout.global.auth.jwt;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private final JwtProvider jwtProvider;
    private final ConcurrentHashMap<String, String> tokenStore = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::removeExpiredTokens, 1, 1, TimeUnit.HOURS);
    }

    @PreDestroy
    public void destory() {
        scheduler.shutdown();
    }

    public void save(String email, String refreshToken) {
        tokenStore.put(email, refreshToken);
    }

    public boolean validate(String email, String refreshToken) {
        String storedToken = tokenStore.get(email);
        if (storedToken == null) {
            return false;
        }
        if (!jwtProvider.validateToken(storedToken)) {
            tokenStore.remove(email);
            return false;
        }
        return storedToken.equals(refreshToken);
    }

    public void delete(String email) {
        tokenStore.remove(email);
    }

    private void removeExpiredTokens() {
        tokenStore.entrySet().removeIf(entry -> !jwtProvider.validateToken(entry.getValue()));
    }

}
