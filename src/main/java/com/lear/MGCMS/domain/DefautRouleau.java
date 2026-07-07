package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class DefautRouleau {

    @Id
    private String titre;

    private Boolean active = true;

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

}
