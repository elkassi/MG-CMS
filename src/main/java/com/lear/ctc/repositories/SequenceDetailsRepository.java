package com.lear.ctc.repositories;

import com.lear.ctc.domain.SequenceDetails;
import com.lear.ctc.domain.Sequences;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SequenceDetailsRepository  extends CrudRepository<SequenceDetails, Long>  {
    List<SequenceDetails> findBySerialNumber(String serie);
}
