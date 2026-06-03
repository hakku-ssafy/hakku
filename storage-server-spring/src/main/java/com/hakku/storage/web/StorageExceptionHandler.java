package com.hakku.storage.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class StorageExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> invalidKind(IllegalArgumentException ex) {
		if ("invalid image kind".equals(ex.getMessage())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "invalid image kind"));
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
	}
}
