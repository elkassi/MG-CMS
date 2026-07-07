package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.dto.RouleauSummaryDto;
import com.lear.MGCMS.services.RouleauSummaryService;
import com.lear.MGCMS.domain.ScanRouleau;
import com.lear.MGCMS.repositories.ScanRouleauRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rouleau-summary")
public class RouleauSummaryController {

    @Autowired
    private RouleauSummaryService service;

    @Autowired
    private ScanRouleauRepository scanRouleauRepository;

    @GetMapping("/test-data")
    public List<ScanRouleau> testData() {
        Page<ScanRouleau> page = scanRouleauRepository.findAll(PageRequest.of(0, 10));
        return page.getContent();
    }

    @GetMapping("/search")
    public Page<RouleauSummaryDto> search(
            @RequestParam(value = "rollId", required = false) String rollId,
            @RequestParam(value = "itemNumber", required = false) String itemNumber,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size
    ) {
        return service.getRouleauSummary(page, size, rollId, itemNumber);
    }
}
