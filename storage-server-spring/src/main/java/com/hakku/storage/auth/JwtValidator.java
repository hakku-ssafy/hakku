package com.hakku.storage.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * main-server(JJWT) 및 Go storage-server와 동일한 HS256 JWT 검증.
 * 시크릿은 base64 디코딩된 raw key bytes.
 */
public final class JwtValidator {

	private final byte[] key;
	private final ObjectMapper mapper = new ObjectMapper();

	public JwtValidator(String base64Secret) {
		this.key = Base64.getDecoder().decode(base64Secret);
	}

	public String subjectFromBearer(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			throw new UnauthorizedException("authentication required");
		}
		return subject(authorizationHeader.substring("Bearer ".length()));
	}

	String subject(String token) {
		String[] parts = token.split("\\.", 3);
		if (parts.length != 3) {
			throw new UnauthorizedException("invalid token");
		}
		verifySignature(parts[0], parts[1], parts[2]);
		JsonNode payload = parsePayload(parts[1]);
		long exp = payload.path("exp").asLong(0);
		if (exp > 0 && System.currentTimeMillis() / 1000 > exp) {
			throw new UnauthorizedException("token expired");
		}
		JsonNode subNode = payload.path("sub");
		if (subNode.isMissingNode() || subNode.isNull()) {
			throw new UnauthorizedException("missing subject");
		}
		String sub = subNode.asString();
		if (sub.isBlank()) {
			throw new UnauthorizedException("missing subject");
		}
		return sub;
	}

	private void verifySignature(String header, String payload, String signaturePart) {
		try {
			byte[] expected = Base64.getUrlDecoder().decode(signaturePart);
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(key, "HmacSHA256"));
			byte[] actual = mac.doFinal((header + "." + payload).getBytes(StandardCharsets.US_ASCII));
			if (!constantTimeEquals(expected, actual)) {
				throw new UnauthorizedException("invalid signature");
			}
		} catch (UnauthorizedException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new UnauthorizedException("invalid signature");
		}
	}

	private JsonNode parsePayload(String payloadPart) {
		try {
			byte[] decoded = Base64.getUrlDecoder().decode(payloadPart);
			return mapper.readTree(decoded);
		} catch (Exception ex) {
			throw new UnauthorizedException("invalid payload");
		}
	}

	private static boolean constantTimeEquals(byte[] a, byte[] b) {
		if (a.length != b.length) {
			return false;
		}
		int result = 0;
		for (int i = 0; i < a.length; i++) {
			result |= a[i] ^ b[i];
		}
		return result == 0;
	}
}
