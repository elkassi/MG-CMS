package com.lear.MGCMS.services.cms;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.cms.domain.OrderSchedule;
import com.lear.cms.repositories.OrderScheduleRepository;

@Service
public class OrderScheduleService {
	
	@Autowired
	private OrderScheduleRepository repo;

	public List<OrderSchedule> findBetweenInterval(LocalDate date1, LocalDate date2) {
		// TODO Auto-generated method stub
		return repo.findBetweenInterval(date1, date2);
	}

	public List<OrderSchedule> findByDateAndShift(LocalDate dateDemande, String shiftDemande) {
		return repo.findByDateAndShift(dateDemande, shiftDemande);
	}

    public List<OrderSchedule> findAllByStatu(List<String> statusDemande) {
		return repo.findAllByStatu(statusDemande);
    }

    public void save(OrderSchedule obj) {
		repo.save(obj);
    }

    public OrderSchedule findById(Long id) {
		return repo.findByIdDemande(id);
    }
}
