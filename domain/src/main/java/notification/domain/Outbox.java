package notification.domain;

import java.time.Instant;

public interface Outbox {

    /**
     * Outbox 메시지를 FAILED 상태로 변경합니다.
     * 
     * @param nextRetryAt
     */
    void markAsFailed(Instant nextRetryAt);

    /**
     * 현재 Outbox 메시지의 재시도 횟수를 maxRetries와 비교하여 최대 재시도 횟수에 도달했는지 확인합니다.
     * 
     * @param maxRetries
     * @return
     */
    boolean isMaxRetryAttemptsReached(int maxRetries);

}
