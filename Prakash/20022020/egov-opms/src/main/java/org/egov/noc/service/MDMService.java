package org.egov.noc.service;

import static java.util.Objects.isNull;

import org.egov.common.contract.request.RequestInfo;
import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MDMService {

	@Autowired
	RestTemplate restTemplate;

	@Value("${egov.mdm.hostname}")
	private String host;

	@Value("${egov.mdm.uri}")
	private String path;

	public JSONArray getMasterCatagory(String tenantId, RequestInfo requestInfo) {

		String url = host + path + "?moduleName=egpm&tenantId=" + tenantId + "&masterName=typeOfAdvertisement";
		JSONArray jsonArray = null;
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode response = restTemplate.postForObject(url, requestInfo, JsonNode.class).findValue("MdmsRes")
				.findValue("egpm").findValue("typeOfAdvertisement");

		if (!isNull(response) && response.isArray()) {
			jsonArray = objectMapper.convertValue(response, JSONArray.class);
		}

		return jsonArray;
	}
	
	public JSONArray getMasterSubCatagory(String tenantId, RequestInfo requestInfo) {

		String url = host + path + "?moduleName=egpm&tenantId=" + tenantId + "&masterName=subTypeOfAdvertisement";
		JSONArray jsonArray = null;
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode response = restTemplate.postForObject(url, requestInfo, JsonNode.class).findValue("MdmsRes")
				.findValue("egpm").findValue("subTypeOfAdvertisement");

		if (!isNull(response) && response.isArray()) {
			jsonArray = objectMapper.convertValue(response, JSONArray.class);
		}

		return jsonArray;
	}
}
