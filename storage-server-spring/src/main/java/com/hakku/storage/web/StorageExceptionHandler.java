package com.hakku.storage.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hakku.storage.auth.ForbiddenException;
import com.hakku.storage.auth.UnauthorizedException;

@RestControllerAdvice
public class StorageExceptionHandler {

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<Map<String, String>> unauthorized(UnauthorizedException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<Map<String, String>> forbidden(ForbiddenException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> invalidKind(IllegalArgumentException ex) {
		if ("invalid image kind".equals(ex.getMessage())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "invalid image kind"));
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
	}
}
