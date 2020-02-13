package org.egov.noc.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.egov.noc.web.contract.ReponseData;
import org.egov.noc.wf.model.ProcessInstance;
import org.egov.noc.wf.model.ProcessInstanceRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
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
	private CounterRowMapper counterRowMapper;

	@Autowired
	private ColumnsNocRowMapper columnsNocRowMapper;

	@Autowired
	private IDGenUtil idgen;

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
		app.setApplicantName(dataPayLoad.get("applicantName").toString());
		app.setHouseNo(dataPayLoad.get("houseNo").toString());
		app.setSector(dataPayLoad.get("sector").toString());
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
		dataPayload.remove("applicantName");
		dataPayload.remove("houseNo");
		dataPayload.remove("sector");
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
			String str = "[:" + string + ":]";
			queryString = queryString.replace(str, "'" + jsonObject.get(string).toString() + "'");
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
						jsonObject.put("remarks", jsonArrayResult);
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

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("application_uuid", applicationuuid);

		return jdbcTemplate.query(queryStr, new Object[] { applicationuuid, status }, nocRowMapper);

	}

	public String saveValidateStatus(RequestData requestData, String status) {
		String NOCID = null;
		if (status.equals("INITIATE")
				&& (requestData.getApplicationId() == null || requestData.getApplicationId().isEmpty())) {
			// Save as draft
			NOCID = saveNocTableJson(requestData, status);
			if (NOCID != null) {
				ResponseInfo info = workflowIntegration(NOCID, requestData, status);
			}
		} else if (status.equals("APPLY") && requestData.getApplicationId() != null
				&& !requestData.getApplicationId().isEmpty()) {
			// update Initiated record
			NOCID = requestData.getApplicationId();
			requestData.setApplicationStatus("APPLIED");
			updateNOC(requestData, NOCID);
			// ResponseInfo info =
			workflowIntegration(NOCID, requestData, "APPLY");
			workflowIntegrationForPayment(NOCID, requestData, "PAY");
			/*
			 * if (info != null && info.getStatus().equals("successful")) {
			 * workflowIntegrationForPayment(NOCID, requestData, "PAY"); }
			 */
		} else if (status.equals("APPLY")
				&& (requestData.getApplicationId() == null || requestData.getApplicationId().isEmpty())) {
			// Save as Fresh Submitted records
			NOCID = saveNocTableJson(requestData, status);
			if (NOCID != null) {
				// ResponseInfo info =
				workflowIntegration(NOCID, requestData, "INITIATE");
				workflowIntegration(NOCID, requestData, "APPLY");
				workflowIntegrationForPayment(NOCID, requestData, "PAY");
				/*
				 * if (info != null && info.getStatus().equals("successful")) { info =
				 * workflowIntegration(NOCID, requestData, "APPLY"); if (info != null &&
				 * info.getStatus().equals("successful")) { workflowIntegrationForPayment(NOCID,
				 * requestData, "PAY"); } }
				 */
			}
		} else {
			NOCID = "Invalid Request";
		}
		return NOCID;
	}

	// add
	private String saveNocTableJson(RequestData requestData, String status) {

		RequestInfo requestInfo = requestData.getRequestInfo();

		String NOCID = idgen.generateApplicationId(requestData.getTenantId());
		int isApplicationIdExists = validateApplicationId(requestData, NOCID);
		if (isApplicationIdExists == 0) {
			String applicationId = null;
			if (NOCID != null) {
				JSONObject dataPayLoad = requestData.getDataPayload();
				Long time = System.currentTimeMillis();
				applicationId = UUID.randomUUID().toString();
				NOCApplication app = new NOCApplication();
				app.setApplicationUuid(applicationId);
				app.setTenantId(requestData.getTenantId());
				app.setNocNumber(NOCID);
				app.setApplicantName(dataPayLoad.get("applicantName").toString());
				app.setHouseNo(dataPayLoad.get("houseNo").toString());
				app.setSector(dataPayLoad.get("sector").toString());
				app.setAppliedDate(new Date().toLocaleString());
				app.setApplicationType(requestData.getApplicationType());
				if (status.equals("INITIATE")) {
					app.setApplicationStatus("INITIATE");
				} else {
					app.setApplicationStatus("APPLIED");
				}
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

				return NOCID;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public int validateApplicationId(RequestData requestData, String applicationId) {
		List<Object> preparedStatementValues = new ArrayList<>();
		String queryStr = QueryBuilder.getApplicationQuery();
		log.debug("query:::" + queryStr + " preparedStatementValues::" + preparedStatementValues);

		Map<String, Object> params = new HashMap<>();
		params.put("application_uuid", applicationId);

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
		dataPayload.remove("applicantName");
		dataPayload.remove("houseNo");
		dataPayload.remove("sector");

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

		/*
		 * jdbcTemplate.batchUpdate(QueryBuilder.INSERT_NOC_DETAILS_QUERY, new
		 * BatchPreparedStatementSetter() {
		 * 
		 * @Override public void setValues(PreparedStatement ps, int index) throws
		 * SQLException {
		 * 
		 * ps.setString(1, applicationDetailsId); ps.setString(2, applicationId);
		 * ps.setString(3, requestData.getDataPayload().toJSONString());
		 * ps.setBoolean(4, true); ps.setString(5, requestInfo.getUserInfo().getUuid());
		 * ps.setLong(6, time); ps.setString(7, requestInfo.getUserInfo().getUuid());
		 * ps.setLong(8, time); }
		 * 
		 * @Override public int getBatchSize() { return 1; } });
		 */
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

			if (responseInfo != null && responseInfo.getStatus().equals("successful")) {

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
				app.setRemark(dataPayLoad.get("remarks").toString());
				app.setRemarkBy(roleCode);
				app.setIsActive(true);
				app.setCreatedBy(requestInfo.getUserInfo().getUuid());
				app.setCreatedTime(time);
				app.setLastModifiedBy(requestInfo.getUserInfo().getUuid());
				app.setLastModifiedTime(time);
				JSONObject str = new JSONObject();
				str.put("fileStoreId", "abcs1");
				app.setDocumentId(str.toJSONString());

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
				apps.setAmount(
						(dataPayLoad.get("amount") != null ? Integer.parseInt(dataPayLoad.get("amount").toString())
								: 0));

				List<NOCApplication> applists = Arrays.asList(apps);
				NOCRequestData dataApp = new NOCRequestData();
				dataApp.setRequestInfo(requestInfo);
				dataApp.setNocApplication(applists);
				producer.push(updateStatusNOCTopic, dataApp);

				responseInfo.setStatus("success");
				requestData.getDataPayload().put("applicationId", applicationId);
				reponseData.setDataPayload(requestData.getDataPayload());
				reponseData.setResponseInfo(responseInfo);
				return reponseData;

			} else {
				if (responseInfo == null) {
					responseInfo = new ResponseInfo();
					responseInfo.setMsgId("Unable to process the request");
					responseInfo.setStatus("Fail");
				}
				reponseData.setResponseInfo(responseInfo);
				return reponseData;
			}
		} catch (Exception e) {

			if (responseInfo == null) {
				responseInfo = new ResponseInfo();
				responseInfo.setMsgId(e.getMessage());
				responseInfo.setStatus("Fail");
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
}
