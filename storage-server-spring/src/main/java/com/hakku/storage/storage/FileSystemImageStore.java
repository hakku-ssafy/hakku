package com.hakku.storage.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;
import com.hakku.storage.domain.ImageKind;
import com.hakku.storage.domain.ImageMetadata;

@Component
public class FileSystemImageStore implements ImageStore {

	private final Path basePath;
	private final ObjectMapper objectMapper;
	private final SecureRandom random = new SecureRandom();

	public FileSystemImageStore(StorageProperties properties, ObjectMapper objectMapper) throws IOException {
		this.basePath = Path.of(properties.basePath()).toAbsolutePath().normalize();
		this.objectMapper = objectMapper;
		Files.createDirectories(this.basePath);
	}

	@Override
	public ImageMetadata put(ImageKind kind, String contentType, InputStream body) throws Exception {
		String id = newId();
		Path blob = blobPath(id);
		long size;
		try (body) {
			size = Files.copy(body, blob);
		}
		ImageMetadata meta = new ImageMetadata(id, kind, contentType, size, Instant.now());
		writeMeta(meta);
		return meta;
	}

	@Override
	public StoredImage get(String id) throws Exception {
		ImageMetadata meta = readMeta(id);
		Path blob = blobPath(id);
		if (!Files.exists(blob)) {
			throw new ImageNotFoundException();
		}
		return new StoredImage(Files.newInputStream(blob), meta);
	}

	@Override
	public ImageMetadata stat(String id) throws Exception {
		return readMeta(id);
	}

	@Override
	public void delete(String id) throws Exception {
		readMeta(id);
		Files.deleteIfExists(blobPath(id));
		Files.deleteIfExists(metaPath(id));
	}

	private ImageMetadata readMeta(String id) throws Exception {
		if (!safeId(id)) {
			throw new ImageNotFoundException();
		}
		Path metaFile = metaPath(id);
		if (!Files.exists(metaFile)) {
			throw new ImageNotFoundException();
		}
		return objectMapper.readValue(Files.readString(metaFile), ImageMetadata.class);
	}

	private void writeMeta(ImageMetadata meta) throws Exception {
		objectMapper.writeValue(metaPath(meta.id()), meta);
	}

	private Path blobPath(String id) {
		return basePath.resolve(id + ".bin");
	}

	private Path metaPath(String id) {
		return basePath.resolve(id + ".json");
	}

	private static boolean safeId(String id) {
		return id != null && !id.isEmpty() && !id.contains("/") && !id.contains("\\") && !id.contains("..");
	}

	private String newId() {
		byte[] buf = new byte[16];
		random.nextBytes(buf);
		return HexFormat.of().formatHex(buf);
	}
}
