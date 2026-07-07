package com.lear.pls.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class Demande {
	
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
	
	private String typeDemande = "Normal";
	
	@ManyToOne
	private Defaut defaut;
	
	
	private boolean envoyerCAD = false;
	
//	private Boolean sparePart = false;
	
//	private Boolean tri = false;


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
//	@Lob
	private String commentaire;
//	@Lob
	private String qualityCommentaire;
	
	private String reponse= "en attente";
	
	private boolean notifier = false;
	
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime validerAt;
	
	@ManyToOne
	private User validerBy;
	
	private String valider;
	
	private String responsableEmail;
	
	private String waitCAD = "";
	
	private String waitVariance = "";
	
	private String waitRecut = "";
	
	@ManyToOne
	private User userCAD;
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime dateCAD;
	@ManyToOne
	private User userVariance;
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime dateVariance;
	@ManyToOne
	private User userRecut;
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime dateRecut;
	
	private String waitMatelassage = "";
	
	private String waitProd = "";
	private String tableProd;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")
	private LocalDateTime tableProdTime;
	@ManyToOne
	private User tableProdUser;
	
	@ManyToOne
	private User userTransport;
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime dateTransport;
	
	private String plsFile;
	private String qualityFile;
	
	
	private Boolean cadBlock = false;
//	@Lob
	private String cadBlockReason;
	
	private boolean cloturer = false;
	@ManyToOne
	private User cloturerBy;
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime cloturerDate;

	private String errorMessage;

	private Boolean urgent = false;
	private String urgentBy;

	@PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }



	public Demande() {
		super();
	}

	public String getUrgentBy() {
		return urgentBy;
	}

	public void setUrgentBy(String urgentBy) {
		this.urgentBy = urgentBy;
	}

	public Boolean getUrgent() {
		return urgent;
	}

	public void setUrgent(Boolean urgent) {
		this.urgent = urgent;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public User getUserTransport() {
		return userTransport;
	}

	

	public User getTableProdUser() {
		return tableProdUser;
	}

	public void setTableProdUser(User tableProdUser) {
		this.tableProdUser = tableProdUser;
	}

	public String getQualityCommentaire() {
		return qualityCommentaire;
	}

	public void setQualityCommentaire(String qualityCommentaire) {
		this.qualityCommentaire = qualityCommentaire;
	}

	public void setUserTransport(User userTransport) {
		this.userTransport = userTransport;
	}

	public LocalDateTime getDateTransport() {
		return dateTransport;
	}

	public void setDateTransport(LocalDateTime dateTransport) {
		this.dateTransport = dateTransport;
	}

	public String getQualityFile() {
		return qualityFile;
	}

	public void setQualityFile(String qualityFile) {
		this.qualityFile = qualityFile;
	}

	public String getTypeDemande() {
		return typeDemande;
	}

	public void setTypeDemande(String typeDemande) {
		this.typeDemande = typeDemande;
	}

	public Boolean getCadBlock() {
		return cadBlock;
	}

	public void setCadBlock(Boolean cadBlock) {
		this.cadBlock = cadBlock;
	}

	public String getCadBlockReason() {
		return cadBlockReason;
	}

	public void setCadBlockReason(String cadBlockReason) {
		this.cadBlockReason = cadBlockReason;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
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
	

	public String getWaitProd() {
		return waitProd;
	}

	public void setWaitProd(String waitProd) {
		this.waitProd = waitProd;
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

	public boolean isEnvoyerCAD() {
		return envoyerCAD;
	}

	public void setEnvoyerCAD(boolean envoyerCAD) {
		this.envoyerCAD = envoyerCAD;
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

	public String getWaitCAD() {
		return waitCAD;
	}

	public void setWaitCAD(String waitCAD) {
		this.waitCAD = waitCAD;
	}

	public String getWaitVariance() {
		return waitVariance;
	}

	public void setWaitVariance(String waitVariance) {
		this.waitVariance = waitVariance;
	}

	public String getWaitRecut() {
		return waitRecut;
	}

	public void setWaitRecut(String waitRecut) {
		this.waitRecut = waitRecut;
	}

	public User getUserCAD() {
		return userCAD;
	}

	public void setUserCAD(User userCAD) {
		this.userCAD = userCAD;
	}

	public LocalDateTime getDateCAD() {
		return dateCAD;
	}

	public void setDateCAD(LocalDateTime dateCAD) {
		this.dateCAD = dateCAD;
	}

	public User getUserVariance() {
		return userVariance;
	}

	public void setUserVariance(User userVariance) {
		this.userVariance = userVariance;
	}

	public LocalDateTime getDateVariance() {
		return dateVariance;
	}

	public void setDateVariance(LocalDateTime dateVariance) {
		this.dateVariance = dateVariance;
	}

	public User getUserRecut() {
		return userRecut;
	}

	public void setUserRecut(User userRecut) {
		this.userRecut = userRecut;
	}

	public LocalDateTime getDateRecut() {
		return dateRecut;
	}

	public void setDateRecut(LocalDateTime dateRecut) {
		this.dateRecut = dateRecut;
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

	public String getTableProd() {
		return tableProd;
	}

	public void setTableProd(String tableProd) {
		this.tableProd = tableProd;
	}

	public LocalDateTime getTableProdTime() {
		return tableProdTime;
	}

	public void setTableProdTime(LocalDateTime tableProdTime) {
		this.tableProdTime = tableProdTime;
	}

	public String getPlsFile() {
		return plsFile;
	}

	public void setPlsFile(String plsFile) {
		this.plsFile = plsFile;
	}

	public LocalDateTime getCloturerDate() {
		return cloturerDate;
	}

	public void setCloturerDate(LocalDateTime cloturerDate) {
		this.cloturerDate = cloturerDate;
	}

	public String getWaitMatelassage() {
		return waitMatelassage;
	}

	public void setWaitMatelassage(String waitMatelassage) {
		this.waitMatelassage = waitMatelassage;
	}

	

	
	
	
	

}
