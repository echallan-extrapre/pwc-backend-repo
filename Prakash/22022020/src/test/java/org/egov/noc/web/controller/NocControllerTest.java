package org.egov.noc.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.egov.noc.service.NocService;
import org.egov.noc.util.FileUtils;
import org.egov.noc.web.contract.factory.ResponseFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(NocController.class)
public class NocControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private NocService nocService;

	@MockBean
	private ResponseFactory responseFactory;

	@Test
	public void testUpdateApplicationStatus() throws IOException, Exception {

		mockMvc.perform(post("/egov-opmsService/noc/_updateappstatus").contentType(MediaType.APPLICATION_JSON)
				.content(getFileContents("updateStatusRequest.json"))).andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(content().json(getFileContents("updateStatusResponse.json")));
	}

	private String getFileContents(final String fileName) throws IOException {
		return new FileUtils().getFileContents(fileName);
	}

}
