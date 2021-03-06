/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/
package org.olat.admin.registration;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.PermissionOnResourceable;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.persistence.DB;
import org.olat.core.configuration.Destroyable;
import org.olat.core.configuration.Initializable;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.StringHelper;
import org.olat.core.util.httpclient.HttpClientFactory;
import org.olat.core.util.i18n.I18nModule;
import org.olat.course.CourseModule;
import org.olat.group.BusinessGroupService;
import org.olat.group.model.SearchBusinessGroupParams;
import org.olat.instantMessaging.InstantMessagingModule;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 * Description:<br>
 * This manager offers methods to store registration preferences and to register
 * the installation on the olat.org server.
 * 
 * <P>
 * Initial Date: 12.12.2008 <br>
 * 
 * @author gnaegi
 */

public class SystemRegistrationManager extends BasicManager implements Initializable, Destroyable {

	private static final String SCHEDULER_NAME = "system.registration";
	private static final String TRIGGER = "system_registration_trigger";
	public static final String PRODUCT = "openolat";

	private final SystemRegistrationModule registrationModule;
	private final Scheduler scheduler;
	private final String clusterMode;
	private final DB database;
	private RepositoryManager repositoryManager;
	private BaseSecurity securityManager;
	private BusinessGroupService businessGroupService;

	private static final String REGISTRATION_SERVER = "http://registration.openolat.org/registration/restapi/registration/openolat";
	//private static final String REGISTRATION_SERVER = "http://localhost:8081/registration/restapi/registration/openolat";
	
	/**
	 * [used by spring]
	 * Use getInstance(), this is a singleton
	 */
	private SystemRegistrationManager(Scheduler scheduler, String clusterMode, DB database, SystemRegistrationModule registrationModule) {
		this.scheduler = scheduler;
		this.clusterMode = clusterMode;
		this.database = database;
		this.registrationModule = registrationModule;
	}
	
	/**
	 * Initialize the configuration
	 */
	public void init() {
		setupRegistrationBackgroundThread();
	}

	/**
	 * Call this to shutdown the cron scheduler and remove cluster event listeners
	 * from the PersistedProperties infrastructure
	 */
	public void destroy() {
		//
	}
	
	/**
	 * [used by Spring]
	 * @param repositoryManager
	 */
	public void setRepositoryManager(RepositoryManager repositoryManager) {
		this.repositoryManager = repositoryManager;
	}
	
	/**
	 * [used by Spring]
	 * @param securityManager
	 */
	public void setSecurityManager(BaseSecurity securityManager) {
		this.securityManager = securityManager;
	}
	
	/**
	 * [used by Spring]
	 * @param businessGroupService
	 */
	public void setBusinessGroupService(BusinessGroupService businessGroupService) {
		this.businessGroupService = businessGroupService;
	}

	/**
	 * Helper method to create a cron trigger expression. The method makes sure
	 * that not every olat installation submits at the same time
	 * 
	 * @return
	 */
	private String createCronTriggerExpression() {
		// Create a random hour and minute for the cronjob so that not every
		// installation registers at the same time
		int min = RandomUtils.nextInt(59);
		int hour = RandomUtils.nextInt(23);
		int day = RandomUtils.nextInt(6) + 1;
		String cronExpression = "0 " + min + " " + hour + " ? * "+ day;
		return cronExpression;
	}

	public String getLocationCoordinates(String textLocation){
		if (textLocation == null || textLocation.length()==0) {
			return null;
		}
		
		String csvCoordinates = null;
		CloseableHttpClient client = null;
		try {
			client = HttpClientFactory.getHttpClientInstance(true);
			URIBuilder uriBuilder = new URIBuilder("http://maps.google.com/maps/geo");
			List<NameValuePair> nvps = new ArrayList<NameValuePair>(5);
			nvps.add(new BasicNameValuePair("q",textLocation));
			nvps.add(new BasicNameValuePair("output","csv"));
			nvps.add(new BasicNameValuePair("oe","utf8"));
			nvps.add(new BasicNameValuePair("sensor","false"));
			nvps.add(new BasicNameValuePair("key","ABQIAAAAq5BZJrKbG-xh--W4MrciXRQZTOqTGVCcmpRMgrUbtlJvJ3buAhSfG7H7hgE66BCW17_gLyhitMNP4A"));
			uriBuilder.addParameters(nvps);

			HttpGet getCall = new HttpGet(uriBuilder.build());
			HttpResponse response = client.execute(getCall);
			String resp = null;
			if(response.getStatusLine().getStatusCode() == 200){
				resp = EntityUtils.toString(response.getEntity());
				String[] split = resp.split(",");
				csvCoordinates = split[2]+","+split[3];
			}
		} catch (Exception e) {
			//
		} finally {
			IOUtils.closeQuietly(client);
		}
		return csvCoordinates;
	}
	
