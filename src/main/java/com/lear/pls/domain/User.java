package com.lear.pls.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name="users")
public class User {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Email(message = "username needs to be an email")
    @NotBlank(message = "Username is required")
    @Column(unique = true)
    private String username;

    @NotBlank(message = "Please enter your first name")
    private String firstName;
    
    @NotBlank(message = "Please enter your last name")
    private String lastName;
    
    @NotBlank(message = "il faut saisir la matricule")
    @Column(unique = true)
    private String matricule;
    
    private String fonction;
    
    private String email;
    
    private String departement;
    
    private String groupEmails;
    
    @ManyToOne
	private Site site;
    
    @NotBlank(message = "Password feild is required")
    private String password;

    @Transient
    private String confirmPassword;
    @NotBlank(message = "responsable feild is required")
    private String responsable;
    
    private boolean active = true;
    
    private boolean authPswChange = false;
	
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

    @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    @Column(updatable = false)
    private LocalDateTime created_At;

    @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    private LocalDateTime updated_At;


 	
	public User(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public User() {
	}

	
	

	public String getGroupEmails() {
		return groupEmails;
	}

	public void setGroupEmails(String groupEmails) {
		this.groupEmails = groupEmails;
	}

	
	
	public String getResponsable() {
		return responsable;
	}

	public void setResponsable(String responsable) {
		this.responsable = responsable;
	}

	public boolean isAuthPswChange() {
		return authPswChange;
	}

	public void setAuthPswChange(boolean authPswChange) {
		this.authPswChange = authPswChange;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public String getDepartement() {
		return departement;
	}

	public void setDepartement(String departement) {
		this.departement = departement;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}


	public LocalDateTime getCreated_At() {
		return created_At;
	}

	public void setCreated_At(LocalDateTime created_At) {
		this.created_At = created_At;
	}

	public LocalDateTime getUpdated_At() {
		return updated_At;
	}

	public void setUpdated_At(LocalDateTime updated_At) {
		this.updated_At = updated_At;
	}

	public String getMatricule() {
		return matricule;
	}

	public void setMatricule(String matricule) {
		this.matricule = matricule;
	}

	public String getFonction() {
		return fonction;
	}

	public void setFonction(String fonction) {
		this.fonction = fonction;
	}

	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}

	@PrePersist
    protected void onCreate() {
        this.created_At = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        this.updated_At = LocalDateTime.now();
    }
    
    
	
	
}
