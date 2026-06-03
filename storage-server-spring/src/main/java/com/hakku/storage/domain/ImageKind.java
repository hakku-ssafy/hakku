package com.hakku.storage.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ImageKind {
	RAW("raw"),
	RESULT("result");

	private final String value;

	ImageKind(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static ImageKind fromValue(String raw) {
		if (raw == null || raw.isBlank()) {
			return RAW;
		}
		for (ImageKind kind : values()) {
			if (kind.value.equals(raw)) {
				return kind;
			}
		}
		throw new IllegalArgumentException("invalid image kind");
	}

	public static ImageKind fromQuery(String raw) {
		return fromValue(raw);
	}
}
