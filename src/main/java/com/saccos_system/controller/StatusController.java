package com.saccos_system.controller;

import com.saccos_system.service.StatusService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statuses")
public class StatusController {

    @SuppressWarnings("unused")
    private final StatusService statusService;

    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }


}