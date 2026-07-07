package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.SerieRouleauTemp;
import com.lear.MGCMS.repositories.SerieRouleauTempRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SerieRouleauTempService {

    @Autowired
    private SerieRouleauTempRepository repo;


    public List<SerieRouleauTemp> getEnCours(String reftissu) {
        LocalDateTime date = LocalDateTime.now().minusMinutes(15);
        return repo.getEnCours(reftissu, date);
    }


    public SerieRouleauTemp save(SerieRouleauTemp serieRouleauTemp) {
        return repo.save(serieRouleauTemp);
    }

    public void deleteByid(String tableMatelassage) {
        try{
            repo.deleteById(tableMatelassage);
        }catch (Exception e){
            System.out.println(e);
        }
    }

    public List<SerieRouleauTemp> getAll() {
        return (List<SerieRouleauTemp>) repo.findAll();
    }
}