	public void send() {
		try {
			scheduler.triggerJob(SCHEDULER_NAME, Scheduler.DEFAULT_GROUP);
		} catch (SchedulerException e) {
			logError("", e);
		}
	}
	
	/**
	 * Send the registration data now. If the user configured nothing to send,
	 * nothing will be sent.
	 */
	protected void sendRegistrationData() {
		HttpPut method = null;
		CloseableHttpClient client = null;
		try {
			// Do it optimistic and try to generate the XML message. If the message
			// doesn't contain anything, the user does not want to register this
			// instance
			Map<String,String> registrationData = getRegistrationPropertiesMessage();
			// only send when there is something to send
			UriBuilder builder = UriBuilder.fromUri(REGISTRATION_SERVER);
			for(Map.Entry<String, String> entry:registrationData.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				if(StringHelper.containsNonWhitespace(value)) {
					builder.queryParam(key, value);
				}
			}
			
			builder.queryParam("instanceId", registrationModule.getInstanceIdentifier());
			if(StringHelper.containsNonWhitespace(registrationModule.getSecretKey())) {
				String secretKey = registrationModule.getSecretKey();
				builder.queryParam("secretKey", secretKey);
			}
			builder.queryParam("product", PRODUCT);

			client = HttpClientFactory.getHttpClientInstance(true);
			String url = builder.build().toString();
			method = new HttpPut(url);
			HttpResponse response = client.execute(method);
			int status = response.getStatusLine().getStatusCode();
			if(status == HttpStatus.SC_CREATED) {
				logInfo("Successfully registered OLAT installation on openolat.org server, thank you for your support!", null);
				String registrationKey = EntityUtils.toString(response.getEntity());
				registrationModule.setSecretKey(registrationKey);
			} else if (status == HttpStatus.SC_NOT_MODIFIED || status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED) {
				logInfo("Successfully registered OLAT installation on openolat.org server, thank you for your support!", null);
			} else if (status == HttpStatus.SC_NOT_FOUND) {
				logError("Registration server not found: " + response.getStatusLine().toString(), null);
			} else if(status == HttpStatus.SC_NO_CONTENT){
				logInfo(EntityUtils.toString(response.getEntity()), response.getStatusLine().toString());
			} else {
				logError("Unexpected HTTP Status: " + response.getStatusLine().toString() + " during registration call", null);
			}
		} catch (Exception e) {
			logError("Unexpected exception during registration call", e);
		} finally {
			database.commitAndCloseSession();
			if(method != null) {
				method.releaseConnection();
			}
			IOUtils.closeQuietly(client);
		}
	}

