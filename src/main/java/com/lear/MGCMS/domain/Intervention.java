package com.lear.MGCMS.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.utils.InterventionIdGenerator;

@Entity
public class Intervention {

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "intervention_id")
    @GenericGenerator(
        name = "intervention_id", 
        strategy = "com.lear.MGCMS.utils.InterventionIdGenerator", 
        parameters = {
        		@Parameter(name = InterventionIdGenerator.INCREMENT_PARAM, value = "1"),
        		})
	private String id;
	
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")
	private LocalDateTime createdAt;
	
	private String serie;
	private String sequence;
	private LocalDate date;
	private String shift;
	private String partNumberMaterial;
	private String partNumberMaterialDescription;
	
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime debutArret;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime debutIntervention;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime finIntervention;
	

	private String codeErreur;
	
	@ManyToOne
	private CodeArret codeArret;
	private String cause;
	private String action;
	@ManyToOne
	private CodeDefaut codeCoupe;

	
	private String departement;
	private String problemeResolu;
	private String matriculeEmetteur;
	private String matriculeResponsable;
	private String machine;
	private String type;
	private String sousType;
	@ManyToOne
	private CodeDefaut solution;
	
//	private String descriptionDeffaillance;
//	private String descriptionArret;
//	private String codeSolution;
//	private String descriptionSolution;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")
	private LocalDateTime dateValidation;
	private String validerPar;
	
	@PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
	
	public LocalDateTime getDateValidation() {
		return dateValidation;
	}

	public void setDateValidation(LocalDateTime dateValidation) {
		this.dateValidation = dateValidation;
	}

	public CodeDefaut getCodeCoupe() {
		return codeCoupe;
	}

	public void setCodeCoupe(CodeDefaut codeCoupe) {
		this.codeCoupe = codeCoupe;
	}

	public String getSousType() {
		return sousType;
	}

	public void setSousType(String sousType) {
		this.sousType = sousType;
	}


	public String getValiderPar() {
		return validerPar;
	}

	public void setValiderPar(String validerPar) {
		this.validerPar = validerPar;
	}

	public CodeDefaut getSolution() {
		return solution;
	}

	public void setSolution(CodeDefaut solution) {
		this.solution = solution;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCodeErreur() {
		return codeErreur;
	}

	public void setCodeErreur(String codeErreur) {
		this.codeErreur = codeErreur;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public String getSerie() {
		return serie;
	}
	public void setSerie(String serie) {
		this.serie = serie;
	}
	public String getSequence() {
		return sequence;
	}
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
	public LocalDate getDate() {
		return date;
	}
	public void setDate(LocalDate date) {
		this.date = date;
	}
	public String getShift() {
		return shift;
	}
	public void setShift(String shift) {
		this.shift = shift;
	}
	public String getPartNumberMaterial() {
		return partNumberMaterial;
	}
	public void setPartNumberMaterial(String partNumberMaterial) {
		this.partNumberMaterial = partNumberMaterial;
	}
	public String getPartNumberMaterialDescription() {
		return partNumberMaterialDescription;
	}
	public void setPartNumberMaterialDescription(String partNumberMaterialDescription) {
		this.partNumberMaterialDescription = partNumberMaterialDescription;
	}
	public LocalDateTime getDebutArret() {
		return debutArret;
	}
	public void setDebutArret(LocalDateTime debutArret) {
		this.debutArret = debutArret;
	}
	public LocalDateTime getDebutIntervention() {
		return debutIntervention;
	}
	public void setDebutIntervention(LocalDateTime debutIntervention) {
		this.debutIntervention = debutIntervention;
	}
	public LocalDateTime getFinIntervention() {
		return finIntervention;
	}
	public void setFinIntervention(LocalDateTime finIntervention) {
		this.finIntervention = finIntervention;
	}
	public CodeArret getCodeArret() {
		return codeArret;
	}
	public void setCodeArret(CodeArret codeArret) {
		this.codeArret = codeArret;
	}
	public String getCause() {
		return cause;
	}
	public void setCause(String cause) {
		this.cause = cause;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getDepartement() {
		return departement;
	}
	public void setDepartement(String departement) {
		this.departement = departement;
	}
	public String getProblemeResolu() {
		return problemeResolu;
	}
	public void setProblemeResolu(String problemeResolu) {
		this.problemeResolu = problemeResolu;
	}
	public String getMatriculeEmetteur() {
		return matriculeEmetteur;
	}
	public void setMatriculeEmetteur(String matriculeEmetteur) {
		this.matriculeEmetteur = matriculeEmetteur;
	}
	public String getMatriculeResponsable() {
		return matriculeResponsable;
	}
	public void setMatriculeResponsable(String matriculeResponsable) {
		this.matriculeResponsable = matriculeResponsable;
	}
	public String getMachine() {
		return machine;
	}
	public void setMachine(String machine) {
		this.machine = machine;
	}
	
	
	
	
}
