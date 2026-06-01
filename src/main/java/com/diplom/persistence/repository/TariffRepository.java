package com.diplom.persistence.repository;

import com.diplom.persistence.entity.TariffEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TariffRepository extends JpaRepository<TariffEntity, Integer> {
    List<TariffEntity> findByProducerId(Integer producerId);
}