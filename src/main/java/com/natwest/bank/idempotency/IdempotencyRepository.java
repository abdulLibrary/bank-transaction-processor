package com.natwest.bank.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, IdempotencyRecord.IdempotencyId> {

    Optional<IdempotencyRecord> findByIdempotencyKeyAndOperation(String key, String operation);
}
