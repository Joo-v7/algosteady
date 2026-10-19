package com.algosteady.judging.repository;

import com.algosteady.judging.domain.ExecutionJob;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 대기 작업의 등록순 조회 */
public interface ExecutionJobRepository extends JpaRepository<ExecutionJob, UUID> {

    /**
     * 지정한 상태의 가장 오래된 작업을 잠금 조회한다.
     * @param status 조회할 작업 상태
     * @return 지정한 상태 중 가장 먼저 등록된 작업
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExecutionJob> findFirstByStatusOrderByQueuedAtAscIdAsc(ExecutionJob.Status status);

    /**
     * 지정한 상태의 작업을 조회한다.
     * @param status 조회할 작업 상태
     * @return 지정한 상태의 작업 목록
     */
    List<ExecutionJob> findAllByStatus(ExecutionJob.Status status);
}
