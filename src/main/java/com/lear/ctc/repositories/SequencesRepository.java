package com.lear.ctc.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.ctc.domain.Sequences;

public interface SequencesRepository extends CrudRepository<Sequences, Long> {
	

		
	void deleteById(Long id);

	Iterable<Sequences> findAll();
		
	List<Sequences> findBySequence(String sequence);

	@Query("select max(id) from Sequences")
	Long getMaxId();
}
