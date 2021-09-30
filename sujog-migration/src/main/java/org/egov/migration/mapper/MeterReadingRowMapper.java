package org.egov.migration.mapper;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.egov.migration.reader.model.WnsMeterReading;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.stereotype.Component;

@Component
public class MeterReadingRowMapper {

	public WnsMeterReading mapRow(String ulb, Row row, Map<String, Integer> columnMap) {
		
		WnsMeterReading meterReading = new WnsMeterReading();
		
		meterReading.setUlb(ulb);
		meterReading.setConnectionNo(columnMap.get(MigrationConst.COL_CONNECTION_NO)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_NO)), false));
		meterReading.setConnectionFacility(columnMap.get(MigrationConst.COL_CONNECTION_FACILTY)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_FACILTY)), false));
		meterReading.setBillingPeriod(columnMap.get(MigrationConst.COL_BILLING_PERIOD)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_BILLING_PERIOD)), false));
		meterReading.setMeterStatus(columnMap.get(MigrationConst.COL_METER_STATUS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_METER_STATUS)), false));
		meterReading.setPreviousReading(columnMap.get(MigrationConst.COL_PREVIOUS_READING)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PREVIOUS_READING)), false));
		meterReading.setPreviousReadingDate(columnMap.get(MigrationConst.COL_PREVIOUS_READING_DATE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PREVIOUS_READING_DATE)), false));
		meterReading.setCurrentReading(columnMap.get(MigrationConst.COL_CURRENT_READING)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CURRENT_READING)), false));
		meterReading.setCurrentReadingDate(columnMap.get(MigrationConst.COL_CURRENT_READING_DATE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CURRENT_READING_DATE)), false));
		meterReading.setCreatedDate(columnMap.get(MigrationConst.COL_CREATED_DATE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CREATED_DATE)), false));
		meterReading.setMeterMake(columnMap.get(MigrationConst.COL_METER_MAKE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_METER_MAKE)), false));
		meterReading.setMeterReadingRatio(columnMap.get(MigrationConst.COL_METER_READING_RATIO)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_METER_READING_RATIO)), false));

		return meterReading;
	}
}
