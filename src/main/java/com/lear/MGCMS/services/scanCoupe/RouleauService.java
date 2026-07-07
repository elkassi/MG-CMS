package com.lear.MGCMS.services.scanCoupe;

import com.lear.MGCMS.domain.scanCoupe.Rouleau;
import com.lear.MGCMS.domain.scanCoupe.RouleauHistorique;
import com.lear.MGCMS.repositories.scanCoupe.RouleauHistoriqueRepository;
import com.lear.MGCMS.repositories.scanCoupe.RouleauRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RouleauService {

    @Autowired
    private RouleauRepository repository;

    @Autowired
    private RouleauHistoriqueRepository historiqueRepository;

    public Rouleau save(Rouleau rouleau) {
        repository.save(rouleau);
        RouleauHistorique historique = new RouleauHistorique();
        historique.setDate(LocalDateTime.now());
        historique.setSerialId(rouleau.getSerialId());
        historique.setContent(rouleau.toString());
        historiqueRepository.save(historique);
        return rouleau;
    }
}
