package com.lear.MGCMS.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MatriculeAlreadyExistsException extends RuntimeException {

	public MatriculeAlreadyExistsException(String message) {
		super(message);
	}
	
}
