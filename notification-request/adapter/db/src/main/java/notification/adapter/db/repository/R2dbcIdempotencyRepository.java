package notification.adapter.db.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import notification.adapter.db.IdempotencyEntity;

public interface R2dbcIdempotencyRepository extends R2dbcRepository<IdempotencyEntity, String> {

}
