package org.egov.noc.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.noc.config.ApplicationProperties;
import org.egov.noc.model.ErrorResponseInfo;
import org.egov.noc.model.Errors;
import org.egov.noc.model.NOCApplication;
import org.egov.noc.model.NOCApplicationDetail;
import org.egov.noc.model.NOCDetailsRequestData;
import org.egov.noc.model.NOCRequestData;
import org.egov.noc.model.RequestData;
import org.egov.noc.repository.NocRepository;
import org.egov.noc.repository.querybuilder.QueryBuilder;
import org.egov.noc.util.CommonConstants;
import org.egov.noc.web.contract.NocResponse;
import org.egov.noc.web.contract.ReponseData;
import org.egov.noc.web.contract.factory.ResponseFactory;
import org.egov.tracer.model.CustomException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NocService {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Autowired
	private ApplicationProperties appProps;

	@Autowired
	private NocRepository nocRepository;

	@Autowired
	private ResponseFactory responseInfoFactory;

	@Autowired
	@Qualifier("validatorAddUpdateJSON")
	private JSONObject jsonAddObject;

	@Autowired
	@Qualifier("validatorApproveRejectJSON")
	private JSONObject jsonApproveRejectObject;

	public NocResponse searchNoc(RequestData requestInfo) {

		Object nocs = nocRepository.findPets(requestInfo);
		return NocResponse.builder()
				.resposneInfo(responseInfoFactory.getResponseInfo(requestInfo.getRequestInfo(), HttpStatus.OK))
				.nocApplicationDetail(nocs).build();
	}

	public NocResponse viewNoc(RequestData requestInfo) {

		Object nocs = nocRepository.viewNoc(requestInfo);
		return NocResponse.builder()
				.resposneInfo(responseInfoFactory.getResponseInfo(requestInfo.getRequestInfo(), HttpStatus.OK))
				.nocApplicationDetail(nocs).build();
	}

	public NocResponse searchApplicaion(RequestData py) {
		List<NOCApplicationDetail> nocs = nocRepository.findPet(py.getDataPayload().get("applicationUuid").toString(),
				py.getApplicationStatus());
		return NocResponse.builder()
				.resposneInfo(responseInfoFactory.getResponseInfo(py.getRequestInfo(), HttpStatus.OK))
				.nocApplicationDetail(nocs).build();
	}

	public NocResponse getCalculations(RequestData requestData) throws ParseException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("totalAmount", nocRepository.calculations(requestData));

		return NocResponse.builder()
				.resposneInfo(responseInfoFactory.getResponseInfo(requestData.getRequestInfo(), HttpStatus.OK))
				.nocApplicationDetail(jsonObject).build();
	}

	//// add
	public ReponseData saveNoc(RequestData requestData) {
		String responseValidate = "";
		String status = requestData.getApplicationStatus();
		ReponseData reponseData = null;
		try {
			if (!requestData.getApplicationStatus().equals(CommonConstants.DRAFT)) {
				responseValidate = validateJsonAddUpdateData(requestData);
			}
			if (responseValidate.equals("")) {
				String applicationId = nocRepository.saveValidateStatus(requestData, status);
				if (applicationId != null) {
					reponseData = new ReponseData();
					ResponseInfo responseInfo = new ResponseInfo();
					responseInfo.setStatus("SUCCESS");
					reponseData.setApplicationId(applicationId);
					reponseData.setDataPayload(requestData.getDataPayload());
					reponseData.setResponseInfo(responseInfo);
				} else {
					reponseData = new ReponseData();
					ResponseInfo responseInfo = new ResponseInfo();
					responseInfo.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.toString());
					responseInfo.setMsgId("ID Generation Failed");
					reponseData.setResponseInfo(responseInfo);
				}
			} else {
				reponseData = new ReponseData();
				ResponseInfo responseInfo = new ResponseInfo();
				responseInfo.setStatus("fail");
				responseInfo.setResMsgId(responseValidate);
				reponseData.setResponseInfo(responseInfo);
				reponseData.setApplicationType(requestData.getApplicationType());
				reponseData.setAuditDetails(requestData.getAuditDetails());
				reponseData.setDataPayload(requestData.getDataPayload());

			}

		} catch (Exception e) {
			log.debug("PetsService createAsync:" + e);
			throw new CustomException("EGBS_PETS_SAVE_ERROR", e.getMessage());

		}

		return reponseData;
	}

	public NocResponse getRemarksForNoc(RequestData requestInfo) {

		return NocResponse.builder()
				.resposneInfo(responseInfoFactory.getResponseInfo(requestInfo.getRequestInfo(), HttpStatus.OK))
				.nocApplicationDetail(
						nocRepository.getRemarksForNoc(requestInfo.getApplicationId(), requestInfo.getTenantId()))
				.build();
	}

	public ReponseData updateNoc(RequestData requestData) {
		String responseValidate = "";
		ReponseData reponseData = new ReponseData();
		ResponseInfo res = new ResponseInfo();
		try {
			responseValidate = validateJsonAddUpdateData(requestData);
			if (responseValidate.equals("")) {
				String applicationId = requestData.getApplicationId();
				if (applicationId != null) {
					int applicationcount = nocRepository.validateApplicationId(applicationId);
					if (applicationcount > 0) {
						nocRepository.updateNOC(requestData, applicationId);
						res.setStatus(CommonConstants.SUCCESS);
					} else {
						res.setStatus(CommonConstants.FAIL);
						res.setMsgId("Invalid Application Id - No Application Id Found: ["
								+ requestData.getApplicationId() + "]");
					}
				} else {
					res.setStatus(CommonConstants.FAIL);
					res.setMsgId("Kindly provide Application Id");
				}
			} else {
				res.setStatus(CommonConstants.FAIL);
				res.setMsgId(responseValidate);
			}
		} catch (Exception e) {
			log.debug("PetsService createAsync:" + e);
			res.setStatus(CommonConstants.FAIL);
			res.setMsgId(CommonConstants.SAVEERROR + e.getMessage());
		}
		reponseData.setResponseInfo(res);

		return reponseData;
	}

	public JSONArray getColumnsRemarksForNoc(RequestData requestData) {
		JSONArray response = new JSONArray();
		try {

			String roleCode = null;
			List<Role> roleList = requestData.getRequestInfo().getUserInfo().getRoles();
			if (roleList != null && !roleList.isEmpty()) {
				Role roleObject = roleList.get(0);
				roleCode = roleObject.getCode();
				System.out.println("roleCode : " + roleCode);
			}

			if (roleCode != null && roleCode.isEmpty()) {
				JSONObject json = new JSONObject();
				json.put("Error", "Invalid User");
				response.add(json);
				return response;
			}

			JSONObject jsonColumnsValue = (JSONObject) new JSONParser().parse(jsonApproveRejectObject.toString());
			jsonColumnsValue = (JSONObject) jsonColumnsValue.get(requestData.getApplicationType());
			jsonColumnsValue = (JSONObject) jsonColumnsValue.get(roleCode);
			jsonColumnsValue = (JSONObject) jsonColumnsValue.get(requestData.getApplicationStatus());

			Set<String> keySets = jsonColumnsValue.keySet();

			for (String keys : keySets) {
				if (!keys.equals("applicationId")) {
					JSONObject intern = (JSONObject) jsonColumnsValue.get(keys);
					response.add(intern);
				}
			}

			System.out.println("jsonColumnss : " + jsonColumnsValue);
		} catch (Exception e) {
			log.info("Unable to read JSON file : Exception : " + e.getMessage());
		}
		return response;
	}

	private String validateJsonUpdateStatusData(RequestData requestData) throws ParseException {
		String responseText = "";
		try {
			String roleCode = null;
			JSONParser jsonParser = new JSONParser();

			List<Role> roleList = requestData.getRequestInfo().getUserInfo().getRoles();
			if (roleList != null && !roleList.isEmpty()) {
				Role roleObject = roleList.get(0);
				roleCode = roleObject.getCode();
			}
			if (roleCode != null && roleCode.isEmpty()) {
				return "Invalid Role";
			}

			JSONObject jsonValidator = (JSONObject) jsonParser.parse(jsonApproveRejectObject.toJSONString());
			jsonValidator = (JSONObject) jsonValidator.get(requestData.getApplicationType());
			jsonValidator = (JSONObject) jsonValidator.get(roleCode);
			jsonValidator = (JSONObject) jsonValidator.get(requestData.getApplicationStatus());
			JSONObject jsonRequested = (JSONObject) jsonParser.parse(requestData.getDataPayload().toString());

			if (jsonValidator == null || jsonRequested == null) {
				return "Invalid data to load the JSON file or requested data.";
			}
			responseText = commonValidation(jsonValidator, jsonRequested);
		} catch (Exception e) {
			responseText = "Unable to Process request => " + e.getMessage();
		}
		return responseText;
	}

	private String validateJsonAddUpdateData(RequestData requestData) throws ParseException {
		String responseText = "";
		try {
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonValidator = (JSONObject) jsonParser.parse(jsonAddObject.toJSONString());
			jsonValidator = (JSONObject) jsonValidator.get(requestData.getApplicationType());
			JSONObject jsonRequested = (JSONObject) jsonParser.parse(requestData.getDataPayload().toString());

			if (jsonValidator == null || jsonRequested == null) {
				return "Unable to load the JSON file or requested data.";
			}
			responseText = commonValidation(jsonValidator, jsonRequested);

		} catch (Exception e) {
			responseText = "Unable to Process request => " + e.getMessage();
		}
		return responseText;
	}

	private String commonValidation(JSONObject jsonValidator, JSONObject jsonRequested) {

		Set<String> keyValidateList = jsonValidator.keySet();
		Set<String> keyRequestedList = jsonRequested.keySet();
		StringBuilder responseText = new StringBuilder();
		try {
			if (keyValidateList.equals(keyRequestedList)) {

				for (String key : keyValidateList) {
					JSONObject actualValidate = (JSONObject) jsonValidator.get(key);
					String isMandatory = actualValidate.get("mandatory").toString();
					String isRegExpression = actualValidate.get("validateRegularExp").toString();
					String dataReq = jsonRequested.get(key).toString();

					if (isMandatory.equals("true") && dataReq.equals("")) {
						responseText.append(key + " : [Mandatory field]");
						responseText.append(",");
					} else {

						if (!dataReq.equals("")) {
							Pattern validatePattern = Pattern.compile(isRegExpression);
							if (!validatePattern.matcher(dataReq).matches()) {
								responseText.append(key + ":[Invalid data]");
								responseText.append(",");
							}
						}
					}
				}
				if (!responseText.toString().equals("")) {
					responseText = new StringBuilder(
							"Error at =>  " + responseText.substring(0, responseText.length() - 1));
				}
			} else {
				responseText = new StringBuilder("Invalid Requested Colunms");
			}

		} catch (Exception e) {
			responseText.append("Unable to Process request => ");
			responseText.append("Exceptions => " + e.getMessage());
		}

		return responseText.toString();
	}

	public ReponseData updateNocApplicationStatus(RequestData requestData) {
		String responseValidate = "";
		ReponseData reponseData = null;
		try {

			responseValidate = validateJsonUpdateStatusData(requestData);
			if (responseValidate.equals("")) {
				reponseData = nocRepository.updateApplicationStatus(requestData);
			} else {
				reponseData = new ReponseData();
				ResponseInfo responseInfo = new ResponseInfo();
				responseInfo.setStatus("fail");
				responseInfo.setResMsgId(responseValidate);
				reponseData.setResponseInfo(responseInfo);
				reponseData.setApplicationType(requestData.getApplicationType());
				reponseData.setAuditDetails(requestData.getAuditDetails());
				reponseData.setDataPayload(requestData.getDataPayload());
			}

		} catch (Exception e) {
			log.debug("PetsService approveRejectAsync:" + e);
			throw new CustomException("EGBS_PETS_SAVE_ERROR", e.getMessage());
		}
		return reponseData;
	}

	public JSONArray getColumnsForNoc(String nocType) {
		JSONArray response = new JSONArray();
		try {
			JSONObject jsonColumnsValue = (JSONObject) new JSONParser().parse(jsonAddObject.toString());
			jsonColumnsValue = (JSONObject) jsonColumnsValue.get(nocType);

			Set<String> keySets = jsonColumnsValue.keySet();

			for (String keys : keySets) {
				JSONObject intern = (JSONObject) jsonColumnsValue.get(keys);
				response.add(intern);
			}

			System.out.println("jsonColumnss : " + jsonColumnsValue);
		} catch (Exception e) {
			log.info("Unable to read JSON file : Exception : " + e.getMessage());
		}
		return response;
	}

}
