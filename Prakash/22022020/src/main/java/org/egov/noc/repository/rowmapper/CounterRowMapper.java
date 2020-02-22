package org.egov.noc.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CounterRowMapper implements ResultSetExtractor<Integer> {

	@Override
	public Integer extractData(ResultSet rs) throws SQLException {
		int counter = 0;
		while (rs.next()) {
			counter++;
		}
		return counter;
	}

}
