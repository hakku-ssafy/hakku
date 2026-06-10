package com.hakku.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import com.hakku.storage.auth.JwtValidator;
import com.hakku.storage.auth.UnauthorizedException;

class JwtValidatorTest {

	private static final byte[] KEY = "bench-jwt-secret-key-32bytes!!".getBytes(StandardCharsets.UTF_8);
	private static final String B64 = Base64.getEncoder().encodeToString(KEY);

	@Test
	void acceptsValidToken() {
		String token = token("42", System.currentTimeMillis() / 1000 + 3600);
		assertEquals("42", new JwtValidator(B64).subjectFromBearer("Bearer " + token));
	}

	@Test
	void rejectsMissingBearer() {
		var validator = new JwtValidator(B64);
		assertThrows(UnauthorizedException.class, () -> validator.subjectFromBearer(null));
	}

	@Test
	void rejectsExpiredToken() {
		String token = token("42", System.currentTimeMillis() / 1000 - 10);
		assertThrows(UnauthorizedException.class, () -> new JwtValidator(B64).subjectFromBearer("Bearer " + token));
	}

	private static String token(String sub, long exp) {
		String header = b64url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
		String payload = b64url("{\"sub\":\"" + sub + "\",\"exp\":" + exp + "}");
		String unsigned = header + "." + payload;
		return unsigned + "." + sign(unsigned);
	}

	private static String sign(String unsigned) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(KEY, "HmacSHA256"));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.US_ASCII)));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private static String b64url(String json) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}
}
