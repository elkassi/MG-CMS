package com.lear.MGCMS.controller;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.apache.commons.lang3.RandomStringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.payload.JWTLoginSuccessResponse;
import com.lear.MGCMS.payload.LoginRequest;
import com.lear.MGCMS.security.JwtTokenProvider;
import com.lear.MGCMS.services.EmailService;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.validator.UserValidator;

import java.time.LocalDateTime;

import static com.lear.MGCMS.security.SecurityConstants.*;


@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @Autowired
    private UserService service;
    
    @Autowired
    private UserValidator userValidator;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    
    @Autowired
    private EmailService emailService;
    
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
	public Page<User> findAll(
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy
			) {
		return service.findAll(page,size,sortBy);
	}
    
    @PostMapping("/login")
    public ResponseEntity<?> authenticationUser(@Valid @RequestBody LoginRequest loginRequest, BindingResult result){
    	ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
    	if(errorMap != null) return errorMap;
    	User user = service.findByUsername(loginRequest.getUsername());
    	if(user != null && user.isActive()) {
    		Authentication authentication = authenticationManager.authenticate(
        			new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        	);
        	SecurityContextHolder.getContext().setAuthentication(authentication);
        	
        	String jwt = TOKEN_PREFIX + tokenProvider.generateToken(authentication);
        	
        	
        	return ResponseEntity.ok(new JWTLoginSuccessResponse(true, jwt));
    	}
    	
    	return new ResponseEntity<String>("Erreur d'authentification", HttpStatus.UNAUTHORIZED);
    	
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addUser(@Valid @RequestBody User user, BindingResult result) {
    	int length = 10;
	    boolean useLetters = true;
	    boolean useNumbers = true;
	    String password = RandomStringUtils.random(length, useLetters, useNumbers);
    	if(user.getPassword() == null) {
    	    user.setPassword(bCryptPasswordEncoder.encode(password));
    	} else {
    		User newUser = service.saveUser(user);
    		return new ResponseEntity<String>("updated", HttpStatus.OK);
    	}
    	
    	ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if(errorMap != null) return errorMap;
        
        User newUser = service.saveUser(user);
        return new ResponseEntity<String>(password, HttpStatus.CREATED);
    }

	@Value("${lear.linkServer}")
	private String linkServer;

	@PostMapping("/reset/{id}")
	@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetPsw(@PathVariable String id) {
    	int length = 10;
	    boolean useLetters = true;
	    boolean useNumbers = true;
	    String password = RandomStringUtils.random(length, useLetters, useNumbers);
    	User user = service.findByMatricule(id);
    	if(user == null) {
    		return new ResponseEntity<String>("Utilisateur introuvable", HttpStatus.NOT_FOUND);
    	}
    	user.setPassword(bCryptPasswordEncoder.encode(password));
    	User newUser = service.saveUser(user);
    	emailService.sendEmailAttachment(newUser.getEmail(), "Mot de Passe CMS-WEB", "<html>"
    			+ "<body>"
    			+ "<p>"
    				+ "Bonjour " + user.getFirstName() + " " + user.getLastName() + "<br/>"
    				+ "Votre compte de système CMS-WEB est :<br/><br/>"
    				+ "Login : " + newUser.getUsername()+ "<br/>"
    				+ "Password : " + password
    				+ "<br/><br/>"
    				+ "NB: Vous pouvez changer votre mot de passe en cliquant sur votre profile"
    				+ "<br/><br/>"
    				+ "Envoyé pas <a href='"+linkServer+"'>CMS-WEB</a>"
    			+ "</p>"
    			+ "</body>"
    			+ "</html>");
        return new ResponseEntity<String>(password, HttpStatus.CREATED);
    }

	@GetMapping("/resetFast/{id}")
	public ResponseEntity<?> resetFast(@PathVariable String id,
		@RequestParam(value = "code", required = true) String code
	) {
		User user = service.findByMatricule(id);
		if(user == null) {
			return new ResponseEntity<String>("UNAUTHORIZED REQUEST", HttpStatus.UNAUTHORIZED);
		}

		// compare the current localdatetime with the one saved in user requestRestPasswordDate, now - requestRestPasswordDate must be less than 6 hours
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime requestRestPasswordDate = user.getRequestRestPasswordDate();
		if(requestRestPasswordDate == null) {
			return new ResponseEntity<String>("UNAUTHORIZED REQUEST", HttpStatus.UNAUTHORIZED);
		}
		if(requestRestPasswordDate.plusHours(6).isBefore(now)) {
			return new ResponseEntity<String>("UNAUTHORIZED REQUEST", HttpStatus.UNAUTHORIZED);
		}

		if(!code.equals(user.getRestPasswordCode())) {
			return new ResponseEntity<String>("UNAUTHORIZED REQUEST", HttpStatus.UNAUTHORIZED);
		}


		int length = 10;
		boolean useLetters = true;
		boolean useNumbers = true;
		String password = RandomStringUtils.random(length, useLetters, useNumbers);
		user.setPassword(bCryptPasswordEncoder.encode(password));
		user.setRequestRestPasswordDate(null);
		user.setRestPasswordCode(null);
		User newUser = service.saveUser(user);
		emailService.sendEmailAttachment(newUser.getEmail(), "Mot de Passe CMS-WEB", "<html>"
				+ "<body>"
				+ "<p>"
				+ "Bonjour " + user.getFirstName() + " " + user.getLastName() + "<br/>"
				+ "Votre compte de système CMS-WEB est :<br/><br/>"
				+ "Login : " + newUser.getUsername()+ "<br/>"
				+ "Password : " + password
				+ "<br/><br/>"
				+ "NB: Vous pouvez changer votre mot de passe en cliquant sur votre profile"
				+ "<br/><br/>"
				+ "Envoyé pas <a href='"+linkServer+"'>CMS-WEB</a>"
				+ "</p>"
				+ "</body>"
				+ "</html>");
		return new ResponseEntity<String>(password, HttpStatus.CREATED);
	}


	@GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable String userId) {
    	User user = service.findByMatricule(userId);
    	return new ResponseEntity<User>(user, HttpStatus.OK);
    }
    
    @PostMapping("/changePsw")
    public ResponseEntity<?> changePsw(@Valid @RequestBody User user, BindingResult result, Authentication authentication) {
    	User oldUser = service.findByUsername(authentication.getName());
    	userValidator.validate(user, result);
    	if(user.getMatricule() == null) {
    		return new ResponseEntity<String>("UNAUTHORIZED REQUEST", HttpStatus.UNAUTHORIZED);
    	}
    	if(!user.getMatricule().equals(oldUser.getMatricule())) {
    		return new ResponseEntity<String>("vous ne pouvey pas changer le mot de passe des autres", HttpStatus.UNAUTHORIZED);
    	}
    	ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if(errorMap != null) return errorMap;
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        User newUser = service.saveUser(user);
        return new ResponseEntity<User>(newUser, HttpStatus.CREATED);
    }

	@GetMapping("/emailConfirmation")
	public ResponseEntity<?> emailConfirmation(
			@RequestParam(value = "username", required = true) String username
			) {
		username = username.trim();
		User oldUser = service.findByUsername(username);
		if(oldUser == null) {
			return new ResponseEntity<String>("User not found", HttpStatus.NOT_FOUND);
		}
		int length = 10;
		boolean useLetters = true;
		boolean useNumbers = true;
		String random = RandomStringUtils.random(length, useLetters, useNumbers);
		oldUser.setRestPasswordCode(random);
		oldUser.setRequestRestPasswordDate(LocalDateTime.now());
		service.saveUser(oldUser);
		// encrypt the current timestamp
		emailService.sendEmailAttachment(oldUser.getEmail(), "Mot de Passe CMS-WEB", "<html>"
				+ "<body>"
				+ "<p>"
				+ "Bonjour " + oldUser.getFirstName() + " " + oldUser.getLastName() + "<br/>"
				+ "Votre compte de système CMS-WEB est :<br/><br/>"
				+ "Login : " + oldUser.getUsername()+ "<br/>"
				+ "<a href='"+linkServer+"/api/user/resetFast/"+oldUser.getMatricule()+"?code="+random
				+"'>Click ici</a> pour réinitialiser votre mot de passe"
				+ "<br/><br/>"
				+ "Envoyé pas <a href='"+linkServer+"'>CMS-WEB</a>"
				+ "</p>"
				+ "</body>"
				+ "</html>");

		return new ResponseEntity<String>("email envoyer", HttpStatus.CREATED);
	}

}
