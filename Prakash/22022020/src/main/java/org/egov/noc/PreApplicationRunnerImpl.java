package org.egov.noc;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Vector;

import org.egov.noc.model.Column;
import org.egov.noc.model.Columns;
import org.egov.noc.model.DisplayColumns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Component
@Order(1)
public class PreApplicationRunnerImpl implements ApplicationRunner {

	@Autowired
	public static ResourceLoader resourceLoader;

	@Value("${egov.disp-columns.json.path}")
	private String jsonPath;

	public static final Logger logger = LoggerFactory.getLogger(PreApplicationRunnerImpl.class);

	public static Vector<DisplayColumns> displayColumns = new Vector<>();

	@Override
	public void run(final ApplicationArguments arg0) throws Exception {
		try {
			logger.info("Reading JSON for display Column files......");
			readFiles();
		} catch (Exception e) {
			logger.error("Exception while loading JSON for display Column files: ", e);
		}
	}

	public PreApplicationRunnerImpl(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public void readFiles() {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

		Columns columns = null;
		DisplayColumns displayCol = null;
		try {

			if (jsonPath.startsWith("https://") || jsonPath.startsWith("http://")) {
				logger.info("Reading....: " + jsonPath);
				URL josnFile = new URL(jsonPath);

				try {
					columns = mapper.readValue(new InputStreamReader(josnFile.openStream()), Columns.class);

					if (columns != null) {
						for (Column col : (columns.getColumnMaps().getColumnConfig())) {
							String appType = col.getApplicationType();
							String cols = col.getColunmNames().toString();
							for (String tenant : columns.getColumnMaps().getTenantId()) {
								for (String role : col.getRoles()) {
									displayCol = new DisplayColumns();
									displayCol.setTenantId(tenant);
									displayCol.setApplicationType(appType);
									displayCol.setRoles(role);
									displayCol.setColumns(cols);
									displayColumns.add(displayCol);
								}
							}
						}
					}

				} catch (Exception e) {
					logger.error("Exception while fetching service map for: " + jsonPath + " = ", e);
				}
				logger.info("Parsed: " + jsonPath);

			} else if (jsonPath.startsWith("file://")) {
				logger.info("Reading....: " + jsonPath);
				Resource resource = resourceLoader.getResource(jsonPath);
				File file = resource.getFile();
				try {
					columns = mapper.readValue(file, Columns.class);
					if (columns != null) {
						for (Column col : (columns.getColumnMaps().getColumnConfig())) {
							String appType = col.getApplicationType();
							String cols = col.getColunmNames().toString();
							for (String tenant : columns.getColumnMaps().getTenantId()) {
								for (String role : col.getRoles()) {
									displayCol = new DisplayColumns();
									displayCol.setTenantId(tenant);
									displayCol.setApplicationType(appType);
									displayCol.setRoles(role);
									displayCol.setColumns(cols);
									displayColumns.add(displayCol);
								}
							}
						}
					}
					System.out.println("displayColumns :" + displayColumns);

				} catch (Exception e) {
					logger.error("Exception while fetching service map for: " + jsonPath);
				}
				logger.info("Parsed to object: " + jsonPath);
			}

		} catch (Exception e) {
			logger.error("Exception while loading yaml files: ", e);
		}
	}

	public static Vector<DisplayColumns> getDisplayColumns() {
		return displayColumns;
	}

	public static String getSqlQuery(String tenantId, String roles, String applicationType) {
		String sqlQuery = "";

		DisplayColumns temp = displayColumns
				.stream().filter(value -> value.getApplicationType().equals(applicationType)
						&& value.getRoles().equals(roles) && value.getTenantId().equals(tenantId))
				.findFirst().orElse(null);
		if (temp != null) {
			sqlQuery = temp.getColumns();
		}
		return sqlQuery;
	}

	public static void setDisplayColumns(Vector<DisplayColumns> displayColumns) {
		PreApplicationRunnerImpl.displayColumns = displayColumns;
	}
}
