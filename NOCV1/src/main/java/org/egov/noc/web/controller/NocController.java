package org.egov.noc.web.controller;

import org.egov.common.contract.response.ResponseInfo;
import org.egov.noc.model.Errors;
import org.egov.noc.model.RequestData;
import org.egov.noc.service.NocService;
import org.egov.noc.util.UserUtil;
import org.egov.noc.web.contract.ReponseData;
import org.egov.noc.web.contract.factory.ResponseFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("noc")
public class NocController {

	
	@Autowired
	private NocService nocService;

	@Autowired
	private UserUtil userUtil;

	@Autowired
	private ResponseFactory responseFactory;

	@PostMapping("_get")
	@ResponseBody
	@CrossOrigin
	public ResponseEntity<?> get(@RequestBody RequestData request, BindingResult bindingResult) {

		if (bindingResult.hasErrors()) {
			return new ResponseEntity<>(responseFactory.getErrorResponse(bindingResult, request.getRequestInfo()),
					HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(nocService.searchNoc(request), HttpStatus.OK);
	}

	@PostMapping("_view")
	@ResponseBody
	@CrossOrigin
	public ResponseEntity<?> view(@RequestBody RequestData request, BindingResult bindingResult) {
		ReponseData responseDataResponse = null;

		if (bindingResult.hasErrors()) {
			return new ResponseEntity<>(responseFactory.getErrorResponse(bindingResult, request.getRequestInfo()),
					HttpStatus.BAD_REQUEST);
		}
		if (!request.getApplicationId().isEmpty()) {
			return new ResponseEntity<>(nocService.viewNoc(request), HttpStatus.OK);
		} else {
			ResponseInfo responseInfo = new ResponseInfo();
			responseInfo.setStatus("Fail");
			responseInfo.setMsgId("Required parameters missing [applicationId]");
			responseDataResponse = new ReponseData();
			responseDataResponse.setResponseInfo(responseInfo);
			return new ResponseEntity<>(responseDataResponse, HttpStatus.OK);
		}
	}

	/////// add
	@PostMapping("_createJson")
	@ResponseBody
	@CrossOrigin
	public ResponseEntity<?> createNoc(@RequestBody RequestData requestData, BindingResult bindingResult) {

		// validate user
		Errors response = userUtil.validateUser(requestData);
		
		if (response.getError().getMessage().equals("success")) {
			log.debug("update petsRequest:" + requestData.getDataPayload());

			if (bindingResult.hasErrors()) {
				return new ResponseEntity<>(
						responseFactory.getErrorResponse(bindingResult, requestData.getRequestInfo()),
						HttpStatus.BAD_REQUEST);
			}

			ReponseData responseDataResponse = nocService.sendNocCreateToTableJsonValue(requestData);

			return new ResponseEntity<>(responseDataResponse, HttpStatus.CREATED);
		} else {
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

	}

	// update Noc
	@PostMapping("_update")
	@CrossOrigin
	public ResponseEntity<?> update(@RequestBody RequestData requestData, BindingResult bindingResult) {
		// validate user
		Errors res = null;

		log.debug("update petsRequest:" + requestData.getDataPayload());

		if (bindingResult.hasErrors()) {
			return new ResponseEntity<>(responseFactory.getErrorResponse(bindingResult, requestData.getRequestInfo()),
					HttpStatus.BAD_REQUEST);
		}

		Errors responses = nocService.updateNoc(requestData);

		return new ResponseEntity<>(responses, HttpStatus.CREATED);
	}

	@PostMapping("_updateappstatus")
	@ResponseBody
	@CrossOrigin
	public ResponseEntity<?> updateApplicationStatus(@RequestBody RequestData requestData,
			BindingResult bindingResult) {

		// validate user
		Errors response = userUtil.validateUser(requestData);
		ReponseData responseDataResponse = null;

		if (response.getError().getMessage().equals("success")) {
			log.debug("update status petsRequest:" + requestData.getDataPayload());

			if (bindingResult.hasErrors()) {
				return new ResponseEntity<>(
						responseFactory.getErrorResponse(bindingResult, requestData.getRequestInfo()),
						HttpStatus.BAD_REQUEST);
			}

			if (!requestData.getApplicationStatus().isEmpty() && !requestData.getApplicationType().isEmpty()
					&& !requestData.getApplicationId().isEmpty()) {

				responseDataResponse = nocService.updateNocApplicationStatus(requestData);
				return new ResponseEntity<>(responseDataResponse, HttpStatus.CREATED);
			} else {
				ResponseInfo responseInfo = new ResponseInfo();
				responseInfo.setStatus("Fail");
				responseInfo.setMsgId("Required parameters missing");
				responseDataResponse = new ReponseData();
				responseDataResponse.setResponseInfo(responseInfo);
				return new ResponseEntity<>(responseDataResponse, HttpStatus.OK);
			}
		} else {
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

	}

	@PostMapping("_getcolumnsmodules")
	@ResponseBody
	@CrossOrigin
	public ResponseEntity<?> getcolumnsModules(@RequestBody RequestData requestData, BindingResult bindingResult,
			@RequestParam(value = "noctype") String nocType) {
		log.debug("getcolumns Request:" + nocType);
		log.debug("getcolumns Request:" + requestData);

		JSONArray jsonColumns = null;

		Errors res = null;
		Errors response = userUtil.validateUser(requestData);
		if (response.getError().getMessage().equals("success")) {
			log.debug("Get Remarks :" + requestData.getDataPayload());

			if (bindingResult.hasErrors()) {
				return new ResponseEntity<>(
						responseFactory.getErrorResponse(bindingResult, requestData.getRequestInfo()),
						HttpStatus.BAD_REQUEST);
			}
			if (nocType != null && !nocType.isEmpty()) {
				jsonColumns = nocService.getColumnsForNoc(nocType);
				return new ResponseEntity<>(jsonColumns, HttpStatus.OK);
			} else {
				jsonColumns = new JSONArray();
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("ErrorMessage", "Invalid NOC Type");
				jsonColumns.add(jsonObject);
				return new ResponseEntity<>(jsonColumns, HttpStatus.OK);
			}
		} else {
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	}

	@PostMapping("_getcolumnsremarks")
	@ResponseBody
	@CrossOrigin
	public ResponseEntity<?> getcolumnsRemarks(@RequestBody RequestData requestData, BindingResult bindingResult) {

		log.debug("getcolumns Remarks Request:" + requestData);
		Errors res = null;
		Errors response = userUtil.validateUser(requestData);
		if (response.getError().getMessage().equals("success")) {
			log.debug("Get Remarks :" + requestData.getDataPayload());

			if (bindingResult.hasErrors()) {
				return new ResponseEntity<>(
						responseFactory.getErrorResponse(bindingResult, requestData.getRequestInfo()),
						HttpStatus.BAD_REQUEST);
			}

			final JSONArray jsonColumns = nocService.getColumnsRemarksForNoc(requestData);
			return new ResponseEntity<>(jsonColumns, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	}
}
