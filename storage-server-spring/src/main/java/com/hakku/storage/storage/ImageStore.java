package com.hakku.storage.storage;

import java.io.InputStream;

import com.hakku.storage.domain.ImageKind;
import com.hakku.storage.domain.ImageMetadata;

public interface ImageStore {

	ImageMetadata put(ImageKind kind, String contentType, String ownerId, InputStream body) throws Exception;

	StoredImage get(String id) throws Exception;

	ImageMetadata stat(String id) throws Exception;

	void delete(String id) throws Exception;

	record StoredImage(InputStream stream, ImageMetadata metadata) {
	}
}
