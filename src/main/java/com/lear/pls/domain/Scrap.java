package com.lear.pls.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Scrap {

	@Id
	private String id;
	private String chefEquipe;

	@ManyToOne
	@NotNull(message = "ce champ ne peut pas être null")
	private LieuDetection lieuDetection;

	@ManyToOne
	private Chaine chaine;

	@ManyToOne
	@NotNull(message = "ce champ ne peut pas être null")
	private Projet projet;

	@ManyToOne
	@NotNull(message = "ce champ ne peut pas être null")
	private Site site;

	@NotBlank(message = "ce champ ne peut pas être null")
	private String typeDefaut;

	@ManyToOne
	private Defaut defaut;
	
	@OneToMany(mappedBy="scrap", cascade = CascadeType.ALL)
	@NotEmpty(message = "ce champ ne peut pas être vide")
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<SubScrap> subScraps = new ArrayList<SubScrap>();

	@ManyToOne
	private User createdBy;

	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	@Column(updatable = false)
	private LocalDateTime createdAt;

	private boolean active = true;
	
	

	@ManyToOne
	private User traiterBy;

	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime traiterAt;
	@Lob
	private String commentaire;

	@Lob
	private String qualityCommentaire;
	
	private String reponse = "en attente";

	private boolean notifier = false;

	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime validerAt;

	@ManyToOne
	private User validerBy;

	private String valider;

	private String responsableEmail;

	private String scrapFile;

	private boolean cloturer = false;
	@ManyToOne
	private User cloturerBy;
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime cloturerDate;

	@ManyToOne
	private User userSuperviseur;
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime dateSuperviseur;

	public Scrap() {
		super();
	}
	
	@PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
	

	public List<SubScrap> getSubScraps() {
		return subScraps;
	}



	public String getQualityCommentaire() {
		return qualityCommentaire;
	}

	public void setQualityCommentaire(String qualityCommentaire) {
		this.qualityCommentaire = qualityCommentaire;
	}

	public void setSubScraps(List<SubScrap> subScraps) {
		this.subScraps = subScraps;
	}

	public User getUserSuperviseur() {
		return userSuperviseur;
	}

	public void setUserSuperviseur(User userSuperviseur) {
		this.userSuperviseur = userSuperviseur;
	}

	public LocalDateTime getDateSuperviseur() {
		return dateSuperviseur;
	}

	public void setDateSuperviseur(LocalDateTime dateSuperviseur) {
		this.dateSuperviseur = dateSuperviseur;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getChefEquipe() {
		return chefEquipe;
	}

	public void setChefEquipe(String chefEquipe) {
		this.chefEquipe = chefEquipe;
	}

	public LieuDetection getLieuDetection() {
		return lieuDetection;
	}

	public void setLieuDetection(LieuDetection lieuDetection) {
		this.lieuDetection = lieuDetection;
	}

	public Chaine getChaine() {
		return chaine;
	}

	public void setChaine(Chaine chaine) {
		this.chaine = chaine;
	}

	public Projet getProjet() {
		return projet;
	}

	public void setProjet(Projet projet) {
		this.projet = projet;
	}

	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}

	public String getTypeDefaut() {
		return typeDefaut;
	}

	public void setTypeDefaut(String typeDefaut) {
		this.typeDefaut = typeDefaut;
	}

	public Defaut getDefaut() {
		return defaut;
	}

	public void setDefaut(Defaut defaut) {
		this.defaut = defaut;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public User getTraiterBy() {
		return traiterBy;
	}

	public void setTraiterBy(User traiterBy) {
		this.traiterBy = traiterBy;
	}

	public LocalDateTime getTraiterAt() {
		return traiterAt;
	}

	public void setTraiterAt(LocalDateTime traiterAt) {
		this.traiterAt = traiterAt;
	}

	public String getCommentaire() {
		return commentaire;
	}

	public void setCommentaire(String commentaire) {
		this.commentaire = commentaire;
	}

	public String getReponse() {
		return reponse;
	}

	public void setReponse(String reponse) {
		this.reponse = reponse;
	}

	public boolean isNotifier() {
		return notifier;
	}

	public void setNotifier(boolean notifier) {
		this.notifier = notifier;
	}

	public LocalDateTime getValiderAt() {
		return validerAt;
	}

	public void setValiderAt(LocalDateTime validerAt) {
		this.validerAt = validerAt;
	}

	public User getValiderBy() {
		return validerBy;
	}

	public void setValiderBy(User validerBy) {
		this.validerBy = validerBy;
	}

	public String getValider() {
		return valider;
	}

	public void setValider(String valider) {
		this.valider = valider;
	}

	public String getResponsableEmail() {
		return responsableEmail;
	}

	public void setResponsableEmail(String responsableEmail) {
		this.responsableEmail = responsableEmail;
	}

	public String getScrapFile() {
		return scrapFile;
	}

	public void setScrapFile(String scrapFile) {
		this.scrapFile = scrapFile;
	}

	public boolean isCloturer() {
		return cloturer;
	}

	public void setCloturer(boolean cloturer) {
		this.cloturer = cloturer;
	}

	public User getCloturerBy() {
		return cloturerBy;
	}

	public void setCloturerBy(User cloturerBy) {
		this.cloturerBy = cloturerBy;
	}

	public LocalDateTime getCloturerDate() {
		return cloturerDate;
	}

	public void setCloturerDate(LocalDateTime cloturerDate) {
		this.cloturerDate = cloturerDate;
	}

}
