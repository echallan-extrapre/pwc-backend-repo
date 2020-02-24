package org.egov.noc.util;

public class CommonConstants {

	/* No args Constructor */
	private CommonConstants() {
	}

	// Status Name
	public static final String APPLY = "APPLY";
	public static final String INITIATE = "INITIATE";
	public static final String DRAFT = "DRAFT";
	public static final String PAY = "PAY";
	public static final String APPLIED = "APPLIED";
	public static final String SYSTEMPAYMENT = "SYSTEM_PAYMENT";

	// Error Messages
	public static final String SAVEERROR = "Unable to Save the record.";
	public static final String GETERROR = "Unable to Get the record.";
	public static final String UPDATEERROR = "Unable to Update the record.";
	public static final String UPDATESTATUSERROR = "Unable to Update Status the record.";

	// Columns Name
	public static final String APPLICANTNAME = "applicantName";
	public static final String APPLICANTUUID = "application_uuid";
	public static final String HOUSENO = "houseNo";
	public static final String SECTOR = "sector";
	public static final String APPLICATIONDETAIL = "application_detail";
	public static final String APPLICATIONDETAILUUID = "application_detail_uuid";
	public static final String APPLICATIONUUID = "application_uuid";
	public static final String CREATEDBY = "created_by";
	public static final String ISACTIVE = "is_active";
	public static final String LASTMODIFIEDBY = "last_modified_by";
	public static final String LASTMODIFIEDTIME = "last_modified_time";
	public static final String REMARKS = "remarks";
	public static final String AMOUNT = "amount";
	public static final String APPLICATIONID = "applicationId";
	public static final String DOCUMENTDETAIL = "documentDetail";
	
	

	public static final String SUCCESS = "success";
	public static final String FAIL = "Fail";
	public static final String SUCCESSFUL = "successful";
	public static final String UNABLETOPROCESSREQUEST = "Unable to Process request : ";
}
