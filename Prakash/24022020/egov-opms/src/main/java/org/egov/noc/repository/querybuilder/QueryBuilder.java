package org.egov.noc.repository.querybuilder;

import org.egov.noc.config.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QueryBuilder {

	@Autowired
	private ApplicationProperties applicationProperties;

	public static final String SELECT_PETS_QUERY = "select ED.* from egpm_noc_application_detail ED inner join egpm_noc_application EA on ED.application_uuid=EA.application_uuid WHERE EA.application_type=? AND EA.application_status=? AND ED.is_active=TRUE AND EA.is_active=TRUE";
	public static final String SELECT_APPLICATION_QUERY = "select * from egpm_noc_application_detail ED inner join egpm_noc_application EA on ED.application_uuid=EA.application_uuid WHERE  ED.application_uuid=? and ED.is_active=true and EA.is_active=TRUE and EA.application_status=?";

	public static final String SELECT_REMARKS_QUERY = "select * from egpm_noc_application_remark WHERE application_uuid=? AND is_active=true";
	public static final String SELECT_APPID_QUERY = "select ED.application_uuid from egpm_noc_application ED WHERE ED.noc_number=?";

	public static final String ALL_REMARKS_QUERY = "select application_uuid applicationUuid,remark_by RoleCode,remark,document_detail documentDetail,application_status applicationStatus, created_time createdTime from egpm_noc_application_remark WHERE application_uuid=?";
	public static final String SELECT_VIEW_QUERY = "select EA.application_uuid applicationUuid, EA.noc_number nocNumber,EA.application_type applicationType,EA.applicant_name applicantName,ED.application_detail applicationDetail,EA.house_number houseNumber,EA.sector sector,EA.applied_date appliedDate,EA.application_status applicationStatus from egpm_noc_application_detail ED inner join egpm_noc_application EA on ED.application_uuid=EA.application_uuid WHERE EA.noc_number=? AND EA.is_active=TRUE AND ED.is_active=TRUE";

	public static final String SELECT_APPLICATION_ID_QUERY = "select * from egpm_noc_application where noc_number=? AND is_active=TRUE";
	public static final String SELECT_APPLICATION_DETAIL_QUERY = "select * from egpm_noc_application_detail where application_uuid=(select application_uuid from egpm_noc_application where noc_number=? and is_active='true') and is_active='true'";

	// add
	public static final String INSERT_NOC_DETAILS_QUERY = "INSERT into public.egpm_noc_application_detail "
			+ "(application_detail_uuid, application_uuid, application_detail, is_active, created_by, created_time, last_modified_by, last_modified_time)"
			+ "values(?,?,to_json(?::json),?,?,?,?,?)";

	public static final String GET_NOC_REMARKS_QUERY = "select EA.noc_number \"applicationId\", ED.application_status \"applicationStatus\", ED.remark, ED.remark_by \"remarkBy\"\r\n"
			+ "from egpm_noc_application_remark ED INNER JOIN egpm_noc_application EA\r\n"
			+ "ON ED.application_uuid=EA.application_uuid \r\n" + "WHERE EA.tenant_id=? and \r\n" + "EA.noc_number=?";

	public static String getPetsQuery() {

		StringBuilder petsQuery = new StringBuilder(SELECT_PETS_QUERY);
		return petsQuery.toString();
	}

	public static String getApplicationQuery() {
		StringBuilder petsQuery = new StringBuilder(SELECT_APPLICATION_QUERY);
		return petsQuery.toString();
	}

}
