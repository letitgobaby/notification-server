package notification.application.user.port.outbound;

import notification.definition.vo.UserConfig;
import reactor.core.publisher.Mono;

public interface UserConfigProviderPort {

    /**
     * Retrieves the user configuration by user ID.
     *
     * @param userId the ID of the user
     * @return a Mono containing the UserConfig, or empty if not found
     */
    Mono<UserConfig> getUserConfigById(String userId);

}
