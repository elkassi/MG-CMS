package com.lear.MGCMS.controller.CuttingRequest.data;

import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestPartNumberData;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import com.lear.MGCMS.services.CuttingRequest.data.CuttingRequestPartNumberDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuttingRequestPartNumberData")
public class CuttingRequestPartNumberDataController {

    @Autowired
    private CuttingRequestPartNumberDataService service;

    @GetMapping("/bySequence/{sequence}")
    public List<CuttingRequestPartNumberData> findBySequence(@PathVariable String sequence) {
        return service.findBySequence(sequence);
    }
    @GetMapping("/bySequences/{sequences}")
    public List<CuttingRequestPartNumberData> findBySequence(@PathVariable List<String> sequences) {
        return service.findBySequences(sequences);
    }

    // we will pass the list of wo and we want to get list of strings of sequences
    @PostMapping("/getSequencesByWos")
    public List<String> getSequencesByWos(@RequestBody List<String> wos) {
        return service.getSequencesByWos(wos);
    }

    @PostMapping("/bySequences")
    public List<CuttingRequestPartNumberData> findBySequences(@RequestBody List<String> sequences) {
        return service.findBySequences(sequences);
    }

}
