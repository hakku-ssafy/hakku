package com.hakku.storage;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "storage.base-path=${java.io.tmpdir}/hakku-spring-storage-test")
class StorageApiTest {

	@Autowired
	private MockMvc mvc;

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void uploadDownloadDelete() throws Exception {
		MvcResult created = mvc.perform(post("/storage/images?kind=result")
				.contentType(MediaType.IMAGE_PNG)
				.content("fake-png".getBytes()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.kind").value("result"))
				.andExpect(jsonPath("$.size").value(8))
				.andReturn();

		JsonNode node = mapper.readTree(created.getResponse().getContentAsString());
		String id = node.get("id").asText();

		mvc.perform(get("/storage/images/" + id))
				.andExpect(status().isOk());

		mvc.perform(get("/storage/images/" + id + "/meta"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contentType").value("image/png"));

		mvc.perform(delete("/storage/images/" + id))
				.andExpect(status().isNoContent());

		mvc.perform(get("/storage/images/" + id))
				.andExpect(status().isNotFound());
	}

	@Test
	void invalidKindReturns400() throws Exception {
		mvc.perform(post("/storage/images?kind=bogus")
				.contentType(MediaType.IMAGE_PNG)
				.content("x".getBytes()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid image kind"));
	}
}
