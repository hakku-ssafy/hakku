package com.hakku.storage.domain;

import java.time.Instant;

public record ImageMetadata(
		String id,
		ImageKind kind,
		String contentType,
		long size,
		Instant createdAt) {
}
