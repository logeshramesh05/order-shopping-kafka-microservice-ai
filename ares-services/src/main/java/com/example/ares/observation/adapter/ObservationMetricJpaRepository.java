package com.example.ares.observation.adapter;

import com.example.ares.observation.domain.ObservationMetricRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data repository for persisted observation snapshots.
 */
interface ObservationMetricJpaRepository extends JpaRepository<ObservationMetricRecord, Long> {

    List<ObservationMetricRecord> findAllByOrderByObservedAtDesc(Pageable pageable);
}
