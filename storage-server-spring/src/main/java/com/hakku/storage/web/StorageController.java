package com.hakku.storage.web;

import java.io.InputStream;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.hakku.storage.domain.ImageKind;
import com.hakku.storage.domain.ImageMetadata;
import com.hakku.storage.metrics.StorageMetrics;
import com.hakku.storage.storage.ImageNotFoundException;
import com.hakku.storage.storage.ImageStore;

@RestController
public class StorageController {

	private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

	private static String normalizeContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return DEFAULT_CONTENT_TYPE;
		}
		int semi = contentType.indexOf(';');
		return semi >= 0 ? contentType.substring(0, semi).trim() : contentType.trim();
	}


	private final ImageStore store;
	private final StorageMetrics metrics;

	public StorageController(ImageStore store, StorageMetrics metrics) {
		this.store = store;
		this.metrics = metrics;
	}

	@PostMapping("/storage/images")
	public ResponseEntity<ImageMetadata> upload(
			@RequestParam(value = "kind", required = false) String kindParam,
			@RequestHeader(value = "Content-Type", required = false) String contentType,
			InputStream body) throws Exception {
		ImageKind kind = ImageKind.fromQuery(kindParam);
		contentType = normalizeContentType(contentType);
		ImageMetadata meta = store.put(kind, contentType, body);
		metrics.recordUpload(kind);
		return ResponseEntity.status(HttpStatus.CREATED).body(meta);
	}

	@GetMapping("/storage/images/{id}")
	public ResponseEntity<StreamingResponseBody> download(@PathVariable String id) throws Exception {
		try {
			ImageStore.StoredImage stored = store.get(id);
			StreamingResponseBody stream = output -> {
				try (InputStream in = stored.stream()) {
					in.transferTo(output);
				}
			};
			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType(stored.metadata().contentType()))
					.contentLength(stored.metadata().size())
					.body(stream);
		} catch (ImageNotFoundException ex) {
			return ResponseEntity.notFound().build();
		}
	}

	@GetMapping("/storage/images/{id}/meta")
	public ResponseEntity<ImageMetadata> metadata(@PathVariable String id) throws Exception {
		try {
			return ResponseEntity.ok(store.stat(id));
		} catch (ImageNotFoundException ex) {
			return ResponseEntity.notFound().build();
		}
	}

	@DeleteMapping("/storage/images/{id}")
	public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
		try {
			store.delete(id);
			return ResponseEntity.noContent().build();
		} catch (ImageNotFoundException ex) {
			return ResponseEntity.notFound().build();
		}
	}
}
