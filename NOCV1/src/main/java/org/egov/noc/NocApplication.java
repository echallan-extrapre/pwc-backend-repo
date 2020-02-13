package org.egov.noc;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.egov.tracer.model.CustomException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class NocApplication {

	@Value("${egov.validation.addupdate.json.path}")
	private String configValidationAddUpdatePaths;

	@Value("${egov.validation.app.status.json.path}")
	private String configValidationApproveRejectPaths;
	
	@Autowired
	public static ResourceLoader resourceLoader;

	public NocApplication(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(NocApplication.class, args);
	}

	@PostConstruct
	@Bean(name = "validatorAddUpdateJSON")
	public JSONObject loadValidationSourceConfigs() {
		Map<String, String> errorMap = new HashMap<>();
		JSONObject jsonObject = new JSONObject();
		ObjectMapper mapper = new ObjectMapper();
		log.info("====================== EGOV NOC SERVICE ======================");
		log.info("LOADING CONFIGS VALIDATION : " + configValidationAddUpdatePaths);
		try {
			log.info("Attempting to load config: " + configValidationAddUpdatePaths);

			if (configValidationAddUpdatePaths.startsWith("https://")
					|| configValidationAddUpdatePaths.startsWith("http://")) {
				log.info("Reading....: " + configValidationAddUpdatePaths);

				URL jsonFile = new URL(configValidationAddUpdatePaths);
				jsonObject = mapper.readValue(new InputStreamReader(jsonFile.openStream()), JSONObject.class);

				log.info("Parsed: " + configValidationAddUpdatePaths);

			} else if (configValidationAddUpdatePaths.startsWith("file://")
					|| configValidationAddUpdatePaths.startsWith("classpath:")) {
				log.info("Reading....: " + configValidationAddUpdatePaths);

				Resource resource = resourceLoader.getResource(configValidationAddUpdatePaths);
				File file = resource.getFile();
				jsonObject = mapper.readValue(file, JSONObject.class);

				log.info("Parsed to object: " + configValidationAddUpdatePaths);
			}
		} catch (Exception e) {
			log.error("Exception while fetching service map for: " + configValidationAddUpdatePaths, e);
			errorMap.put("FAILED_TO_FETCH_FILE", configValidationAddUpdatePaths);
		}

		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);
		else
			log.info("====================== VALIDATION CONFIGS LOADED SUCCESSFULLY! ====================== ");

		return jsonObject;
	}

	@PostConstruct
	@Bean(name = "validatorApproveRejectJSON")
	public JSONObject loadValidationSourceApproveRejectConfigs() {
		Map<String, String> errorMap = new HashMap<>();
		JSONObject jsonObject = new JSONObject();
		ObjectMapper mapper = new ObjectMapper();
		log.info("====================== EGOV NOC SERVICE ======================");
		log.info("LOADING CONFIGS VALIDATION : " + configValidationApproveRejectPaths);
		try {
			log.info("Attempting to load config: " + configValidationApproveRejectPaths);

			if (configValidationApproveRejectPaths.startsWith("https://")
					|| configValidationApproveRejectPaths.startsWith("http://")) {
				log.info("Reading....: " + configValidationApproveRejectPaths);

				URL jsonFile = new URL(configValidationApproveRejectPaths);
				jsonObject = mapper.readValue(new InputStreamReader(jsonFile.openStream()), JSONObject.class);

				log.info("Parsed: " + configValidationApproveRejectPaths);

			} else if (configValidationApproveRejectPaths.startsWith("file://")
					|| configValidationApproveRejectPaths.startsWith("classpath:")) {
				log.info("Reading....: " + configValidationApproveRejectPaths);

				Resource resource = resourceLoader.getResource(configValidationApproveRejectPaths);
				File file = resource.getFile();
				jsonObject = mapper.readValue(file, JSONObject.class);

				log.info("Parsed to object: " + configValidationApproveRejectPaths);
			}
		} catch (Exception e) {
			log.error("Exception while fetching service map for: " + configValidationApproveRejectPaths, e);
			errorMap.put("FAILED_TO_FETCH_FILE", configValidationApproveRejectPaths);
		}

		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);
		else
			log.info("====================== VALIDATION CONFIGS LOADED SUCCESSFULLY! ====================== ");

		return jsonObject;
	}
}
