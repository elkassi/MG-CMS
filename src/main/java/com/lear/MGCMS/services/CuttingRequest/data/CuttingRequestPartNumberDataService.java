package com.lear.MGCMS.services.CuttingRequest.data;

import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestPartNumberData;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestPartNumberDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CuttingRequestPartNumberDataService {

    @Autowired
    private CuttingRequestPartNumberDataRepository repo;

    public List<CuttingRequestPartNumberData> findBySequence(String sequence) {
        return repo.findBySequence(sequence);
    }

    public List<CuttingRequestPartNumberData> findBySequences(List<String> sequences) {
        return repo.findBySequences(sequences);
    }

    public List<String> getSequencesByWos(List<String> wos) {
        return repo.getSequencesByWos(wos);
    }

    public List<String> findWoBySequence(String sequence) {
        return repo.findWoBySequence(sequence);
    }
}
