package org.egov.noc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Collection of audit related fields used by most models
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NOCApplicationRemark {

	private String remarkId;
	private String applicationUuid;
	private String applicationStatus;
	private String remark;
	private String remarkBy;
	private Boolean isActive;
	private String createdBy;
	private Long createdTime;
	private String lastModifiedBy;
	private Long lastModifiedTime;
	private String documentId;
}
