package com.natwest.bank.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, IdempotencyRecord.IdempotencyId> {

    Optional<IdempotencyRecord> findByIdempotencyKeyAndOperation(String key, String operation);

    @Modifying
    @Query("DELETE FROM IdempotencyRecord r WHERE r.idempotencyKey = :key AND r.operation = :operation AND r.status = 'FAILED'")
    int deleteFailed(@Param("key") String key, @Param("operation") String operation);
}