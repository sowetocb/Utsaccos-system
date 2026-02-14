package com.saccos_system.service;



import com.saccos_system.model.LookupStatus;
import com.saccos_system.repository.LookupStatusRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class StatusService {

    private final LookupStatusRepository statusRepository;

    // Constructor Injection (Best Practice for @Autowired)
    public StatusService(LookupStatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    public List<LookupStatus> getAllStatuses() {
        return statusRepository.findAll();
    }
}