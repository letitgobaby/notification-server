package notification.adapter.client;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import notification.application.user.port.outbound.UserConfigProviderPort;
import notification.definition.vo.UserConfig;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class MockingUserConfigProviderAdapter implements UserConfigProviderPort {

    private final ConcurrentHashMap<String, UserConfig> users;

    public MockingUserConfigProviderAdapter() {
        users = new ConcurrentHashMap<>() {
            {
                put("user-001", new UserConfig(
                        "user-001",
                        "Alice Kim",
                        "push-token-001",
                        "alice@example.com",
                        "010-1234-5678",
                        "ko",
                        "Asia/Seoul"));

                put("user-002", new UserConfig(
                        "user-002",
                        "Bob Lee",
                        "push-token-002",
                        "bob@example.com",
                        "010-2345-6789",
                        "ko",
                        "Asia/Seoul"));

                put("user-100", new UserConfig(
                        "user-100",
                        "Charlie",
                        "push-token-100",
                        "charlie@example.com",
                        "010-1111-2222",
                        "en",
                        "America/New_York"));

                put("user-101", new UserConfig(
                        "user-101",
                        "Dana",
                        "push-token-101",
                        "dana@example.com",
                        "010-2222-3333",
                        "ko",
                        "Asia/Seoul"));

                put("user-102", new UserConfig(
                        "user-102",
                        "Eve",
                        "push-token-102",
                        "eve@example.com",
                        "010-3333-4444",
                        "en",
                        "America/New_York"));

                put("user-555", new UserConfig(
                        "user-555",
                        "Frank",
                        "push-token-555",
                        "frank@example.com",
                        "010-5555-6666",
                        "ko",
                        "Asia/Seoul"));
            }
        };
    }

    @Override
    public Mono<UserConfig> getUserConfigById(String userId) {
        UserConfig userConfig = users.get(userId);

        if (userConfig == null) {
            log.warn("User config not found for userId: {}", userId);
            return Mono.empty();
        }

        return Mono.just(userConfig);
    }

}
