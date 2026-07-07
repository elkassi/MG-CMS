package com.lear.MGCMS.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.repositories.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(username);
		
		if(user == null) new UsernameNotFoundException("User not found");
		return UserDetailsImpl.build(user);
		
	}
	
	@Transactional
	public UserDetails loadUserByMatricule(String matricule) {
		User user = userRepository.findByMatricule(matricule);
		if(user==null) new UsernameNotFoundException("User not found");
		return UserDetailsImpl.build(user);
	}
	
}
