package com.example.splitwise.repository;

import com.example.splitwise.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    boolean existsByGroup_IdAndUser_Id(
            Long groupId, Long userId
    );
    List<GroupMember> findByGroup_Id(Long groupId);
}
