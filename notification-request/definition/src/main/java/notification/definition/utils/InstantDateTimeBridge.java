package notification.definition.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class InstantDateTimeBridge {

    private final static ZoneId UTC_ZONE = ZoneId.of("UTC");

    /**
     * LocalDateTime을 Instant로 변환합니다.
     * 
     * @param localDateTime
     * @return
     */
    public static Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }

        return localDateTime.atZone(UTC_ZONE).toInstant();
    }

    /**
     * Instant를 LocalDateTime으로 변환합니다.
     * 
     * @param instant
     * @return
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }

        return LocalDateTime.ofInstant(instant, UTC_ZONE);
    }

    /**
     * Instant를 LocalDateTime으로 변환합니다. 특정 ZoneId를 사용합니다.
     * 
     * @param instant
     * @param zoneId
     * @return
     */
    public static LocalDateTime toLocalDateTime(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }

        return LocalDateTime.ofInstant(instant, zoneId);
    }

}
