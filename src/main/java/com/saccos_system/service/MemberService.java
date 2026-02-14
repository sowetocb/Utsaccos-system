package com.saccos_system.service;

import com.saccos_system.dto.MemberRequestDTO;
import com.saccos_system.dto.MemberResponseDTO;
import com.saccos_system.dto.StatusDTO;
import com.saccos_system.model.LookupStatus;
import com.saccos_system.model.StaffProfile;
import com.saccos_system.repository.LookupStatusRepository;
import com.saccos_system.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final StaffProfileRepository staffProfileRepository;
    private final LookupStatusRepository statusRepository;

    public MemberResponseDTO createMember(MemberRequestDTO request) {
        validateMemberRequest(request);

        LookupStatus status = statusRepository.findById(request.getStatusId())
                .orElseThrow(() -> new RuntimeException("Status not found with ID: " + request.getStatusId()));

        StaffProfile member = mapToEntity(request, status);
        StaffProfile savedMember = staffProfileRepository.save(member);

        return convertToResponseDTO(savedMember);
    }

    public List<MemberResponseDTO> getAllMembers() {
        return staffProfileRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public MemberResponseDTO getMemberById(Long id) {
        StaffProfile member = staffProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found with ID: " + id));
        return convertToResponseDTO(member);
    }

    public MemberResponseDTO getMemberByMemberNumber(String memberNumber) {
        StaffProfile member = staffProfileRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new RuntimeException("Member not found with number: " + memberNumber));
        return convertToResponseDTO(member);
    }

    public MemberResponseDTO updateMember(Long id, MemberRequestDTO request) {
        StaffProfile member = staffProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found with ID: " + id));

        updateEntity(member, request);
        StaffProfile updatedMember = staffProfileRepository.save(member);

        return convertToResponseDTO(updatedMember);
    }

    public void deleteMember(Long id) {
        if (!staffProfileRepository.existsById(id)) {
            throw new RuntimeException("Member not found with ID: " + id);
        }
        staffProfileRepository.deleteById(id);
    }

    // ========== HELPER METHODS ==========

    private void validateMemberRequest(MemberRequestDTO request) {
        if (staffProfileRepository.existsByMemberNumber(request.getMemberNumber())) {
            throw new RuntimeException("Member number already exists: " + request.getMemberNumber());
        }

        if (request.getEmail() != null && staffProfileRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }
    }

    private StaffProfile mapToEntity(MemberRequestDTO request, LookupStatus status) {
        StaffProfile member = new StaffProfile();
        member.setMemberNumber(request.getMemberNumber());
        member.setFirstName(request.getFirstName());
        member.setLastName(request.getLastName());
        member.setEmail(request.getEmail());
        member.setPhone(request.getPhone());
        member.setIdNumber(request.getIdNumber());
        member.setDateOfBirth(request.getDateOfBirth());
        member.setStatus(status);
        member.setCreatedBy(request.getCreatedBy());
        return member;
    }

    private void updateEntity(StaffProfile member, MemberRequestDTO request) {
        member.setFirstName(request.getFirstName());
        member.setLastName(request.getLastName());
        member.setEmail(request.getEmail());
        member.setPhone(request.getPhone());
        member.setIdNumber(request.getIdNumber());
        member.setDateOfBirth(request.getDateOfBirth());

        if (request.getStatusId() != null) {
            LookupStatus status = statusRepository.findById(request.getStatusId())
                    .orElseThrow(() -> new RuntimeException("Status not found with ID: " + request.getStatusId()));
            member.setStatus(status);
        }

        member.setModifiedBy(request.getCreatedBy());
    }

    private MemberResponseDTO convertToResponseDTO(StaffProfile member) {
        MemberResponseDTO dto = new MemberResponseDTO();
        dto.setProfileId(member.getProfileId());
        dto.setMemberNumber(member.getMemberNumber());
        dto.setFirstName(member.getFirstName());
        dto.setLastName(member.getLastName());
        dto.setEmail(member.getEmail());
        dto.setPhone(member.getPhone());
        dto.setIdNumber(member.getIdNumber());
        dto.setDateOfBirth(member.getDateOfBirth());
        dto.setJoinDate(member.getJoinDate());
        dto.setCreatedDate(member.getCreatedDate());
        dto.setCreatedBy(member.getCreatedBy());
        dto.setModifiedDate(member.getModifiedDate());
        dto.setModifiedBy(member.getModifiedBy());

        // Convert status
        if (member.getStatus() != null) {
            StatusDTO statusDTO = convertStatusToDTO(member.getStatus());
            dto.setStatus(statusDTO);
        }

        return dto;
    }

    private StatusDTO convertStatusToDTO(LookupStatus status) {
        StatusDTO statusDTO = new StatusDTO();
        statusDTO.setStatusId(status.getStatusId());
        statusDTO.setStatusCode(status.getStatusCode());
        statusDTO.setStatusName(status.getStatusName());
        statusDTO.setDescription(status.getDescription());
        statusDTO.setIsActive(status.getIsActive());
        return statusDTO;
    }
}