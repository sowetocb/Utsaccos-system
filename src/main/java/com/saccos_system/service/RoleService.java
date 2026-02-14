package com.saccos_system.service;


import com.saccos_system.model.Role;
import com.saccos_system.model.SystemUser;
import com.saccos_system.model.UserRole;
import com.saccos_system.repository.RoleRepository;
import com.saccos_system.repository.SystemUserRepository;
import com.saccos_system.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SystemUserRepository systemUserRepository;

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public void assignRoleToUser(Long userId, String roleName, String assignedBy) {
        SystemUser user = systemUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        // Check if already assigned
        if (userRoleRepository.findByUser_UserIdAndRole_RoleName(userId, roleName).isPresent()) {
            log.warn("User {} already has role {}", userId, roleName);
            return;
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedBy(assignedBy);

        userRoleRepository.save(userRole);

        log.info("Role {} assigned to user {} by {}", roleName, userId, assignedBy);
    }

    public void removeRoleFromUser(Long userId, String roleName, String removedBy) {
        UserRole userRole = userRoleRepository.findByUser_UserIdAndRole_RoleName(userId, roleName)
                .orElseThrow(() -> new RuntimeException("User does not have this role"));

        userRoleRepository.delete(userRole);

        log.info("Role {} removed from user {} by {}", roleName, userId, removedBy);
    }

    public List<String> getUserRoles(Long userId) {
        return userRoleRepository.findAllRolesByUserId(userId).stream()
                .map(ur -> ur.getRole().getRoleName())
                .collect(Collectors.toList());
    }

    public boolean userHasRole(Long userId, String roleName) {
        return userRoleRepository.findByUser_UserIdAndRole_RoleName(userId, roleName).isPresent();
    }

    public List<SystemUser> getUsersByRole(String roleName) {
        List<Long> userIds = userRoleRepository.findUserIdsByRoleName(roleName);
        return systemUserRepository.findAllById(userIds);
    }
}