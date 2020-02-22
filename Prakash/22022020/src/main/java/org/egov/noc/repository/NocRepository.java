package org.egov.noc.repository;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.noc.PreApplicationRunnerImpl;
import org.egov.noc.model.NOCApplication;
import org.egov.noc.model.NOCApplicationDetail;
import org.egov.noc.model.NOCApplicationRemark;
import org.egov.noc.model.NOCDetailsRequestData;
import org.egov.noc.model.NOCRemarksRequestData;
import org.egov.noc.model.NOCRequestData;
import org.egov.noc.model.RequestData;
import org.egov.noc.producer.Producer;
import org.egov.noc.repository.querybuilder.QueryBuilder;
import org.egov.noc.repository.rowmapper.ColumnsNocRowMapper;
import org.egov.noc.repository.rowmapper.CounterRowMapper;
import org.egov.noc.repository.rowmapper.NocRowMapper;
import org.egov.noc.service.IDGenUtil;
import org.egov.noc.service.MDMService;
import org.egov.noc.util.CommonConstants;
import org.egov.noc.web.contract.ReponseData;
import org.egov.noc.wf.model.ProcessInstance;
import org.egov.noc.wf.model.ProcessInstanceRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class NocRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private NocRowMapper nocRowMapper;

	@Autowired
	@Qualifier("validatorCalculationJSON")
	private JSONObject jsonCalculation;

	@Autowired
	private CounterRowMapper counterRowMapper;

	@Autowired
	private ColumnsNocRowMapper columnsNocRowMapper;

	@Autowired
	private IDGenUtil idgen;

	@Autowired
	MDMService mdmService;

	@Autowired
	private Producer producer;

	@Value("${persister.save.transition.noc.topic}")
	private String saveNOCTopic;

	@Value("${persister.save.transition.noc.details.topic}")
	private String saveNOCDetailsTopic;

	@Value("${persister.save.transition.nocapprovereject.topic}")
	private String saveNOCApproveRejectTopic;

	@Value("${persister.update.transition.noc.topic}")
	private String updateNOCTopic;

	@Value("${persister.update.transition.noc.status.topic}")
	private String updateStatusNOCTopic;

	@Value("${persister.update.transition.nocapprovereject.topic}")
	private String updateNOCApproveRejectTopic;

	@Value("${persister.update.transition.noc.details.topic}")
	private String updateNOCDetailsTopic;

	@Value("${persister.delete.transition.noc.details.topic}")
	private String deleteNOCDetailsTopic;

	@Autowired
	private PreApplicationRunnerImpl applicationRunnerImpl;

	public void updateNOC(RequestData requestData, String applicationId) {
		RequestInfo requestInfo = requestData.getRequestInfo();
		JSONObject dataPayLoad = requestData.getDataPayload();
		NOCApplication app = new NOCApplication();
		app.setApplicantName((dataPayLoad.get(CommonConstants.APPLICANTNAME) == null ? ""
				: dataPayLoad.get(CommonConstants.APPLICANTNAME).toString()));
		app.setHouseNo((dataPayLoad.get(CommonConstants.HOUSENO) == null ? ""
				: dataPayLoad.get(CommonConstants.HOUSENO).toString()));
		app.setSector((dataPayLoad.get(CommonConstants.SECTOR) == null ? ""
				: dataPayLoad.get(CommonConstants.SECTOR).toString()));
		app.setNocNumber(applicationId);
		app.setApplicationStatus(requestData.getApplicationStatus());

		List<NOCApplication> applist = Arrays.asList(app);
		NOCRequestData data = new NOCRequestData();
		data.setRequestInfo(requestInfo);
		data.setNocApplication(applist);
		producer.push(updateNOCTopic, data);
		// update set is active false

		// update detail table
		Long time = System.currentTimeMillis();
		String applicationDetailsId = UUID.randomUUID().toString();

		List<NOCApplicationDetail> preparedStatementValues = jdbcTemplate
				.query(QueryBuilder.SELECT_APPLICATION_DETAIL_QUERY, new Object[] { applicationId }, nocRowMapper);

		JSONObject dataPayload = requestData.getDataPayload();
		dataPayload.remove(CommonConstants.APPLICANTNAME);
		dataPayload.remove(CommonConstants.HOUSENO);
		dataPayload.remove(CommonConstants.SECTOR);
		NOCApplicationDetail nocappdetails = new NOCApplicationDetail();
		for (NOCApplicationDetail ps : preparedStatementValues) {
			nocappdetails.setApplicationDetailUuid(applicationDetailsId);
			nocappdetails.setApplicationUuid(ps.getApplicationUuid());
			nocappdetails.setApplicationDetail(requestData.getDataPayload().toJSONString());
			nocappdetails.setIsActive(true);
			nocappdetails.setCreatedBy(ps.getCreatedBy());
			nocappdetails.setCreatedTime(ps.getCreatedTime());
			nocappdetails.setLastModifiedBy(requestInfo.getUserInfo().getUuid());
			nocappdetails.setLastModifiedTime(time);
			List<NOCApplicationDetail> applist1 = Arrays.asList(nocappdetails);
			NOCDetailsRequestData data1 = new NOCDetailsRequestData();
			data1.setRequestInfo(requestInfo);
			data1.setNocApplicationDetails(applist1);
			producer.push(updateNOCDetailsTopic, data1);

			NOCApplicationDetail nocappdetails1 = new NOCApplicationDetail();
			nocappdetails1.setApplicationDetailUuid(ps.getApplicationDetailUuid());
			List<NOCApplicationDetail> applisttodelete = Arrays.asList(nocappdetails1);
			NOCDetailsRequestData data2 = new NOCDetailsRequestData();
			data2.setRequestInfo(requestInfo);
			data2.setNocApplicationDetails(applisttodelete);
			producer.push(deleteNOCDetailsTopic, data2);
		}
	}

	public JSONArray getRemarksForNoc(String appId, String tenantId) {
		return jdbcTemplate.query(QueryBuilder.GET_NOC_REMARKS_QUERY, new Object[] { tenantId, appId },
				columnsNocRowMapper);
	}

	public JSONArray findPets(RequestData requestInfo) {

		String roleCode = requestInfo.getRequestInfo().getUserInfo().getRoles().get(0).getCode();
		String tenantId = requestInfo.getTenantId();
		String requestType = requestInfo.getApplicationType();

		String queryString = "";
		if (roleCode != null && tenantId != null && requestType != null) {
			queryString = applicationRunnerImpl.getSqlQuery(tenantId, roleCode, requestType);
		}
		System.out.println("queryString : " + queryString);

		JSONObject jsonObject = requestInfo.getDataPayload();
		Set<String> keyList = jsonObject.keySet();
		for (String string : keyList) {
			StringBuilder values = new StringBuilder();
			String[] strParameters = jsonObject.get(string).toString().split(",");
			for (int i = 0; i < strParameters.length; i++) {
				values.append("'" + strParameters[i] + "',");
			}
			queryString = queryString.replace("[:" + string + ":]", values.substring(0, values.length() - 1));
		}

		if (!queryString.equals("")) {
			return jdbcTemplate.query(queryString, new Object[] {}, columnsNocRowMapper);
		} else {
			return new JSONArray();
		}
	}

	public JSONArray viewNoc(RequestData requestInfo) {
		try {
			if (!requestInfo.getApplicationId().isEmpty()) {
				JSONArray actualResult = jdbcTemplate.query(QueryBuilder.SELECT_VIEW_QUERY,
						new Object[] { requestInfo.getApplicationId() }, columnsNocRowMapper);
				JSONArray jsonArray = new JSONArray();
				JSONObject jsonObject = new JSONObject();
				String uUid = "";
				for (int i = 0; i < actualResult.size(); i++) {
					JSONObject jsonObject1 = (JSONObject) actualResult.get(i);
					for (int j = 0; j < jsonObject1.size(); j++) {
						jsonObject.put("applicationuuid", jsonObject1.get("applicationuuid"));
						uUid = jsonObject1.get("applicationuuid").toString();
						jsonObject.put("applicationId", jsonObject1.get("nocnumber"));
						jsonObject.put("applicationtype", jsonObject1.get("applicationtype"));
						jsonObject.put("applicationstatus", jsonObject1.get("applicationstatus"));
						jsonObject.put("houseNo", jsonObject1.get("housenumber"));
						jsonObject.put("sector", jsonObject1.get("sector"));
						jsonObject.put("applieddate", jsonObject1.get("applieddate"));
						jsonObject.put("applicantname", jsonObject1.get("applicantname"));

						JSONObject jsonObject2 = (JSONObject) new JSONParser()
								.parse(jsonObject1.get("applicationdetail").toString());
						Set<String> keys = jsonObject2.keySet();
						for (String key : keys) {
							jsonObject.put(key, jsonObject2.get(key));
						}
						// Remarks
						JSONArray jsonArrayResult = new JSONArray();
						if (uUid != null && !uUid.isEmpty()) {
							JSONArray actualRemarksResult = jdbcTemplate.query(QueryBuilder.ALL_REMARKS_QUERY,
									new Object[] { uUid }, columnsNocRowMapper);
							JSONObject jsonObject22 = new JSONObject();
							for (int n = 0; n < actualRemarksResult.size(); n++) {
								JSONObject jsonObject11 = (JSONObject) actualRemarksResult.get(n);
								Set<String> keyss = jsonObject11.keySet();
								for (String key : keyss) {
									jsonObject22.put(key, jsonObject11.get(key));
								}
								jsonArrayResult.add(jsonObject22);
							}
						}
						jsonObject.put(CommonConstants.REMARKS, jsonArrayResult);
					}
					jsonArray.add(jsonObject);
				}
				return jsonArray;
			} else {
				return new JSONArray();
			}
		} catch (Exception e) {
			return null;
		}
	}

	public List<NOCApplicationDetail> findPet(String applicationuuid, String status) {

		List<Object> preparedStatementValues = new ArrayList<>();
		String queryStr = QueryBuilder.getApplicationQuery();
		log.debug("query:::" + queryStr + "  preparedStatementValues::" + preparedStatementValues);

		Map<String, Object> params = new HashMap<>();
		params.put(CommonConstants.APPLICATIONUUID, applicationuuid);

		return jdbcTemplate.query(queryStr, new Object[] { applicationuuid, status }, nocRowMapper);

	}

	public String saveValidateStatus(RequestData requestData, String status) {
		String nocId = null;
		if (status.equals(CommonConstants.DRAFT)
				&& (requestData.getApplicationId() == null || requestData.getApplicationId().isEmpty())) {
			// Save as Draft for first time - new Record
			nocId = saveNoc(requestData, status);
		} else if (status.equals(CommonConstants.DRAFT)
				&& (requestData.getApplicationId() != null && !requestData.getApplicationId().isEmpty())) {
			// Update as Draft for all time - old Record
			nocId = requestData.getApplicationId();
			updateNOC(requestData, nocId);
		} else if (!status.equals(CommonConstants.DRAFT)
				&& (requestData.getApplicationId() != null && !requestData.getApplicationId().isEmpty())) {
			// Update as Submit for all time - old Record
			nocId = requestData.getApplicationId();
			updateNOC(requestData, nocId);

			// Call Workflow
			if (nocId != null) {
				workflowIntegration(nocId, requestData, status);
			} else {
				nocId = "Invalid Request";
			}
		} else {
			// Save as new entry
			nocId = saveNoc(requestData, status);
			// Call Workflow
			if (nocId != null) {
				workflowIntegration(nocId, requestData, status);
			} else {
				nocId = "Invalid Request";
			}
		}
		return nocId;
	}

	// add
	private String saveNoc(RequestData requestData, String status) {

		RequestInfo requestInfo = requestData.getRequestInfo();

		String nocId = idgen.generateApplicationId(requestData.getTenantId());
		int isApplicationIdExists = validateApplicationId(nocId);
		if (isApplicationIdExists == 0) {
			String applicationId = null;
			if (nocId != null) {
				JSONObject dataPayLoad = requestData.getDataPayload();
				Long time = System.currentTimeMillis();
				applicationId = UUID.randomUUID().toString();
				NOCApplication app = new NOCApplication();
				app.setApplicationUuid(applicationId);
				app.setTenantId(requestData.getTenantId());
				app.setNocNumber(nocId);
				app.setApplicantName((dataPayLoad.get(CommonConstants.APPLICANTNAME) == null ? ""
						: dataPayLoad.get(CommonConstants.APPLICANTNAME).toString()));
				app.setHouseNo((dataPayLoad.get(CommonConstants.HOUSENO) == null ? ""
						: dataPayLoad.get(CommonConstants.HOUSENO).toString()));
				app.setSector((dataPayLoad.get(CommonConstants.SECTOR) == null ? ""
						: dataPayLoad.get(CommonConstants.SECTOR).toString()));
				app.setAppliedDate(new Date().toLocaleString());
				app.setApplicationType(requestData.getApplicationType());
				app.setApplicationStatus(status);
				app.setIsActive(true);
				app.setCreatedBy(requestInfo.getUserInfo().getUuid());
				app.setCreatedTime(time);
				app.setLastModifiedBy(requestInfo.getUserInfo().getUuid());
				app.setLastModifiedTime(time);
				List<NOCApplication> applist = Arrays.asList(app);
				NOCRequestData data = new NOCRequestData();
				data.setRequestInfo(requestInfo);
				data.setNocApplication(applist);
				producer.push(saveNOCTopic, data);
				saveNOCDetails(requestData, applicationId);

				return nocId;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public int validateApplicationId(String applicationId) {
		List<Object> preparedStatementValues = new ArrayList<>();
		String queryStr = QueryBuilder.getApplicationQuery();
		log.debug("query:::" + queryStr + " preparedStatementValues::" + preparedStatementValues);

		Map<String, Object> params = new HashMap<>();
		params.put(CommonConstants.APPLICATIONUUID, applicationId);

		return jdbcTemplate.query(QueryBuilder.SELECT_APPLICATION_ID_QUERY, new Object[] { applicationId },
				counterRowMapper);

	}

	private ResponseInfo workflowIntegration(String applicationId, RequestData requestData, String status) {

		ProcessInstanceRequest workflowRequest = new ProcessInstanceRequest();
		workflowRequest.setRequestInfo(requestData.getRequestInfo());
		ProcessInstance processInstances = new ProcessInstance();
		processInstances.setTenantId(requestData.getTenantId());
		processInstances.setAction(status);
		processInstances.setBusinessId(applicationId);
		processInstances.setModuleName(requestData.getApplicationType());
		processInstances.setBusinessService(requestData.getApplicationType());
		List<ProcessInstance> processList = Arrays.asList(processInstances);
		workflowRequest.setProcessInstances(processList);
		return idgen.createWorkflowRequest(workflowRequest);
	}

	private ResponseInfo workflowIntegrationForPayment(String applicationId, RequestData requestData, String status) {

		ProcessInstanceRequest workflowRequest = new ProcessInstanceRequest();
		org.egov.common.contract.request.User userInfo = new org.egov.common.contract.request.User();
		userInfo.setUuid("e77f23de-9219-410f-be16-478328a184d9");
		userInfo.setId(165L);
		userInfo.setUserName("SYSTEM_PAYMENT");
		userInfo.setTenantId("CH");
		userInfo.setRoles(Arrays.asList(Role.builder().name("SYSTEM_PAYMENT").code("SYSTEM_PAYMENT").build()));
		RequestInfo requestInfo = requestData.getRequestInfo();
		requestInfo.setUserInfo(userInfo);
		workflowRequest.setRequestInfo(requestInfo);
		ProcessInstance processInstances = new ProcessInstance();
		processInstances.setTenantId(requestData.getTenantId());
		processInstances.setAction(status);
		processInstances.setBusinessId(applicationId);
		processInstances.setModuleName(requestData.getApplicationType());
		processInstances.setBusinessService(requestData.getApplicationType());
		List<ProcessInstance> processList = Arrays.asList(processInstances);
		workflowRequest.setProcessInstances(processList);
		return idgen.createWorkflowRequest(workflowRequest);
	}

	public void saveNOCDetails(RequestData requestData, String applicationId) {

		RequestInfo requestInfo = requestData.getRequestInfo();
		System.out.println("savePet requestInfo:" + applicationId);
		log.debug("savePet requestInfo:" + applicationId);
		JSONObject dataPayload = requestData.getDataPayload();
		dataPayload.remove(CommonConstants.APPLICANTNAME);
		dataPayload.remove(CommonConstants.HOUSENO);
		dataPayload.remove(CommonConstants.SECTOR);

		log.debug("savePet requestData : " + dataPayload);
		Long time = System.currentTimeMillis();
		String applicationDetailsId = UUID.randomUUID().toString();
		requestData.getDataPayload();
		NOCApplicationDetail nocappdetails = new NOCApplicationDetail();
		nocappdetails.setApplicationDetailUuid(applicationDetailsId);
		nocappdetails.setApplicationUuid(applicationId);
		nocappdetails.setApplicationDetail(dataPayload.toJSONString());
		nocappdetails.setIsActive(true);
		nocappdetails.setCreatedBy(requestInfo.getUserInfo().getUuid());
		nocappdetails.setCreatedTime(time);
		nocappdetails.setLastModifiedBy(requestInfo.getUserInfo().getUuid());
		nocappdetails.setLastModifiedTime(time);
		List<NOCApplicationDetail> applist = Arrays.asList(nocappdetails);
		NOCDetailsRequestData data = new NOCDetailsRequestData();
		data.setRequestInfo(requestInfo);
		data.setNocApplicationDetails(applist);

		producer.push(saveNOCDetailsTopic, data);
	}

	public ReponseData updateApplicationStatus(RequestData requestData) throws ParseException {
		log.info("Started approveRejectNocTable() : " + requestData);
		RequestInfo requestInfo = requestData.getRequestInfo();
		String applicationId = null;
		ReponseData reponseData = new ReponseData();
		ResponseInfo responseInfo = null;
		try {

			responseInfo = workflowIntegration(requestData.getApplicationId(), requestData,
					requestData.getApplicationStatus());

			if (responseInfo != null && responseInfo.getStatus().equals(CommonConstants.SUCCESSFUL)) {

				JSONObject dataPayLoad = requestData.getDataPayload();
				String roleCode = "";
				Role role = requestData.getRequestInfo().getUserInfo().getRoles().get(0);

				if (role != null) {
					roleCode = role.getCode();
				}

				String appId = getAppIdUuid(requestData.getApplicationId());

				Long time = System.currentTimeMillis();
				applicationId = UUID.randomUUID().toString();
				NOCApplicationRemark app = new NOCApplicationRemark();
				app.setRemarkId(applicationId);
				app.setApplicationUuid(appId);
				app.setApplicationStatus(requestData.getApplicationStatus());
				app.setRemark((dataPayLoad.get(CommonConstants.REMARKS) == null ? ""
						: dataPayLoad.get(CommonConstants.REMARKS).toString()));
				app.setRemarkBy(roleCode);
				app.setIsActive(true);
				app.setCreatedBy(requestInfo.getUserInfo().getUuid());
				app.setCreatedTime(time);
				app.setLastModifiedBy(requestInfo.getUserInfo().getUuid());
				app.setLastModifiedTime(time);

				app.setDocumentId((dataPayLoad.get(CommonConstants.DOCUMENTDETAIL) == null ? ""
						: dataPayLoad.get(CommonConstants.DOCUMENTDETAIL).toString()));

				List<NOCApplicationRemark> applist = Arrays.asList(app);

				NOCRemarksRequestData data = new NOCRemarksRequestData();
				data.setRequestInfo(requestInfo);
				data.setNocApplicationRamarks(applist);

				Integer isAvail = findRemarks(appId);
				if (isAvail != null && isAvail > 0) {
					// Call Update first
					producer.push(updateNOCApproveRejectTopic, data);
				}

				// then Save new entry
				producer.push(saveNOCApproveRejectTopic, data);

				// then Update the main table application status
				NOCApplication apps = new NOCApplication();
				apps.setTenantId(requestData.getTenantId());
				apps.setApplicationUuid(appId);
				apps.setApplicationStatus(requestData.getApplicationStatus());
				apps.setLastModifiedBy(requestInfo.getUserInfo().getUuid());
				apps.setLastModifiedTime(time);
				apps.setAmount((dataPayLoad.get(CommonConstants.AMOUNT) != null
						? Integer.parseInt(dataPayLoad.get(CommonConstants.AMOUNT).toString())
						: 0));

				List<NOCApplication> applists = Arrays.asList(apps);
				NOCRequestData dataApp = new NOCRequestData();
				dataApp.setRequestInfo(requestInfo);
				dataApp.setNocApplication(applists);
				producer.push(updateStatusNOCTopic, dataApp);

				responseInfo.setStatus(CommonConstants.SUCCESS);
				requestData.getDataPayload().put(CommonConstants.APPLICATIONID, applicationId);
				reponseData.setDataPayload(requestData.getDataPayload());
				reponseData.setResponseInfo(responseInfo);
				return reponseData;

			} else {
				if (responseInfo == null) {
					responseInfo = new ResponseInfo();
					responseInfo.setMsgId(CommonConstants.UNABLETOPROCESSREQUEST);
					responseInfo.setStatus(CommonConstants.FAIL);
				}
				reponseData.setResponseInfo(responseInfo);
				return reponseData;
			}
		} catch (Exception e) {

			if (responseInfo == null) {
				responseInfo = new ResponseInfo();
				responseInfo.setMsgId(e.getMessage());
				responseInfo.setStatus(CommonConstants.FAIL);
			}
			reponseData.setResponseInfo(responseInfo);
			return reponseData;
		}
	}

	private String getAppIdUuid(String applicationId) {
		String appId = "";
		JSONArray jsonArray = jdbcTemplate.query(QueryBuilder.SELECT_APPID_QUERY, new Object[] { applicationId },
				columnsNocRowMapper);
		if (!jsonArray.isEmpty()) {
			JSONObject obj = (JSONObject) jsonArray.get(0);
			appId = obj.get("application_uuid").toString();
		}
		return appId;
	}

	public Integer findRemarks(String appId) {
		return jdbcTemplate.query(QueryBuilder.SELECT_REMARKS_QUERY, new Object[] { appId }, counterRowMapper);
	}

	public String calculations(RequestData requestData) throws ParseException {
		String results = "";
		if (requestData.getApplicationType().equals("ADVERTISEMENTNOC")) {
			results = calculationAdvertisementNoc(requestData);
		} else if (requestData.getApplicationType().equals("ROADCUTNOC")) {
			results = calculationRoadCutNoc(requestData);
		} else if (requestData.getApplicationType().equals("PETNOC")) {
			results = calculationPetNoc(requestData);
		} else if (requestData.getApplicationType().equals("SELLMEATNOC")) {
			results = calculationSellMeatNoc(requestData);
		}
		return results;
	}

	private String calculationPetNoc(RequestData requestData) {
		// Need to MDM service for amount
		return "200";
	}

	private String calculationSellMeatNoc(RequestData requestData) {
		// Need to MDM service for amount
		return null;
	}

	private String calculationRoadCutNoc(RequestData requestData) {
		// Need to MDM service for amount
		return null;
	}

	public String calculationAdvertisementNoc(RequestData requestData) throws ParseException {
		JSONArray jsonMdmCatagory = mdmService.getMasterCatagory(requestData.getTenantId(),
				requestData.getRequestInfo());
		JSONArray jsonMdmSubCatagory = mdmService.getMasterSubCatagory(requestData.getTenantId(),
				requestData.getRequestInfo());
		String finalResults = "";
		String calculationType = "";
		SortedMap<Integer, JSONObject> calData = new TreeMap<>();

		if (jsonMdmCatagory != null && !jsonMdmCatagory.isEmpty()
				&& (jsonMdmSubCatagory != null && !jsonMdmSubCatagory.isEmpty())) {
			try {

				JSONObject dataPayload = requestData.getDataPayload();
				String catagoryId = (dataPayload.get("categotyId") == null ? ""
						: dataPayload.get("categotyId").toString());
				String subCatagoryId = (dataPayload.get("subCategotyId") == null ? ""
						: dataPayload.get("subCategotyId").toString());
				String squareFeet = (dataPayload.get("squareFeet") == null ? ""
						: dataPayload.get("squareFeet").toString());
				String calculateByPer = (dataPayload.get("calculateByPer") == null ? ""
						: dataPayload.get("calculateByPer").toString());
				String dateBefore = (dataPayload.get("fromDate") == null ? "" : dataPayload.get("fromDate").toString());
				String dateAfter = (dataPayload.get("toDate") == null ? "" : dataPayload.get("toDate").toString());

				
				// Need Implements -  fromdate and todate from db or json
				
				// Parsing the date
				LocalDate fromDate = LocalDate.parse(dateBefore);
				LocalDate toDate = LocalDate.parse(dateAfter);

				Double sqFeets = Double.parseDouble(squareFeet);

				for (int i = 0; i < jsonMdmCatagory.size(); i++) {
					JSONObject jsonCal = (JSONObject) jsonMdmCatagory.get(i);
					if (jsonCal.get("id") != null && jsonCal.get("id").equals(catagoryId)) {
						calculationType = jsonCal.get("calculationType").toString();
						break;
					}
				}

				for (int i = 0; i < jsonMdmSubCatagory.size(); i++) {
					JSONObject jsonCal = (JSONObject) jsonMdmSubCatagory.get(i);
					if (jsonCal.get("typeOfAdvertisementId") != null
							&& jsonCal.get("typeOfAdvertisementId").equals(catagoryId)) {
						calData.put(Integer.parseInt(jsonCal.get("calculationSequense").toString()), jsonCal);
					}
				}

				if (dataPayload == null || catagoryId == null || squareFeet == null || calculateByPer == null)
					return "Invalid/null at dataPayload [catagoryId or squareFeet or calculateByPer]";

				if ((calculationType != null && calculationType.isEmpty()))
					return "Invalid/null at [Request Data]";

				if ((calData != null && calData.isEmpty()) || sqFeets <= 0.0 || sqFeets <= 0
						|| calculateByPer.isEmpty())
					return "Invalid/null at [applicationType or categoryId or squareFeet or calculateByPer]";

				Double results = 0.0;

				long duration = 0;
				if (calculateByPer.equalsIgnoreCase("day")) {
					calculateByPer = "perDayPrice";
					duration = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
				} else if (calculateByPer.equalsIgnoreCase("week")) {
					calculateByPer = "weekDayPrice";
					duration = ChronoUnit.WEEKS.between(fromDate, toDate) + 1;
				} else if (calculateByPer.equalsIgnoreCase("month")) {
					calculateByPer = "monthDayPrice";
					duration = ChronoUnit.MONTHS.between(fromDate, toDate) + 1;
				} else if (calculateByPer.equalsIgnoreCase("annual")) {
					calculateByPer = "annualPrice";
					duration = ChronoUnit.YEARS.between(fromDate, toDate) + 1;
				} else {
					return "Invalid/null at dataPayload [calculateByPer]";
				}

				int size = calData.size();
				if (calculationType.equalsIgnoreCase("range")) {

					for (int i = 1; i <= size; i++) {
						if (i != size) {

							JSONObject jsonObject1 = calData.get(i);
							Integer min = Integer.parseInt(jsonObject1.get("min").toString());
							Integer max = Integer.parseInt(jsonObject1.get("max").toString());

							Integer rate = Integer.parseInt(
									((jsonObject1.get(calculateByPer) == null || jsonObject1.get(calculateByPer) != null
											&& jsonObject1.get(calculateByPer).toString().isEmpty()) ? "0"
													: jsonObject1.get(calculateByPer).toString()));
							max = max - min;
							sqFeets = sqFeets - max;
							results += (rate * duration);

							if (sqFeets <= 0)
								break;

						} else {
							JSONObject jsonObject1 = calData.get(i);
							Integer min = Integer.parseInt(jsonObject1.get("min").toString());
							Integer max = Integer.parseInt(jsonObject1.get("max").toString());

							Integer rate = Integer.parseInt(
									((jsonObject1.get(calculateByPer) == null || jsonObject1.get(calculateByPer) != null
											&& jsonObject1.get(calculateByPer).toString().isEmpty()) ? "0"
													: jsonObject1.get(calculateByPer).toString()));
							max = max - min;
							sqFeets = sqFeets - max;
							results += (rate * duration);
							while (sqFeets > 0) {
								sqFeets = sqFeets - max;
								results += (rate * duration);
							}
						}
					}
				} else if (calculationType.equalsIgnoreCase("units")) {
					for (int i = 1; i <= size; i++) {
						JSONObject jsonObject1 = calData.get(i);

						if (jsonObject1.get("id").equals(subCatagoryId)) {
							Integer rate = Integer.parseInt(
									((jsonObject1.get(calculateByPer) == null || jsonObject1.get(calculateByPer) != null
											&& jsonObject1.get(calculateByPer).toString().isEmpty()) ? "0"
													: jsonObject1.get(calculateByPer).toString()));
							results += (rate * duration);
							break;
						}
					}
				} else if (calculationType.equalsIgnoreCase("days")) {
					for (int i = 1; i <= size; i++) {
						if (i != size) {

							JSONObject jsonObject1 = calData.get(i);
							Integer min = Integer.parseInt(jsonObject1.get("min").toString());
							Integer max = Integer.parseInt(jsonObject1.get("max").toString());

							Integer rate = Integer.parseInt(
									((jsonObject1.get(calculateByPer) == null || jsonObject1.get(calculateByPer) != null
											&& jsonObject1.get(calculateByPer).toString().isEmpty()) ? "0"
													: jsonObject1.get(calculateByPer).toString()));
							max = max - min;
							results += rate * sqFeets * max;
							duration = duration - max;

							if (duration <= 0)
								break;

						} else {
							JSONObject jsonObject1 = calData.get(i);
							Integer min = Integer.parseInt(jsonObject1.get("min").toString());
							Integer max = Integer.parseInt(jsonObject1.get("max").toString());

							Integer rate = Integer.parseInt(
									((jsonObject1.get(calculateByPer) == null || jsonObject1.get(calculateByPer) != null
											&& jsonObject1.get(calculateByPer).toString().isEmpty()) ? "0"
													: jsonObject1.get(calculateByPer).toString()));

							max = max - min;
							results += rate * sqFeets * max;
							duration = duration - max;

							while (duration > 0) {
								results += rate * sqFeets * max;
								duration = duration - max;
							}
						}
					}
				}
				finalResults = results.toString();
			} catch (Exception e) {
				return "Exception : " + e.getMessage();
			}
		}
		return finalResults;
	}
}
