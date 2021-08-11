package org.egov.pt.migration.common.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RecordStatistic {
	
	private Map<String, List<String>> errorRecords = new HashMap<String, List<String>>();
	
	private Map<String, String> successRecords = new HashMap<String, String>();
	
	private String successFile;
	
	private String errorFile;

}
