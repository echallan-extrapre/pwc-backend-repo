package org.egov.noc.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class Column {
	@JsonProperty("applicationType")
	private String applicationType;

	@JsonProperty("roles")
	public List<String> roles;

	@JsonProperty("colunmNames")
	private String colunmNames;
}
