package org.egov.swService.workflow;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.egov.swService.config.SWConfiguration;
import org.egov.swService.model.Connection.ApplicationStatusEnum;
import org.egov.swService.model.SewerageConnection;
import org.egov.swService.model.SewerageConnectionRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

@Service
@Slf4j
public class WorkflowIntegrator {

	private static final String TENANTIDKEY = "tenantId";

	private static final String BUSINESSSERVICEKEY = "businessService";

	private static final String ACTIONKEY = "action";

	private static final String COMMENTKEY = "comment";

	private static final String MODULENAMEKEY = "moduleName";

	private static final String BUSINESSIDKEY = "businessId";

	private static final String DOCUMENTSKEY = "documents";

	private static final String ASSIGNEEKEY = "assignes";

	private static final String UUIDKEY = "uuid";

	private static final String MODULENAMEVALUE = "SW";

	private static final String WORKFLOWREQUESTARRAYKEY = "ProcessInstances";

	private static final String REQUESTINFOKEY = "RequestInfo";

	private static final String PROCESSINSTANCESJOSNKEY = "$.ProcessInstances";

	private static final String BUSINESSIDJOSNKEY = "$.businessId";

	private static final String STATUSJSONKEY = "$.state.applicationStatus";

	private RestTemplate rest;

	private SWConfiguration config;

	@Autowired
	public WorkflowIntegrator(RestTemplate rest, SWConfiguration config) {
		this.rest = rest;
		this.config = config;
	}

	/**
	 * Method to integrate with workflow
	 *
	 * takes the sewerage request as parameter constructs the work-flow request
	 *
	 * and sets the resultant status from wf-response back to sewerage object
	 *
	 * @param sewerageRequest
	 */
	public void callWorkFlow(SewerageConnectionRequest sewerageConnectionRequest) {

		String wfTenantId = sewerageConnectionRequest.getSewerageConnection().getProperty().getTenantId();

		JSONArray array = new JSONArray();
		SewerageConnection connection = sewerageConnectionRequest.getSewerageConnection();
		JSONObject obj = new JSONObject();
		List<Map<String, String>> uuidmaps = new LinkedList<>();
		// Add assignes to processInsatance

		obj.put(BUSINESSIDKEY, connection.getApplicationNo());
		obj.put(TENANTIDKEY, wfTenantId);
		obj.put(BUSINESSSERVICEKEY, config.getBusinessServiceValue());
		obj.put(MODULENAMEKEY, MODULENAMEVALUE);
		obj.put(ACTIONKEY, connection.getAction());
		// Add comment
		// obj.put(COMMENTKEY, connection.getComment);
		if (!CollectionUtils.isEmpty(uuidmaps))
			obj.put(ASSIGNEEKEY, uuidmaps);
		obj.put(DOCUMENTSKEY, connection.getDocuments());
		array.add(obj);
		JSONObject workFlowRequest = new JSONObject();
		workFlowRequest.put(REQUESTINFOKEY, sewerageConnectionRequest.getRequestInfo());
		workFlowRequest.put(WORKFLOWREQUESTARRAYKEY, array);
		String response = null;
		try {
			response = rest.postForObject(config.getWfHost().concat(config.getWfTransitionPath()), workFlowRequest,
					String.class);
		} catch (HttpClientErrorException e) {

			/*
			 * extracting message from client error exception
			 */
			DocumentContext responseContext = JsonPath.parse(e.getResponseBodyAsString());
			List<Object> errros = null;
			try {
				errros = responseContext.read("$.Errors");
			} catch (PathNotFoundException pnfe) {
				log.error("EG_WS_WF_ERROR_KEY_NOT_FOUND",
						" Unable to read the json path in error object : " + pnfe.getMessage());
				throw new CustomException("EG_WS_WF_ERROR_KEY_NOT_FOUND",
						" Unable to read the json path in error object : " + pnfe.getMessage());
			}
			throw new CustomException("EG_WF_ERROR", errros.toString());
		} catch (Exception e) {
			throw new CustomException("EG_WF_ERROR",
					" Exception occured while integrating with workflow : " + e.getMessage());
		}

		/*
		 * on success result from work-flow read the data and set the status
		 * back to WS object
		 */
		DocumentContext responseContext = JsonPath.parse(response);
		List<Map<String, Object>> responseArray = responseContext.read(PROCESSINSTANCESJOSNKEY);
		Map<String, String> idStatusMap = new HashMap<>();
		responseArray.forEach(object -> {

			DocumentContext instanceContext = JsonPath.parse(object);
			idStatusMap.put(instanceContext.read(BUSINESSIDJOSNKEY), instanceContext.read(STATUSJSONKEY));
		});
		// setting the status back to SW object from wf response
		sewerageConnectionRequest.getSewerageConnection().setApplicationStatus(ApplicationStatusEnum
				.fromValue(idStatusMap.get(sewerageConnectionRequest.getSewerageConnection().getApplicationNo())));

	}
}