	public Map<String,String> getRegistrationPropertiesMessage() {
		Map<String,String> msgProperties = new HashMap<String,String>();
		
		boolean website = registrationModule.isPublishWebsite();
		boolean notify = registrationModule.isNotifyReleases();
		//OLAT version
		msgProperties.put("appName", Settings.getApplicationName());
		msgProperties.put("version", Settings.getFullVersionInfo());
		
		//Location
		msgProperties.put("location", registrationModule.getLocation());
		msgProperties.put("locationCSV", registrationModule.getLocationCoordinates());
		
		// System config
		msgProperties.put("instantMessagingEnabled", String.valueOf(CoreSpringFactory.getImpl(InstantMessagingModule.class).isEnabled()));
		msgProperties.put("enabledLanguages", I18nModule.getEnabledLanguageKeys().toString());
		msgProperties.put("clusterEnabled", clusterMode);
		msgProperties.put("debuggingEnabled", String.valueOf(Settings.isDebuging()));
		
		// Course counts
		int allCourses = repositoryManager.countByTypeLimitAccess(CourseModule.ORES_TYPE_COURSE, RepositoryEntry.ACC_OWNERS);
		int publishedCourses = repositoryManager.countByTypeLimitAccess(CourseModule.ORES_TYPE_COURSE, RepositoryEntry.ACC_USERS);
		msgProperties.put("courses", String.valueOf(allCourses));
		msgProperties.put("coursesPublished", String.valueOf(publishedCourses));
		
		// User counts
		SecurityGroup olatuserGroup = securityManager.findSecurityGroupByName(Constants.GROUP_OLATUSERS);
		int users = securityManager.countIdentitiesOfSecurityGroup(olatuserGroup);
		long disabled = securityManager.countIdentitiesByPowerSearch(null, null, true, null, null, null, null, null, null, null, Identity.STATUS_LOGIN_DENIED);
		msgProperties.put("usersEnabled", String.valueOf(users - disabled));
				
		PermissionOnResourceable[] permissions = { new PermissionOnResourceable(Constants.PERMISSION_HASROLE, Constants.ORESOURCE_AUTHOR) };
		long authors = securityManager.countIdentitiesByPowerSearch(null, null, true, null, permissions, null, null, null, null, null, null);
		msgProperties.put("authors", String.valueOf(authors));
		
		// Activity
		Calendar lastLoginLimit = Calendar.getInstance();
		lastLoginLimit.add(Calendar.DAY_OF_YEAR, -6); // -1 - 6 = -7 for last week
		Long activeUsersLastWeek = securityManager.countUniqueUserLoginsSince(lastLoginLimit.getTime());
		msgProperties.put("activeUsersLastWeek", String.valueOf(activeUsersLastWeek));
		lastLoginLimit = Calendar.getInstance();
		lastLoginLimit.add(Calendar.MONTH, -1);
		Long activeUsersLastMonth = securityManager.countUniqueUserLoginsSince(lastLoginLimit.getTime());
		msgProperties.put("activeUsersLastMonth", String.valueOf(activeUsersLastMonth));
		
		// Groups
		SearchBusinessGroupParams params = new SearchBusinessGroupParams();
		int groups = businessGroupService.countBusinessGroups(params, null);
		msgProperties.put("buddyGroups", String.valueOf(groups));
		msgProperties.put("learningGroups", String.valueOf(groups));
		msgProperties.put("rightGroups", String.valueOf(groups));
		msgProperties.put("groups", String.valueOf(groups));
			
		if (website) {
			// URL
			msgProperties.put("url", Settings.getServerContextPathURI());
			// Description
			String desc = registrationModule.getWebsiteDescription();
			msgProperties.put("description", desc);
		}
		if (notify) {
			// Email
			String email = registrationModule.getEmail();
			msgProperties.put("email", email);
		}
		return msgProperties;
	}

	/**
	 * Method to initialize the registration submission scheduler. The scheduler
	 * normally runs once a week and submitts the most current data.
	 */
	public void setupRegistrationBackgroundThread() {
		// Only run scheduler on first cluster node
		// This is accomplished by the SystemRegistrationJobStarter which is configured and ensured to run only once in a cluster from within 
		// the olatextconfig.xml. This Job uses this method to setup the cronjob defined with the cronexpressioin from the properties.
		// 
		
		// Don't run in jUnit mode
		if (Settings.isJUnitTest()) return;

		String cronExpression = createCronTriggerExpression();
		try {
			// Create job with cron trigger configuration
			JobDetail jobDetail = new JobDetail(SCHEDULER_NAME, Scheduler.DEFAULT_GROUP, SystemRegistrationJob.class);
			CronTrigger trigger = new CronTrigger();
			trigger.setName(TRIGGER);
			// Use this cron expression for debugging, tries to send data every minute
			//trigger.setCronExpression("0 * * * * ?");
			trigger.setCronExpression(cronExpression);
			// Schedule job now
			scheduler.scheduleJob(jobDetail, trigger);
		} catch (ParseException e) {
			logError("Illegal cron expression for system registration", e);
		} catch (SchedulerException e) {
			logError("Can not start system registration scheduler", e);
		}
		logInfo("Registration background job successfully started: "+cronExpression, null);
	}
}