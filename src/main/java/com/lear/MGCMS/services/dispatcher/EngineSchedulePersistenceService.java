package com.lear.MGCMS.services.dispatcher;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.dispatcher.EngineScheduleEntry;
import com.lear.MGCMS.repositories.dispatcher.EngineScheduleEntryRepository;

@Service
public class EngineSchedulePersistenceService {

    @Autowired
    private EngineScheduleEntryRepository repository;

    @Transactional
    public void replaceCurrentSchedule(Collection<EngineScheduleEntry> entries) {
        repository.deleteAllInBatchFast();
        if (entries != null && !entries.isEmpty()) {
            repository.saveAll(entries);
        }
    }
}
