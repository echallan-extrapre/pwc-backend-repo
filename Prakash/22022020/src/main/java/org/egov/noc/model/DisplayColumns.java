package org.egov.noc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class DisplayColumns {

	private String tenantId;
	private String columns;
	private String roles;
	private String applicationType;

	public int hashCode() {
		int hashcode = 99;
		hashcode += tenantId.hashCode() + roles.hashCode() + applicationType.hashCode();
		return hashcode;
	}

	public boolean equals(Object obj) {
		if (obj instanceof DisplayColumns) {
			DisplayColumns pp = (DisplayColumns) obj;
			return (pp.tenantId.equals(this.tenantId) && pp.columns == this.columns && pp.roles == this.roles
					&& pp.applicationType == this.applicationType);
		} else {
			return false;
		}
	}

}
