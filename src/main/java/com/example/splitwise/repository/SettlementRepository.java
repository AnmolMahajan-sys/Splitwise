package com.example.splitwise.repository;

import com.example.splitwise.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByGroup_Id(Long groupId);
}
