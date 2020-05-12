package org.egov.land.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.web.model.user.CreateUserRequest;
import org.egov.bpa.web.model.user.UserDetailResponse;
import org.egov.bpa.web.model.user.UserSearchRequest;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.land.web.models.LandInfo;
import org.egov.land.web.models.LandRequest;
import org.egov.land.web.models.LandSearchCriteria;
import org.egov.land.web.models.OwnerInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LandUserService {

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private ObjectMapper mapper;

	public void manageUser(LandRequest bpaRequest) {
		LandInfo landInfo = bpaRequest.getLandInfo();
		RequestInfo requestInfo = bpaRequest.getRequestInfo();
		Role role = getCitizenRole();

		landInfo.getOwners().forEach(owner -> {
			UserDetailResponse userDetailResponse = null;
			if (owner.getMobileNumber() != null) {
				if (owner.getTenantId() == null) {
					addUserDefaultFields(landInfo.getTenantId().split("\\.")[0], role, owner);
				}

				String searchResultUuid = getUserSearchResult(owner.getMobileNumber(), requestInfo,
						owner.getTenantId());

				if (searchResultUuid == null) {
					// if no user found with mobileNo,
					// creating new one..
					StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserContextPath())
							.append(config.getUserCreateEndpoint());
					setUserName(owner);
					owner.setOwnerType(BPAConstants.CITIZEN);
					userDetailResponse = userCall(new CreateUserRequest(requestInfo, owner), uri);
					log.info("owner created --> " + userDetailResponse.getUser().get(0).getUuid());
				} else {
					owner.setTenantId(landInfo.getTenantId());
					owner.setUuid(searchResultUuid);
					userDetailResponse = userExists(owner, requestInfo);
					if (!owner.equals(userDetailResponse.getUser().get(0))) {
						// User found but the user data got changed from UI,
						// update the same in user service
						StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserContextPath())
								.append(config.getUserUpdateEndpoint());
						OwnerInfo user = new OwnerInfo();
						user.addUserWithoutAuditDetail(owner);
						userDetailResponse = userCall(new CreateUserRequest(requestInfo, user), uri);
						log.info("owner updated --> " + userDetailResponse.getUser().get(0).getUuid());
						owner.setOwnerId(null);
					}
				}
				if (userDetailResponse != null)
					setOwnerFields(owner, userDetailResponse, requestInfo);
			}
		});
	}

	/**
	 * Checks if the user exists in the database
	 * 
	 * @param owner
	 *            The owner from the LandInfo
	 * @param requestInfo
	 *            The requestInfo of the request
	 * @return The search response from the user service
	 */
	private UserDetailResponse userExists(OwnerInfo owner, RequestInfo requestInfo) {

		UserSearchRequest userSearchRequest = new UserSearchRequest();
		userSearchRequest.setTenantId(owner.getTenantId().split("\\.")[0]);

		if (owner.getUuid() != null) {
			userSearchRequest.setUuid(Arrays.asList(owner.getUuid()));
		}

		StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
		return userCall(userSearchRequest, uri);
	}

	/**
	 * Sets the username as uuid
	 * 
	 * @param owner
	 *            The owner to whom the username is to assigned
	 */
	private void setUserName(OwnerInfo owner) {
		owner.setUserName(owner.getMobileNumber());
	}

	/**
	 * Sets ownerfields from the userResponse
	 * 
	 * @param owner
	 *            The owner from landInfo
	 * @param userDetailResponse
	 *            The response from user search
	 * @param requestInfo
	 *            The requestInfo of the request
	 */
	private void setOwnerFields(OwnerInfo owner, UserDetailResponse userDetailResponse, RequestInfo requestInfo) {
		owner.setId(userDetailResponse.getUser().get(0).getId());
		owner.setUuid(userDetailResponse.getUser().get(0).getUuid());
		owner.setUserName((userDetailResponse.getUser().get(0).getUserName()));
	}

	/**
	 * Sets the role,type,active and tenantId for a Citizen
	 * 
	 * @param tenantId
	 *            TenantId of the property
	 * @param role
	 *            The role of the user set in this case to CITIZEN
	 * @param owner
	 *            The user whose fields are to be set
	 */
	private void addUserDefaultFields(String tenantId, Role role, OwnerInfo owner) {
		owner.setTenantId(tenantId);
	}

	/**
	 * Creates citizen role
	 * 
	 * @return Role object for citizen
	 */
	private Role getCitizenRole() {
		Role role = new Role();
		role.setCode(BPAConstants.CITIZEN);
		role.setName("Citizen");
		return role;
	}

	public UserDetailResponse getUsersForBpas(List<LandInfo> landInfos) {
		UserSearchRequest userSearchRequest = new UserSearchRequest();
		List<String> ids = new ArrayList<String>();
		List<String> uuids = new ArrayList<String>();
		landInfos.forEach(landInfo -> {
			landInfo.getOwners().forEach(owner -> {
				if (owner.getUuid() != null)
					ids.add(owner.getUuid().toString());

				if (owner.getUuid() != null)
					uuids.add(owner.getUuid().toString());
			});
		});

		userSearchRequest.setId(ids);
		userSearchRequest.setUuid(uuids);
		StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
		return userCall(userSearchRequest, uri);
	}

	/**
	 * Returns UserDetailResponse by calling user service with given uri and
	 * object
	 * 
	 * @param userRequest
	 *            Request object for user service
	 * @param uri
	 *            The address of the end point
	 * @return Response from user service as parsed as userDetailResponse
	 */
	@SuppressWarnings("rawtypes")
	UserDetailResponse userCall(Object userRequest, StringBuilder uri) {
		String dobFormat = null;
		if (uri.toString().contains(config.getUserSearchEndpoint())
				|| uri.toString().contains(config.getUserUpdateEndpoint()))
			dobFormat = "yyyy-MM-dd";
		else if (uri.toString().contains(config.getUserCreateEndpoint()))
			dobFormat = "dd/MM/yyyy";
		try {
			LinkedHashMap responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(uri, userRequest);
			parseResponse(responseMap, dobFormat);
			UserDetailResponse userDetailResponse = mapper.convertValue(responseMap, UserDetailResponse.class);
			return userDetailResponse;
		} catch (IllegalArgumentException e) {
			throw new CustomException("IllegalArgumentException", "ObjectMapper not able to convertValue in userCall");
		}
	}

	private String getUserSearchResult(String userName, RequestInfo requestInfo, String tenantId) {

		StringBuilder uri = new StringBuilder();
		uri.append(config.getUserHost()).append(config.getUserSearchEndpoint());
		Map<String, Object> userSearchRequest = new HashMap<>();
		userSearchRequest.put("RequestInfo", requestInfo);
		userSearchRequest.put("tenantId", tenantId);
		userSearchRequest.put("userType", "CITIZEN");
		userSearchRequest.put("userName", userName);
		try {
			Object user = serviceRequestRepository.fetchResult(uri, userSearchRequest);
			if (null != user) {
				return JsonPath.read(user, "$.user[0].uuid");
			}
		} catch (Exception e) {
			log.error("Exception while fetching user for username - " + userName);
		}

		return null;
	}

	/**
	 * Parses date formats to long for all users in responseMap
	 * 
	 * @param responeMap
	 *            LinkedHashMap got from user api response
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void parseResponse(LinkedHashMap responeMap, String dobFormat) {
		List<LinkedHashMap> users = (List<LinkedHashMap>) responeMap.get("user");
		String format1 = "dd-MM-yyyy HH:mm:ss";
		if (users != null) {
			users.forEach(map -> {
				map.put("createdDate", dateTolong((String) map.get("createdDate"), format1));
				if ((String) map.get("lastModifiedDate") != null)
					map.put("lastModifiedDate", dateTolong((String) map.get("lastModifiedDate"), format1));
				if ((String) map.get("dob") != null)
					map.put("dob", dateTolong((String) map.get("dob"), dobFormat));
				if ((String) map.get("pwdExpiryDate") != null)
					map.put("pwdExpiryDate", dateTolong((String) map.get("pwdExpiryDate"), format1));
			});
		}
	}

	/**
	 * Converts date to long
	 * 
	 * @param date
	 *            date to be parsed
	 * @param format
	 *            Format of the date
	 * @return Long value of date
	 */
	private Long dateTolong(String date, String format) {
		SimpleDateFormat f = new SimpleDateFormat(format);
		Date d = null;
		try {
			d = f.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return d.getTime();
	}

	/**
	 * Call search in user service based on ownerids from criteria
	 * 
	 * @param criteria
	 *            The search criteria containing the ownerids
	 * @param requestInfo
	 *            The requestInfo of the request
	 * @return Search response from user service based on ownerIds
	 */
	public UserDetailResponse getUser(LandSearchCriteria criteria, RequestInfo requestInfo) {
		UserSearchRequest userSearchRequest = getUserSearchRequest(criteria, requestInfo);
		StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
		UserDetailResponse userDetailResponse = userCall(userSearchRequest, uri);
		return userDetailResponse;
	}

	/**
	 * Creates userSearchRequest from bpaSearchCriteria
	 * 
	 * @param criteria
	 *            The bpaSearch criteria
	 * @param requestInfo
	 *            The requestInfo of the request
	 * @return The UserSearchRequest based on ownerIds
	 */
	private UserSearchRequest getUserSearchRequest(LandSearchCriteria criteria, RequestInfo requestInfo) {
		UserSearchRequest userSearchRequest = new UserSearchRequest();
		userSearchRequest.setRequestInfo(requestInfo);
		userSearchRequest.setTenantId(criteria.getTenantId().split("\\.")[0]);
		userSearchRequest.setMobileNumber(criteria.getMobileNumber());
		userSearchRequest.setActive(true);
		userSearchRequest.setUserType(BPAConstants.CITIZEN);
		return userSearchRequest;
	}
}
