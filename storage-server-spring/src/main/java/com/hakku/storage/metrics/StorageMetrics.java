package com.hakku.storage.metrics;

import org.springframework.stereotype.Component;

import com.hakku.storage.domain.ImageKind;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class StorageMetrics {

	private final MeterRegistry registry;

	public StorageMetrics(MeterRegistry registry) {
		this.registry = registry;
	}

	public void recordUpload(ImageKind kind) {
		registry.counter("storage_uploads_total", "kind", kind.getValue()).increment();
	}
}
