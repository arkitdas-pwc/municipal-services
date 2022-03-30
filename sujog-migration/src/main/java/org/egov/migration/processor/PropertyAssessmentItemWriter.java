package org.egov.migration.processor;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.egov.migration.business.model.PropertyDTO;
import org.egov.migration.business.model.PropertyDetailDTO;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.service.PropertyService;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertyAssessmentItemWriter  implements ItemWriter<PropertyDetailDTO> {
	
	@Autowired
	private PropertyService propertyService;
	
	@Autowired
	private RecordStatistic recordStatistic;

	@Override
	public void write(List<? extends PropertyDetailDTO> items) {
		
		items.forEach(propertyDetail -> {
			try {
				PropertyDTO propertyDTO = propertyService.searchProperty(propertyDetail);
				if(!Objects.isNull(propertyDTO)) {
					propertyDetail.setProperty(propertyDTO);
				}
			} catch (Exception e) {
				log.error(String.format("PropertyId: %s, error message: %s", propertyDetail.getProperty().getOldPropertyId(), e.getMessage()));
				MigrationUtility.addError(propertyDetail.getProperty().getOldPropertyId(),String.format("Property Migration error: %s",  e.getMessage()));
			}
		});
		
		items.forEach(propertyDetail -> {
			boolean isAssessmentMigrated = false;
			try {
				isAssessmentMigrated = propertyService.migrateAssessmentV1(propertyDetail);
			} catch (Exception e) {
				log.error(String.format("Assessment migration error for PropertyId: %s, error message: %s", propertyDetail.getProperty().getOldPropertyId(), e.getMessage()));
				MigrationUtility.addError(propertyDetail.getProperty().getOldPropertyId(), String.format("Assessment migration error: %s", e.getMessage()));
			}
			
			if(!isAssessmentMigrated && propertyDetail.getProperty().getPropertyId() != null) {
				MigrationUtility.addError(propertyDetail.getProperty().getOldPropertyId(), "Assessment not migrated");
			} else if(isAssessmentMigrated) {
				MigrationUtility.addSuccessForAssessment(propertyDetail.getProperty(), propertyDetail.getAssessment());
			}
		});
		
		try {
			generateReport();
		} catch (InvalidFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void generateReport() throws IOException, InvalidFormatException {
		log.info("Generating Reports");
		propertyService.writeError();
		recordStatistic.getErrorRecords().clear();
		
		propertyService.writeSuccess();
		recordStatistic.getSuccessRecords().clear();
		log.info("Reports updated");
	}

}
