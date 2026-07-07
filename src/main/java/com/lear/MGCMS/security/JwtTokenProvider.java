package com.lear.MGCMS.security;



import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.lear.MGCMS.security.SecurityConstants.*;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.lear.MGCMS.services.UserDetailsImpl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

@Component
public class JwtTokenProvider {
	
	//Generate the token

    public String generateToken(Authentication authentication){
    	UserDetailsImpl user = (UserDetailsImpl)authentication.getPrincipal();
        Date now = new Date();
    	/*List<String> roles = user.getAuthorities().stream()
				.map(item -> item.getAuthority())
				.collect(Collectors.toList());*/

        Date expiryDate = new Date(now.getTime()+EXPIRATION_TIME);

        String userId = user.getMatricule();

        Map<String,Object> claims = new HashMap<>();
        claims.put("matricule", user.getMatricule());
        claims.put("username", user.getUsername());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        claims.put("roles", user.getAuthorities());

        return Jwts.builder()
                .setSubject(userId)
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
        }

    //Validate the token
    public boolean validateToken(String token){
        try{
            Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token);
            return true;
        }catch (SignatureException ex){
            System.out.println("Invalid JWT Signature");
        }catch (MalformedJwtException ex){
            System.out.println("Invalid JWT Token");
        }catch (ExpiredJwtException ex){
            System.out.println("Expired JWT token : "+ ex.getMessage());
        }catch (UnsupportedJwtException ex){
            System.out.println("Unsupported JWT token");
        }catch (IllegalArgumentException ex){
            System.out.println("JWT claims string is empty");
        }
        return false;
    }


    //Get user Id from token

    public String getUserMatriculeFromJWT(String token){
        Claims claims = Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token).getBody();
        String matricule = (String)claims.get("matricule");

        return matricule;
    }

}
