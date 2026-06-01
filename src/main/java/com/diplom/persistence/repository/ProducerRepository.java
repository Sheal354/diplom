package com.diplom.persistence.repository;

import com.diplom.persistence.entity.ProducerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProducerRepository extends JpaRepository<ProducerEntity, Integer> {
    Optional<ProducerEntity> findByEmail(String email);
    boolean existsByEmail(String email);

    // Только активные производители, сортировка по убыванию updatedAt
    List<ProducerEntity> findAllByIsActiveTrueOrderByUpdatedAtDesc();
}