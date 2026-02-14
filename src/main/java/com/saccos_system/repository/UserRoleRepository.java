package com.saccos_system.repository;

import com.saccos_system.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUser_UserId(Long userId);

    Optional<UserRole> findByUser_UserIdAndRole_RoleName(Long userId, String roleName);

    @Query("SELECT ur FROM UserRole ur WHERE ur.user.userId = :userId")
    List<UserRole> findAllRolesByUserId(@Param("userId") Long userId);

    @Query("SELECT ur.user.userId FROM UserRole ur WHERE ur.role.roleName = :roleName")
    List<Long> findUserIdsByRoleName(@Param("roleName") String roleName);

    void deleteByUser_UserIdAndRole_RoleName(Long userId, String roleName);
}