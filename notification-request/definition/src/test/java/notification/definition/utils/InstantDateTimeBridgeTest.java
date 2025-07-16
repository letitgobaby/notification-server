package notification.definition.utils;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

public class InstantDateTimeBridgeTest {

    @Test
    public void testToInstantWithValidLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2024, 6, 1, 12, 30, 45);
        Instant instant = InstantDateTimeBridge.toInstant(ldt);

        assertThat(instant).isNotNull();
        assertThat(instant).isEqualTo(ldt.atZone(ZoneId.of("UTC")).toInstant());
    }

    @Test
    public void testToInstantWithNull() {
        assertThat(InstantDateTimeBridge.toInstant(null)).isNull();
    }

    @Test
    public void testToLocalDateTimeWithValidInstant() {
        LocalDateTime ldt = LocalDateTime.of(2024, 6, 1, 12, 30, 45);
        Instant instant = ldt.atZone(ZoneId.of("UTC")).toInstant();
        LocalDateTime result = InstantDateTimeBridge.toLocalDateTime(instant);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(ldt);
    }

    @Test
    public void testToLocalDateTimeWithNull() {
        assertThat(InstantDateTimeBridge.toLocalDateTime(null)).isNull();
    }

    @Test
    public void testToLocalDateTimeWithZoneId() {
        LocalDateTime ldt = LocalDateTime.of(2024, 6, 1, 12, 30, 45);
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Instant instant = ldt.atZone(zoneId).toInstant();
        LocalDateTime result = InstantDateTimeBridge.toLocalDateTime(instant, zoneId);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(ldt);
    }

    @Test
    public void testToLocalDateTimeWithZoneIdNullInstant() {
        assertThat(InstantDateTimeBridge.toLocalDateTime(null, ZoneId.of("UTC"))).isNull();
    }
}