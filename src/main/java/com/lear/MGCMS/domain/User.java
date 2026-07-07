package com.lear.MGCMS.domain;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonFormat;


@Entity
@Table(name="users")
public class User {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@NotBlank(message = "il faut saisir la matricule")
    private String matricule;
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;

    // @Email(message = "username needs to be an email")
    @NotBlank(message = "Username is required")
    @Column(unique = true)
    private String username;

    @NotBlank(message = "Please enter your first name")
    private String firstName;
    
    @NotBlank(message = "Please enter your last name")
    private String lastName;
    
    private String fonction;
    
    private String email;
    
    private String departement;
    
    private String chefDirect;

    @ManyToOne
	private Site site;
    
    private String password;

    @Transient
    private String confirmPassword;
    
    private boolean active = true;
    
    private String ipPrinter;

	private LocalDateTime requestRestPasswordDate;
	private String restPasswordCode;

	public String getRestPasswordCode() {
		return restPasswordCode;
	}

	public void setRestPasswordCode(String restPasswordCode) {
		this.restPasswordCode = restPasswordCode;
	}

	public LocalDateTime getRequestRestPasswordDate() {
		return requestRestPasswordDate;
	}

	public void setRequestRestPasswordDate(LocalDateTime requestRestPasswordDate) {
		this.requestRestPasswordDate = requestRestPasswordDate;
	}

	public String getIpPrinter() {
		return ipPrinter;
	}

	public void setIpPrinter(String ipPrinter) {
		this.ipPrinter = ipPrinter;
	}

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

 	@ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_matricule", referencedColumnName = "matricule"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
            )
// 	@NotEmpty(message = "il faut spécifier son role")
    private Set<Role> roles = new HashSet<>();
 	    
 	
 	
	public User(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public String getChefDirect() {
		return chefDirect;
	}

	public void setChefDirect(String chefDirect) {
		this.chefDirect = chefDirect;
	}

	public User() {
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

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
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
