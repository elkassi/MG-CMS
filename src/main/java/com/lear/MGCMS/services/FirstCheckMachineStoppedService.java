package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.FirstCheck;
import com.lear.MGCMS.domain.FirstCheckMachineStopped;
import com.lear.MGCMS.repositories.FirstCheckMachineStoppedRepository;
import com.lear.MGCMS.repositories.FirstCheckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FirstCheckMachineStoppedService {

    @Autowired
    private FirstCheckMachineStoppedRepository repo;


    public FirstCheckMachineStopped save(FirstCheckMachineStopped obj) {
        return repo.save(obj);
    }

    //findList
    public List<FirstCheckMachineStopped> findList(LocalDate date, String shift, String machine, String category) {
        return repo.findList(date, shift, machine, category);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
