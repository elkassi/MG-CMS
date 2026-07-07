package com.lear.MGCMS.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.Site;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.exceptions.MatriculeAlreadyExistsException;
import com.lear.MGCMS.exceptions.UsernameAlreadyExistsException;
import com.lear.MGCMS.repositories.RoleRepository;
import com.lear.MGCMS.repositories.UserRepository;
import org.springframework.data.domain.Sort;

@Service
public class UserService {
	
	@Autowired
    private UserRepository userRepository;
	
	@Autowired
	private RoleRepository roleRepository;
	
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    
    public ArrayList<User> finAllByRoleAndSite(String roleGest) {
		Role role = new Role();
		for(Role testRole:roleRepository.findAll()) {
			if(testRole.getName().equals(roleGest)) {
				role = testRole;
			}
		}
		
		
		ArrayList<User> users = new ArrayList<User>();
		for(User user:userRepository.findAll()) {
			/*String sitesUser = "";
			for(Site siteTest : user.getSites()) {
				sitesUser += siteTest.getNom();
			}*/
			if(user.getRoles().contains(role)) {
				users.add(user);
				System.out.println(user.getMatricule() + " : success");
			} else {
				System.out.println(user.getMatricule() + " : failed");
			}
			
		}
		return users;
		
		
	}

    public User saveUser(User newUser){
        
       try {
           // Username has to be unique
           newUser.setUsername(newUser.getUsername());
       }catch(UsernameAlreadyExistsException err) {
    	   throw new UsernameAlreadyExistsException("Username '"+ newUser.getUsername() + "' already exist");
       }
       User userTest = userRepository.findByMatricule(newUser.getMatricule());
	   if(
			   (newUser.getMatricule() == null && userTest != null) 
			   || (newUser.getMatricule() != null && userTest != null 
			   && !userTest.getMatricule().equals(newUser.getMatricule()))
			   ) {
		   throw new MatriculeAlreadyExistsException("Matricule '"+ newUser.getMatricule() + "' already exist");
	   }
       // Make sure the password and confirmationPassword match
       // We dont't persist or show the confirm Password
       newUser.setConfirmPassword("");
       Set<Role> roles = new HashSet<Role>();
       Set<Role> userRoles = newUser.getRoles();
       if (!userRoles.isEmpty()) {
    	   for(Role role: newUser.getRoles()) {
        	   Role roleDB = roleRepository.findByName(role.getName());
               roles.add(roleDB);
           }
           newUser.setRoles(roles);
       }
       
       return userRepository.save(newUser);
     
    }
    
    public User findFirstByFullName(String name) {
    	return userRepository.findFirstByFullName(name);
    }
    
    
    public Iterable<User> findAll() {
    	//return userRepository.findAllByActiveTrue();
    	return userRepository.findAll();
    }
    
    public User findByMatricule(String matricule) {
    	return userRepository.findByMatricule(matricule);
    }
    
    public void deleteUser(String matricule) {
    	User user = userRepository.findByMatricule(matricule);
    	user.setActive(false);
    	userRepository.save(user);
    }
    
    public User findByUsername(String username) {
    	return userRepository.findByUsername(username);
    }

    public Page<User> findAll(int page, int size, String sort) {
		String[] sortArr = sort.split(",");
        String evalSort = sortArr[0];
        String sortDirection = sortArr[1];
        Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
        Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection,evalSort).ignoreCase());
        
		return userRepository.findAll(PageRequest.of(page, size, sortOrderIgnoreCase));
	}
	
	private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
        if (sortDirection.equalsIgnoreCase("desc")){
            return Sort.Direction.DESC;
        } else {
            return Sort.Direction.ASC;
        }
    }

}
