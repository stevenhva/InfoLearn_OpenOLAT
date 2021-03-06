/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * 12.10.2011 by frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.vitero.manager;

import java.io.File;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.ConnectException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.annotation.PostConstruct;
import javax.ws.rs.core.UriBuilder;
import javax.xml.soap.SOAPElement;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.PortInfo;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.olat.basesecurity.Authentication;
import org.olat.basesecurity.BaseSecurity;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.StringHelper;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.xml.XStreamHelper;
import org.olat.group.BusinessGroup;
import org.olat.modules.vitero.ViteroModule;
import org.olat.modules.vitero.model.CheckUserInfo;
import org.olat.modules.vitero.model.ErrorCode;
import org.olat.modules.vitero.model.GetUserInfo;
import org.olat.modules.vitero.model.GroupRole;
import org.olat.modules.vitero.model.ViteroBooking;
import org.olat.modules.vitero.model.ViteroGroup;
import org.olat.modules.vitero.model.ViteroGroupRoles;
import org.olat.modules.vitero.model.ViteroStatus;
import org.olat.modules.vitero.model.ViteroUser;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;
import org.olat.user.DisplayPortraitManager;
import org.olat.user.UserDataDeletable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.CompactWriter;

import de.vitero.ViteroSecurityHandler;
import de.vitero.schema.booking.Booking;
import de.vitero.schema.booking.BookingService;
import de.vitero.schema.booking.Booking_Type;
import de.vitero.schema.booking.Bookingid;
import de.vitero.schema.booking.Bookinglist;
import de.vitero.schema.booking.Bookingtype;
import de.vitero.schema.booking.CreateBookingRequest;
import de.vitero.schema.booking.CreateBookingResponse;
import de.vitero.schema.booking.DeleteBookingRequest;
import de.vitero.schema.booking.DeleteBookingResponse;
import de.vitero.schema.booking.GetBookingListByDateRequest;
import de.vitero.schema.booking.GetBookingListByUserInFutureRequest;
import de.vitero.schema.booking.Newbookingtype;
import de.vitero.schema.group.ChangeGroupRoleRequest;
import de.vitero.schema.group.Completegrouptype;
import de.vitero.schema.group.CreateGroupRequest;
import de.vitero.schema.group.Group;
import de.vitero.schema.group.GroupService;
import de.vitero.schema.group.Group_Type;
import de.vitero.schema.group.Groupid;
import de.vitero.schema.group.Groupiduserid;
import de.vitero.schema.group.Groupnamecustomerid;
import de.vitero.schema.licence.GetBookableRoomsForGroupResponse;
import de.vitero.schema.licence.GetBookableRoomsForGroupResponse.Rooms;
import de.vitero.schema.licence.GetModulesForCustomerRequest;
import de.vitero.schema.licence.Grouprequesttype;
import de.vitero.schema.licence.Licence;
import de.vitero.schema.licence.LicenceService;
import de.vitero.schema.licence.Modulestype;
import de.vitero.schema.licence.Modulestype.Modules;
import de.vitero.schema.licence.Modulestype.Modules.Module;
import de.vitero.schema.mtom.CompleteAvatarWrapper;
import de.vitero.schema.mtom.Mtom;
import de.vitero.schema.mtom.MtomService;
import de.vitero.schema.sessioncode.Codetype;
import de.vitero.schema.sessioncode.CreatePersonalBookingSessionCodeRequest;
import de.vitero.schema.sessioncode.CreateVmsSessionCodeRequest;
import de.vitero.schema.sessioncode.SessionCode;
import de.vitero.schema.sessioncode.SessionCodeService;
import de.vitero.schema.user.Completeusertype;
import de.vitero.schema.user.CreateUserRequest;
import de.vitero.schema.user.GetUserListByCustomerRequest;
import de.vitero.schema.user.GetUserListByGroupRequest;
import de.vitero.schema.user.Newusertype;
import de.vitero.schema.user.UpdateUserRequest;
import de.vitero.schema.user.UserService;
import de.vitero.schema.user.Userid;
import de.vitero.schema.user.Userlist;
import de.vitero.schema.user.Usertype;

/**
 * 
 * Description:<br>
 * Implementation of the Virtual Classroom for the Vitero Booking System
 * 
 * <P>
 * Initial Date:  26 sept. 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
@Service
public class ViteroManager extends BasicManager implements UserDataDeletable {
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
	
	private static final String VMS_PROVIDER = "VMS";
	private static final String VMS_CATEGORY = "vitero-category";
	private static final String VMS_CATEGORY_ZOMBIE = "vitero-category-zombie";
	
	@Autowired
	private ViteroModule viteroModule;
	@Autowired
	private PropertyManager propertyManager;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private DB dbInstance;
	
	private XStream xStream;

	public ViteroManager() {
		//make Spring happy
	}
	
	@PostConstruct
	public void init() {
		xStream = XStreamHelper.createXStreamInstance();
		xStream.alias("vBooking", ViteroBooking.class);
		xStream.omitField(ViteroBooking.class, "property");
	}
	
	public void setViteroModule(ViteroModule module) {
		this.viteroModule = module;
	}
	
	public List<ViteroBooking> getBookingByDate(Date start, Date end) 
	throws VmsNotAvailableException {
		try {
			Booking bookingWs = getBookingWebService();
			GetBookingListByDateRequest dateRequest = new GetBookingListByDateRequest();
			dateRequest.setStart(format(start));
			dateRequest.setEnd(format(end));
			dateRequest.setTimezone(viteroModule.getTimeZoneId());
			dateRequest.setCustomerid(viteroModule.getCustomerId());
			Bookinglist bookingList = bookingWs.getBookingListByDate(dateRequest);
			List<Booking_Type> bookings = bookingList.getBooking();
			return convert(bookings);
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				default: logAxisError("Cannot get the list of bookings by date.", f);
			}
			return Collections.emptyList();
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			return Collections.emptyList();
		}
	}
	
	public boolean canGoBooking(ViteroBooking booking) {
		Date now = new Date();
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(booking.getStart());
		cal.add(Calendar.MINUTE, -booking.getStartBuffer());
		Date start = cal.getTime();
		cal.setTime(booking.getEnd());
		cal.add(Calendar.MINUTE, booking.getEndBuffer());
		Date end = cal.getTime();
		
		if(start.before(now) && end.after(now)) {
			return true;
		}
		return false;
	}
	
	public String getURLToBooking(Identity identity, ViteroBooking booking)
	throws VmsNotAvailableException {
		String sessionCode = createPersonalBookingSessionCode(identity, booking);
		String url = getStartPoint(sessionCode);
		return url;
	}
	
	public String getURLToGroup(Identity identity, ViteroBooking booking)
	throws VmsNotAvailableException {
		String sessionCode = createVMSSessionCode(identity);
		if(sessionCode == null) {
			return null;
		} else {
			String url = getGroupURL(sessionCode, booking.getGroupId());
			return url;
		}
	}
	
	/**
	 * Create a session code with a one hour expiration date
	 * @param identity
	 * @param booking
	 * @return
	 */
	protected String createVMSSessionCode(Identity identity)
	throws VmsNotAvailableException {
		try {
			GetUserInfo userInfo = getVmsUserId(identity, true);
			int userId = userInfo.getUserId();
			
			//update user information
			if(!userInfo.isCreated()) {
				try {
					updateVmsUser(identity, userId);
					storePortrait(identity, userId);
				} catch (Exception e) {
					logError("Cannot update user on vitero system:" + identity.getName(), e);
				}
			}
			
			CreateVmsSessionCodeRequest.Sessioncode code = new CreateVmsSessionCodeRequest.Sessioncode();
			code.setUserid(userId);
			code.setTimezone(viteroModule.getTimeZoneId());
		
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			cal.add(Calendar.HOUR, 1);
			code.setExpirationdate(format(cal.getTime()));
			
			CreateVmsSessionCodeRequest codeRequest = new CreateVmsSessionCodeRequest();
			codeRequest.setSessioncode(code);
			Codetype myCode = getSessionCodeWebService().createVmsSessionCode(codeRequest);
			return myCode.getCode();
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				case userDoesntExist: logError("User does not exist.", f); break;
				case userNotAssignedToGroup: logError("User not assigned to group.", f); break;
				case invalidAttribut: logError("Invalid attribute.", f); break; 
				case invalidTimezone: logError("Invalid time zone.", f); break;
				case bookingDoesntExist:
				case bookingDoesntExistPrime: logError("Booking does not exist.", f); break;
				default: logAxisError("Cannot create session code.", f);
			}
			return null;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot create session code.", e);
			return null;
		}
	}
	
	/**
	 * Create a session code with a one hour expiration date
	 * @param identity
	 * @param booking
	 * @return
	 */
	protected String createPersonalBookingSessionCode(Identity identity, ViteroBooking booking)
	throws VmsNotAvailableException {
		try {
			GetUserInfo userInfo = getVmsUserId(identity, true);
			int userId = userInfo.getUserId();
			
			//update user information
			if(!userInfo.isCreated()) {
				try {
					updateVmsUser(identity, userId);
					storePortrait(identity, userId);
				} catch (Exception e) {
					logError("Cannot update user on vitero system:" + identity.getName(), e);
				}
			}

			
			CreatePersonalBookingSessionCodeRequest.Sessioncode code = new CreatePersonalBookingSessionCodeRequest.Sessioncode();
			code.setBookingid(booking.getBookingId());
			code.setUserid(userId);
			code.setTimezone(viteroModule.getTimeZoneId());
		
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			cal.add(Calendar.HOUR, 1);
			code.setExpirationdate(format(cal.getTime()));
			
			CreatePersonalBookingSessionCodeRequest codeRequest = new CreatePersonalBookingSessionCodeRequest();
			codeRequest.setSessioncode(code);
			Codetype myCode = getSessionCodeWebService().createPersonalBookingSessionCode(codeRequest);
			return myCode.getCode();
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				case userDoesntExist: logError("User does not exist.", f); break;
				case userNotAssignedToGroup: logError("User not assigned to group.", f); break;
				case invalidAttribut: logError("Invalid attribute.", f); break; 
				case invalidTimezone: logError("Invalid time zone.", f); break;
				case bookingDoesntExist:
				case bookingDoesntExistPrime: logError("Booking does not exist.", f); break;
				default: logAxisError("Cannot create session code.", f);
			}
			return null;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot create session code.", e);
			return null;
		}
	}
	
	public ViteroGroupRoles getGroupRoles(int id)
	throws VmsNotAvailableException {
		try {
			Group groupWs = getGroupWebService();
			Groupid groupId = new Groupid();
			groupId.setGroupid(id);
			
			Group_Type group = groupWs.getGroup(groupId);
			Completegrouptype groupType = group.getGroup();
			List<Completegrouptype.Participant> participants = groupType.getParticipant();
			int numOfParticipants = participants == null ? 0 : participants.size();

			ViteroGroupRoles groupRoles = new ViteroGroupRoles();
			if(numOfParticipants > 0) {
				Map<Integer,String> idToEmails = new HashMap<Integer,String>();
				List<Usertype> vmsUsers = getVmsUsersByGroup(id);
				if(vmsUsers != null) {
					for(Usertype vmsUser:vmsUsers) {
						Integer userId = new Integer(vmsUser.getId());
						String email = vmsUser.getEmail();
						groupRoles.getEmailsOfParticipants().add(email);
						idToEmails.put(userId, email);
					}	
				}
				
				for(int i=0; i<numOfParticipants; i++) {
					Completegrouptype.Participant participant = participants.get(i);
					Integer userId = new Integer(participant.getUserid());
					String email = idToEmails.get(userId);
					if(email != null) {
						GroupRole role = GroupRole.valueOf(participant.getRole());
						groupRoles.getEmailsToRole().put(email, role);
					}
				}
			}

			return groupRoles;
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				default: logAxisError("Cannot get group roles",f);
			}
			return null;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			return null;
		}	
	}
	
	public boolean isUserOf(ViteroBooking booking, Identity identity)
	throws VmsNotAvailableException {
		boolean member = false;
		GetUserInfo userInfo = getVmsUserId(identity, false);
		int userId = userInfo.getUserId();
		if(userId > 0) {
			List<Usertype> users = getVmsUsersByGroup(booking.getGroupId());
			if(users != null) {
				for(Usertype user:users) {
					if(userId == user.getId()) {
						member = true;
					}
				}
			}
		}
		return member;
	}
	
	public List<ViteroUser> getUsersOf(ViteroBooking booking) 
	throws VmsNotAvailableException {
		return convertUsertype(getVmsUsersByGroup(booking.getGroupId()));
	}
	
	public List<Usertype> getCustomersUsers() throws VmsNotAvailableException {
		try {
			GetUserListByCustomerRequest listRequest = new GetUserListByCustomerRequest();
			listRequest.setCustomerid(viteroModule.getCustomerId());
			Userlist userList = getUserWebService().getUserListByCustomer(listRequest);
			List<Usertype> userTypes = userList.getUser();
			return userTypes;
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				default: logAxisError("Cannot get the list of users of customer: " + viteroModule.getCustomerId(), f);
			}
			return null;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot get the list of users of customer: " + viteroModule.getCustomerId(), e);
			return null;
		}
	}
	
	protected List<Usertype> getVmsUsersByGroup(int groupId)
	throws VmsNotAvailableException {
		try {
			GetUserListByGroupRequest listRequest = new GetUserListByGroupRequest();
			listRequest.setGroupid(groupId);
			Userlist userList = getUserWebService().getUserListByGroup(listRequest);
			List<Usertype> userTypes = userList.getUser();
			return userTypes;
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				default: logAxisError("Cannot get the list of users in group: " + groupId, f);
			}
			return null;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot get the list of users in group: " + groupId, e);
			return null;
		}
	}
	
	protected GetUserInfo getVmsUserId(Identity identity, boolean create) 
	throws VmsNotAvailableException {
		int userId;
		boolean created = false;
		
		closeDBSessionSafely();

		Authentication authentication = securityManager.findAuthentication(identity, VMS_PROVIDER);
		if(authentication == null) {
			if(create) {
				created = true;
				userId =  createVmsUser(identity);
				if(userId > 0) {
					securityManager.createAndPersistAuthentication(identity, VMS_PROVIDER, Integer.toString(userId), null, null);
				}
			} else {
				userId = -1;
			}
		} else {
			userId = Integer.parseInt(authentication.getAuthusername());
		}
		
		closeDBSessionSafely();
		
		return new GetUserInfo(created, userId);
	}
	
	private void closeDBSessionSafely() {
		try {
			dbInstance.commitAndCloseSession();
		} catch (Exception e) {
			logError("Close safely for VMS", e);
		}
	}
	
	protected boolean updateVmsUser(Identity identity, int vmsUserId)
	throws VmsNotAvailableException {
		try {
			UpdateUserRequest updateRequest = new UpdateUserRequest();
			Completeusertype user = new Completeusertype();
			user.setId(vmsUserId);
			
			//mandatory
			User olatUser = identity.getUser();
			user.setUsername("olat." + WebappHelper.getInstanceId() + "." + identity.getName());
			user.setSurname(olatUser.getProperty(UserConstants.LASTNAME, null));
			user.setFirstname(olatUser.getProperty(UserConstants.FIRSTNAME, null));
			user.setEmail(olatUser.getProperty(UserConstants.EMAIL, null));

			//optional
			String language = identity.getUser().getPreferences().getLanguage();
			if(StringHelper.containsNonWhitespace(language) && language.startsWith("de")) {
				user.setLocale("de");
			} else {
				user.setLocale("en");
			}
			user.setPcstate("NOT_TESTED");
			user.setTimezone(viteroModule.getTimeZoneId());
			
			String street = olatUser.getProperty(UserConstants.STREET, null);
			if(StringHelper.containsNonWhitespace(street)) {
				user.setStreet(street);
			}
			String zip = olatUser.getProperty(UserConstants.ZIPCODE, null);
			if(StringHelper.containsNonWhitespace(zip)) {
				user.setZip(zip);
			}
			String city = olatUser.getProperty(UserConstants.CITY, null);
			if(StringHelper.containsNonWhitespace(city)) {
				user.setCity(city);
			}
			String country = olatUser.getProperty(UserConstants.COUNTRY, null);
			if(StringHelper.containsNonWhitespace(country)) {
				user.setCountry(country);
			}
			
			String mobile = olatUser.getProperty(UserConstants.TELMOBILE, null);
			if(StringHelper.containsNonWhitespace(mobile)) {
				user.setMobile(mobile);
			}
			String phonePrivate = olatUser.getProperty(UserConstants.TELPRIVATE, null);
			if(StringHelper.containsNonWhitespace(phonePrivate)) {
				user.setPhone(phonePrivate);
			}
			String phoneOffice = olatUser.getProperty(UserConstants.TELOFFICE, null);
			if(StringHelper.containsNonWhitespace(phoneOffice)) {
				user.setPhone(phoneOffice);
			}
			String institution = olatUser.getProperty(UserConstants.INSTITUTIONALNAME, null);
			if(StringHelper.containsNonWhitespace(institution)) {
				user.setCompany(institution);
			}
			
			updateRequest.setUser(user);
			getUserWebService().updateUser(updateRequest);

			return true;
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				default: logAxisError("Cannot create vms user.", f);
			}
			return true;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot create vms user.", e);
			return true;
		}
	}
	
	private final int createVmsUser(Identity identity)
	throws VmsNotAvailableException {
		String username = null;
		try {
			CreateUserRequest createRequest = new CreateUserRequest();
			Newusertype user = new Newusertype();
			
			//mandatory
			User olatUser = identity.getUser();
			username = "olat." + WebappHelper.getInstanceId() + "." + identity.getName();
			user.setUsername(username);
			user.setSurname(olatUser.getProperty(UserConstants.LASTNAME, null));
			user.setFirstname(olatUser.getProperty(UserConstants.FIRSTNAME, null));
			user.setEmail(olatUser.getProperty(UserConstants.EMAIL, null));
			user.setPassword("changeme");
			int customerId =viteroModule.getCustomerId();
			user.getCustomeridlist().add(new Integer(customerId));

			//optional
			String language = identity.getUser().getPreferences().getLanguage();
			if(StringHelper.containsNonWhitespace(language) && language.startsWith("de")) {
				user.setLocale("de");
			} else {
				user.setLocale("en");
			}
			user.setPcstate("NOT_TESTED");
			user.setTimezone(viteroModule.getTimeZoneId());
			
			String street = olatUser.getProperty(UserConstants.STREET, null);
			if(StringHelper.containsNonWhitespace(street)) {
				user.setStreet(street);
			}
			String zip = olatUser.getProperty(UserConstants.ZIPCODE, null);
			if(StringHelper.containsNonWhitespace(zip)) {
				user.setZip(zip);
			}
			String city = olatUser.getProperty(UserConstants.CITY, null);
			if(StringHelper.containsNonWhitespace(city)) {
				user.setCity(city);
			}
			String country = olatUser.getProperty(UserConstants.COUNTRY, null);
			if(StringHelper.containsNonWhitespace(country)) {
				user.setCountry(country);
			}
			
			String mobile = olatUser.getProperty(UserConstants.TELMOBILE, null);
			if(StringHelper.containsNonWhitespace(mobile)) {
				user.setMobile(mobile);
			}
			String phonePrivate = olatUser.getProperty(UserConstants.TELPRIVATE, null);
			if(StringHelper.containsNonWhitespace(phonePrivate)) {
				user.setPhone(phonePrivate);
			}
			String phoneOffice = olatUser.getProperty(UserConstants.TELOFFICE, null);
			if(StringHelper.containsNonWhitespace(phoneOffice)) {
				user.setPhone(phoneOffice);
			}
			String institution = olatUser.getProperty(UserConstants.INSTITUTIONALNAME, null);
			if(StringHelper.containsNonWhitespace(institution)) {
				user.setCompany(institution);
			}
			/*
			user.setTitle("");
			*/
			user.setTechnicalnote("Generated by OpenOLAT");
			
			createRequest.setUser(user);
			Userid userId = getUserWebService().createUser(createRequest);
			
			storePortrait(identity, userId.getUserid());
			return userId.getUserid();
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				default: logAxisError("Cannot create vms user.", f);
			}
			return -1;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot create vms user.", e);
			return -1;
		}
	}
	
	protected boolean storePortrait(Identity identity, int userId)
	throws VmsNotAvailableException {
		try {
			File portrait = DisplayPortraitManager.getInstance().getBigPortrait(identity.getName());
			if(portrait != null && portrait.exists()) {
				Mtom mtomWs = getMtomWebService();
				
				CompleteAvatarWrapper avatar = new CompleteAvatarWrapper();
				
				avatar.setType(BigInteger.ZERO);
				avatar.setUserid(BigInteger.valueOf(userId));
				avatar.setFilename(portrait.getName());
				
				DataHandler portraitHandler = new DataHandler(new FileDataSource(portrait));
				avatar.setFile(portraitHandler);

				mtomWs.storeAvatar(avatar);
				return true;
			}
			return false;
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				default: logAxisError("Cannot store the portrait of " + userId, f);
			}
			return false;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot store the portrait of " + userId, e);
			return false;
		}
	}
	
	@Override
	public void deleteUserData(Identity identity, String newDeletedUserName) {
		if(!viteroModule.isDeleteVmsUserOnUserDelete()) return;
		
		try {
			GetUserInfo userInfo = getVmsUserId(identity, false);
			int userId = userInfo.getUserId();
			if(userId > 0) {
				deleteVmsUser(userId);
			}
		} catch (VmsNotAvailableException e) {
			logError("Cannot delete a vms user after a OLAT user deletion.", e);
		}
	}
	
	protected void deleteVmsUser(int userId) 
	throws VmsNotAvailableException {
		try {
			Userid userIdType = new Userid();
			userIdType.setUserid(userId);
			getUserWebService().deleteUser(userIdType);
		} catch (SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				default: logAxisError("Cannot delete vms user: " + userId, f);
			}
		} catch(WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot delete vms user: " + userId, e);
		}
	}

	public List<Integer> getLicencedRoomSizes() 
	throws VmsNotAvailableException {
		List<Integer> roomSizes = new ArrayList<Integer>();
		try {
			GetModulesForCustomerRequest licenceRequest = new GetModulesForCustomerRequest();
			licenceRequest.setCustomerid(viteroModule.getCustomerId());
			
			Modulestype modules = getLicenceWebService().getModulesForCustomer(licenceRequest);
			Modules modulesType = modules.getModules();
			for(Module module:modulesType.getModule()) {
				if("ROOM".equals(module.getType())) {
					Integer roomSize = module.getRoomsize();
					if(!roomSizes.contains(roomSize)) {
						roomSizes.add(roomSize);
					}
				}
			}
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				case invalidAttribut: logError("ids <=0 or invalid attributs", f); break;
				default: logAxisError("Cannot get licence for customer: " + viteroModule.getCustomerId(), f);
			}
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot get licence for customer: " + viteroModule.getCustomerId(), e);
		}
		return roomSizes;
	}
	
	public List<Integer> getLicenceForAvailableRooms(Date begin, Date end) 
	throws VmsNotAvailableException {
		List<Integer> roomSizes = new ArrayList<Integer>();
		try {
			Grouprequesttype groupRequest = new Grouprequesttype();
			groupRequest.setStart(format(begin));
			groupRequest.setEnd(format(end));

			GetBookableRoomsForGroupResponse response = getLicenceWebService().getBookableRoomsForGroup(groupRequest);
			Rooms rooms = response.getRooms();
			for(Integer roomSize : rooms.getRoomsize()) {
				if(!roomSizes.contains(roomSize)) {
					roomSizes.add(roomSize);
				}
			}
			
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				default: logAxisError("Cannot get licence for available room by dates.", f);
			}
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot get licence for available room by dates.", e);
		}
		return roomSizes;
	}
	
	public int createGroup(String groupName)
	throws VmsNotAvailableException {
		try {
			CreateGroupRequest createRequest = new CreateGroupRequest();
			Groupnamecustomerid groupInfos = new Groupnamecustomerid();
			groupInfos.setGroupname(groupName + "_OLAT_" + UUID.randomUUID().toString().replace("-", ""));
			groupInfos.setCustomerid(viteroModule.getCustomerId());
			createRequest.setGroup(groupInfos);
			
			Groupid groupId = getGroupWebService().createGroup(createRequest);
			return groupId.getGroupid();
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				default: logAxisError("Cannot create a group",f);
			}
			return -1;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot create a group.", e);
			return -1;
		}
	}
	
	public ViteroGroup getGroup(int id)
	throws VmsNotAvailableException {
		try {
			Groupid groupId = new Groupid();
			groupId.setGroupid(id);
			Group_Type group = getGroupWebService().getGroup(groupId);
			Completegrouptype groupType = group.getGroup();
			return convert(groupType);
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				default: logAxisError("Cannot create a group",f);
			}
			return null;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot create a group.", e);
			return null;
		}
	}
	
	public boolean deleteGroup(ViteroBooking vBooking)
	throws VmsNotAvailableException {
		try {
			Groupid groupId = new Groupid();
			groupId.setGroupid(vBooking.getGroupId());
			getGroupWebService().deleteGroup(groupId);
			return true;
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				case groupDoesntExist: logError("Group doesn't exist!", f); break;
				case invalidAttribut: logError("Group id <= 0!", f);
				default: logAxisError("Cannot delete group: " + vBooking.getGroupId(), f);
			}
			return false;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot delete group: " + vBooking.getGroupId(), e);
			return false;
		}
	}
	
	public boolean addToRoom(ViteroBooking booking, Identity identity, GroupRole role)
	throws VmsNotAvailableException {
		try {
			GetUserInfo userInfo = getVmsUserId(identity, true);
			int userId = userInfo.getUserId();
			if(userId < 0) {
				return false;
			}
			
			if(!userInfo.isCreated()) {
			//update user information
				try {
					updateVmsUser(identity, userId);
					//storePortrait(identity, userId);
				} catch (Exception e) {
					logError("Cannot update user on vitero system:" + identity.getName(), e);
				}
			}
			
			Group groupWs = getGroupWebService();
			Groupiduserid groupuserId = new Groupiduserid();
			groupuserId.setGroupid(booking.getGroupId());
			groupuserId.setUserid(userId);
			groupWs.addUserToGroup(groupuserId);
			
			if(role != null) {
				groupWs = getGroupWebService();
				ChangeGroupRoleRequest roleRequest = new ChangeGroupRoleRequest();
				roleRequest.setGroupid(booking.getGroupId());
				roleRequest.setUserid(userId);
				roleRequest.setRole(role.getVmsValue());
				groupWs.changeGroupRole(roleRequest);
			}
			
			return true;
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				case userDoesntExist: logError("The user doesn ́t exist!", f); break;
				case userNotAttachedToCustomer: logError("The user is not attached to the customer (to which this group belongs)", f); break;
				case groupDoesntExist: logError("The group doesn ́t exist", f); break;
				case invalidAttribut: logError("An id <= 0", f); break;
				default: logAxisError("Cannot add an user to a group", f);
			}
			return false;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot add an user to a group", e);
			return false;
		}
	}
	
	public boolean removeFromRoom(ViteroBooking booking, Identity identity)
	throws VmsNotAvailableException {
		GetUserInfo userInfo = getVmsUserId(identity, true);
		int userId = userInfo.getUserId();
		if(userId < 0) {
			return true;//nothing to remove
		}
		return removeFromRoom(booking, userId);
	}
	
	public boolean removeFromRoom(ViteroBooking booking, int userId)
	throws VmsNotAvailableException {
		try {
			Groupiduserid groupuserId = new Groupiduserid();
			groupuserId.setGroupid(booking.getGroupId());
			groupuserId.setUserid(userId);
			getGroupWebService().removeUserFromGroup(groupuserId);
			return true;
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				case userDoesntExist: logError("The user doesn ́t exist!", f); break;
				case groupDoesntExist: logError("The group doesn ́t exist", f); break;
				case invalidAttribut: logError("An id <= 0", f); break;
				default: logAxisError("Cannot remove an user from a group", f);
			}
			return false;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot remove an user from a group", e);
			return false;
		}
	}
	
	public ViteroBooking createBooking(String resourceName)
	throws VmsNotAvailableException {
		ViteroBooking booking = new ViteroBooking();
		
		booking.setBookingId(-1);
		booking.setGroupId(-1);
		booking.setResourceName(resourceName);
		
		Calendar cal = Calendar.getInstance();
		int minute = cal.get(Calendar.MINUTE);
		if(minute < 10) {
			cal.set(Calendar.MINUTE, 15);
		} else if (minute < 25) {
			cal.set(Calendar.MINUTE, 30);
		} else if (minute < 40) {
			cal.set(Calendar.MINUTE, 45);
		} else {
			cal.add(Calendar.HOUR, 1);
			cal.set(Calendar.MINUTE, 0);
		}

		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		booking.setStart(cal.getTime());
		booking.setStartBuffer(15);
		cal.add(Calendar.HOUR, 1);
		booking.setEnd(cal.getTime());
		booking.setEndBuffer(15);
		
		List<Integer> roomSizes = getLicencedRoomSizes();
		if(!roomSizes.isEmpty()) {
			booking.setRoomSize(roomSizes.get(0));
		}
		return booking;
	}

	public ViteroStatus createBooking(BusinessGroup group, OLATResourceable ores, String subIdentifier, ViteroBooking vBooking)
	throws VmsNotAvailableException {
		Bookingtype booking = getBookingById(vBooking.getBookingId());
		if(booking != null) {
			logInfo("Booking already exists: " + vBooking.getBookingId());
			return new ViteroStatus();
		}

		try {
			//a group per meeting
			String groupName = vBooking.getGroupName();
			int groupId = createGroup(groupName);
			if(groupId < 0) {
				return new ViteroStatus(ErrorCode.unkown);
			}
			vBooking.setGroupId(groupId);
			
			//create the meeting with the new group
			Booking bookingWs = getBookingWebService();
			CreateBookingRequest createRequest = new CreateBookingRequest();
			Newbookingtype newBooking = new Newbookingtype();
			//mandatory
			newBooking.setStart(format(vBooking.getStart()));
			newBooking.setEnd(format(vBooking.getEnd()));
			newBooking.setStartbuffer(vBooking.getStartBuffer());
			newBooking.setEndbuffer(vBooking.getEndBuffer());
			newBooking.setGroupid(groupId);
			newBooking.setRoomsize(vBooking.getRoomSize());
			
			//optional
			/*
			newBooking.setIgnorefaults(false);
			newBooking.setCafe(false);
			newBooking.setCapture(false);
			//phone
			BookingServiceStub.Phonetype phone = new BookingServiceStub.Phonetype();
			phone.setDialout(false);
			phone.setPhoneconference(false);
			phone.setShowdialogue(false);
			newBooking.setPhone(phone);

			newBooking.setPcstateokrequired(false);
			newBooking.setRepetitionpattern("once");
			newBooking.setRepetitionenddate("");
			*/
			newBooking.setTimezone(viteroModule.getTimeZoneId());
			
			createRequest.setBooking(newBooking);

			CreateBookingResponse response = bookingWs.createBooking(createRequest);
			Boolean bookingCollision = response.isBookingcollision();
			Boolean moduleCollision = response.isModulecollision();
			int bookingId = response.getBookingid();
			
			if(bookingCollision != null && bookingCollision.booleanValue()) {
				return new ViteroStatus(ErrorCode.bookingCollision);
			} else if(moduleCollision != null && moduleCollision.booleanValue()) {
				return new ViteroStatus(ErrorCode.moduleCollision);
			}
			vBooking.setBookingId(bookingId);
			getOrCreateProperty(group, ores, subIdentifier, vBooking);
			return new ViteroStatus();
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				case invalidTimezone: logError("Invalid time zone!", f); break;
				case bookingCollision: logError("Booking collision!", f); break;
				case moduleCollision: logError("Invalid module selection!", f); break;
				case bookingInPast: logError("Booking in the past!", f); break;
				case licenseExpired: logError("License/customer expired!", f); break;
				default: logAxisError("Cannot create a booking.", f);
			}
			return new ViteroStatus(code);
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot create a booking.", e);
			return new ViteroStatus(ErrorCode.remoteException);
		}
	}
	
	/**
	 * There is not update on vms. We can only update some OLAT specific options.
	 * @param group
	 * @param ores
	 * @param vBooking
	 * @return
	 * @throws VmsNotAvailableException
	 */
	public ViteroBooking updateBooking(BusinessGroup group, OLATResourceable ores, String subIdentifier, ViteroBooking vBooking)
	throws VmsNotAvailableException {
		Bookingtype bookingType = getBookingById(vBooking.getBookingId());
		if(bookingType == null) {
			logInfo("Booking doesn't exist: " + vBooking.getBookingId());
			return null;
		}
		
		Booking_Type booking = bookingType.getBooking();
		//set the vms values
		update(vBooking, booking);
		//update the property
		updateProperty(group, ores, subIdentifier, vBooking);
		return vBooking;
	}
	
	public boolean deleteBooking(ViteroBooking vBooking)
	throws VmsNotAvailableException {
		try {
			DeleteBookingRequest deleteRequest = new DeleteBookingRequest();
			deleteRequest.setBookingid(vBooking.getBookingId());

			DeleteBookingResponse response = getBookingWebService().deleteBooking(deleteRequest);
			BigInteger state = response.getDeletestate();
			deleteGroup(vBooking);
			deleteProperty(vBooking);
			return state != null;
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				case bookingDoesntExist:
				case bookingDoesntExistPrime: {
					deleteGroup(vBooking);
					deleteProperty(vBooking);
					return true;//ok, vms deleted, group deleted...
				}
				default: {
					logAxisError("Cannot delete a booking.", f);
				}
			}
			return false;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot delete a booking.", e);
			return false;
		}
	}
	
	public void deleteAll(BusinessGroup group, OLATResourceable ores, String subIdentifier) {
		try {
			List<Property> properties = propertyManager.listProperties(null, group, ores, VMS_CATEGORY, null);
			for(Property property:properties) {
				String bookingStr = property.getTextValue();
				ViteroBooking booking = deserializeViteroBooking(bookingStr);
				deleteBooking(booking);
			}
		} catch (VmsNotAvailableException e) {
			logError("", e);
			markAsZombie(group, ores, subIdentifier);
		}
	}
	
	private final void markAsZombie(BusinessGroup group, OLATResourceable ores, String subIdentifier) {
		List<Property> properties = propertyManager.listProperties(null, group, ores, VMS_CATEGORY, null);
		for(Property property:properties) {
			String propIdentifier = property.getStringValue();
			if((subIdentifier == null && propIdentifier == null)
					|| (subIdentifier != null && subIdentifier.equals(propIdentifier))) {
				property.setName(VMS_CATEGORY_ZOMBIE);
				propertyManager.updateProperty(property);
			}
		}
	}
	
	public void slayZombies() {
		List<Property> properties = propertyManager.listProperties(null, null, null, VMS_CATEGORY_ZOMBIE, null);
		for(Property property:properties) {
			try {
				String bookingStr = property.getTextValue();
				ViteroBooking booking = deserializeViteroBooking(bookingStr);
				deleteBooking(booking);
			} catch (VmsNotAvailableException e) {
				//try later
				logDebug("Cannot clean-up vitero room, vms not available");
			} catch (Exception e) {
				logError("", e);
			}
		}
	}
	
	public List<ViteroBooking> getBookingInFutures(Identity identity)
	throws VmsNotAvailableException {
		GetUserInfo userInfo = getVmsUserId(identity, false);
		int userId = userInfo.getUserId();
		if(userId > 0) {
			List<Booking_Type> bookings = getBookingInFutureByUserId(userId);
			return convert(bookings);
		}
		return Collections.emptyList();
	}
	
	/**
	 * Return the 
	 * @param group The group (optional)
	 * @param ores The OLAT resourceable (of the course) (optional)
	 * @return
	 */
	public List<ViteroBooking> getBookings(BusinessGroup group, OLATResourceable ores, String subIdentifier)
	throws VmsNotAvailableException {
		List<Property> properties = propertyManager.listProperties(null, group, ores, VMS_CATEGORY, null);
		List<ViteroBooking> bookings = new ArrayList<ViteroBooking>();
		for(Property property:properties) {
			String propIdentifier = property.getStringValue();
			if((propIdentifier == null || subIdentifier == null)
					|| (subIdentifier != null
						&& (propIdentifier == null || subIdentifier.equals(propIdentifier))
					)) {
				String bookingStr = property.getTextValue();
				ViteroBooking booking = deserializeViteroBooking(bookingStr);
				Bookingtype bookingType = getBookingById(booking.getBookingId());
				if(bookingType != null) {
					Booking_Type vmsBooking = bookingType.getBooking();
					booking.setProperty(property);
					update(booking, vmsBooking);
					bookings.add(booking);
				}
			}
		}
		return bookings;
	}
	
	protected List<Booking_Type> getBookingInFutureByUserId(int userId)
	throws VmsNotAvailableException {
		try {
			GetBookingListByUserInFutureRequest request = new GetBookingListByUserInFutureRequest();
			request.setUserid(userId);
			request.setTimezone(viteroModule.getTimeZoneId());
			
			Bookinglist bookingList = getBookingWebService().getBookingListByUserInFuture(request);
			
			return bookingList.getBooking();
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				case userDoesntExist: logError("The user does not exist!", f); break;
				case invalidAttribut: logError("ids <= 0!", f); break;
				case invalidTimezone: logError("Invalid time zone!", f); break;
				default: logAxisError("Cannot get booking in future for user: " + userId, f);
			}
			return null;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot get booking in future for custom: " + userId, e);
			return null;
		}
	}

	protected Bookingtype getBookingById(int id)
	throws VmsNotAvailableException {
		if(id < 0) return null;
		
		try {
			Bookingid bookingId = new Bookingid();
			bookingId.setBookingid(id);
			Bookingtype booking = getBookingWebService().getBookingById(bookingId);
			return booking;
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				case invalidAttribut: logError("ids <= 0", f); break;
				case bookingDoesntExist: logError("The booking does not exist", f); break;
				default: logAxisError("Cannot get booking by id: " + id, f);
			}
			return null;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logError("Cannot get booking by id: " + id, e);
			return null;
		}
	}
	
	public CheckUserInfo checkUsers() throws VmsNotAvailableException {
		final String[] authProviders = new String[]{ VMS_PROVIDER };
		final String prefix = getVmsUsernamePrefix();
		
		int authenticationCreated = 0;
		int authenticationDeleted = 0;
		
		//check if vms user with an openolat login exists on vms server
		//without the need authentication object in openolat.
		List<Usertype> users = getCustomersUsers();
		if(users != null && users.size() > 0) {
			for(Usertype user:users) {
				String vmsUsername = user.getUsername();
				if(vmsUsername.startsWith(prefix)) {
					String olatUsername = vmsUsername.substring(prefix.length(), vmsUsername.length());
					List<Identity> identities = securityManager.getIdentitiesByPowerSearch(olatUsername, null, false, null, null, authProviders, null, null, null, null, null);
					if(identities.isEmpty()) {
						Identity identity = securityManager.findIdentityByName(olatUsername);
						if(identity != null) {
							authenticationCreated++;
							securityManager.createAndPersistAuthentication(identity, VMS_PROVIDER, Integer.toString(user.getId()), null, null);
							logInfo("Recreate VMS authentication for: " + identity.getName());
						}
					}
				}	
			}
		}
		
		//check if all openolat users with a vms authentication have an user
		//on the vms server
		List<Identity> identities = securityManager.getIdentitiesByPowerSearch(null, null, false, null, null, authProviders, null, null, null, null, null);
		for(Identity identity :identities) {
			Authentication authentication = securityManager.findAuthentication(identity, VMS_PROVIDER);
			String vmsUserId = authentication.getAuthusername();
			
			boolean foundIt = false;
			for(Usertype user:users) {
				if(vmsUserId.equals(Integer.toString(user.getId()))) {
					foundIt = true;
				}
			}
			
			if(!foundIt) {
				securityManager.deleteAuthentication(authentication);
				authenticationDeleted++;
			}
		}
		
		CheckUserInfo infos = new CheckUserInfo();
		infos.setAuthenticationCreated(authenticationCreated);
		infos.setAuthenticationDeleted(authenticationDeleted);
		return infos;
	}
	
	private String getVmsUsernamePrefix() {
		return "olat." + WebappHelper.getInstanceId() + ".";
	}
	
	public boolean checkConnection() {
		try {
			return checkConnection(viteroModule.getVmsURI().toString(), viteroModule.getAdminLogin(), 
					viteroModule.getAdminPassword(), viteroModule.getCustomerId());
		} catch (VmsNotAvailableException e) {
			return false;
		}
	}
	
	public boolean checkConnection(final String url, final String login, final String password, final int customerId)
	throws VmsNotAvailableException {
		try {
			LicenceService ss = new LicenceService();
	        ss.setHandlerResolver(new HandlerResolver() {
						@SuppressWarnings("rawtypes")
						@Override
						public List<Handler> getHandlerChain(PortInfo portInfo) {
							List<Handler> handlerList = new ArrayList<Handler>();
							handlerList.add(new ViteroSecurityHandler(login, password));
							return handlerList;
						}
	        	
	        });
	        Licence port = ss.getLicenceSoap11();
	        String endPoint = UriBuilder.fromUri(url).path("services").path("LicenseService").build().toString();
	        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);
	        GetModulesForCustomerRequest request = new GetModulesForCustomerRequest();
	        request.setCustomerid(customerId);
	        Modulestype modulesType = port.getModulesForCustomer(request);
			return modulesType != null;
		} catch(SOAPFaultException f) {
			ErrorCode code = handleAxisFault(f);
			switch(code) {
				case unsufficientRights: logError("Unsufficient rights", f); break;
				default: logAxisError("Cannot check connection", f);
			}
			return false;
		} catch (WebServiceException e) {
			if(e.getCause() instanceof ConnectException) {
				throw new VmsNotAvailableException();
			}
			logWarn("Error checking connection", e);
			return false;
		}
	}
	
	//Utilities
	private final ErrorCode handleAxisFault(final SOAPFaultException f) 
	throws VmsNotAvailableException {
		if(f.getFault() != null) {
			String errorCode = extractErrorCode(f.getFault());
			if(StringHelper.isLong(errorCode)) {
				int code = Integer.parseInt(errorCode);
				return ErrorCode.find(code);
			}
			return ErrorCode.unkown;
		} else if (f.getCause() instanceof ConnectTimeoutException) {
			throw new VmsNotAvailableException(f);
		}
		return ErrorCode.unkown;
	}
	
	private String extractErrorCode(SOAPElement element) {
		if(element == null) return null;
		
		for(Iterator<?> it=element.getChildElements(); it.hasNext(); ) {
			Object childElement = it.next();
			if(childElement instanceof SOAPElement) {
				SOAPElement soapElement = (SOAPElement)childElement;
				String nodeName = soapElement.getNodeName();
				if("errorCode".equals(nodeName)) {
					return extractText(soapElement);
				} else {
					extractErrorCode(soapElement);
				}
			}
		}
		return null;
	}
	
	private String extractText(SOAPElement errorCodeEl) {
		for(Node node=errorCodeEl.getFirstChild(); node != null; node=node.getNextSibling()) {
			if(node instanceof Text && StringHelper.containsNonWhitespace(node.getNodeValue())) {
				return node.getNodeValue();
			}
		}
		return null;
	}
	
	private void logAxisError(String message, SOAPFaultException f) {
		StringBuilder sb = new StringBuilder();
		if(StringHelper.containsNonWhitespace(message)) {
			sb.append(message);
		}
		
		if(f.getMessage() != null) {
			if(sb.length() > 0) sb.append(" -> ");
			sb.append(f.getMessage().toString());
		}
		
		if(StringHelper.containsNonWhitespace(f.getMessage())) {
			if(sb.length() > 0) sb.append(" -> ");
			sb.append(f.getMessage());
		}

		logError(sb.toString(), f);
	}
	
	private final List<ViteroBooking> convert(List<Booking_Type> bookings) {
		List<ViteroBooking> viteroBookings = new ArrayList<ViteroBooking>();
		
		if(bookings != null && bookings.size() > 0) {
			for(Booking_Type b:bookings) {
				viteroBookings.add(convert(b));
			}
		}

		return viteroBookings;
	}
	
	private final ViteroBooking convert(Booking_Type booking) {
		ViteroBooking vb = new ViteroBooking();
		return update(vb, booking);
	}
	
	private final ViteroBooking update(ViteroBooking vb, Booking_Type booking) {
		vb.setBookingId(booking.getBookingid());
		vb.setGroupId(booking.getGroupid());
		vb.setRoomSize(booking.getRoomsize());
		vb.setStart(parse(booking.getStart()));
		vb.setStartBuffer(booking.getStartbuffer());
		vb.setEnd(parse(booking.getEnd()));
		vb.setEndBuffer(booking.getEndbuffer());
		return vb;
	}
	
	private final ViteroGroup convert(Completegrouptype groupType) {
		ViteroGroup vg = new ViteroGroup();
		vg.setGroupId(groupType.getId());
		vg.setName(groupType.getName());
		int numOfParticipants = groupType.getParticipant() == null ? 0 : groupType.getParticipant().size();
		vg.setNumOfParticipants(numOfParticipants);
		return vg;
	}
	
	private final List<ViteroUser> convertUsertype(List<Usertype> userTypes) {
		List<ViteroUser> vUsers = new ArrayList<ViteroUser>();
		if(userTypes != null) {
			for(Usertype userType:userTypes) {
				vUsers.add(convert(userType));
			}
		}
		return vUsers;
	}
	
	private final ViteroUser convert(Usertype userType) {
		ViteroUser vu = new ViteroUser();
		vu.setUserId(userType.getId());
		vu.setFirstName(userType.getFirstname());
		vu.setLastName(userType.getSurname());
		vu.setEmail(userType.getEmail());
		return vu;
	}
	
	//Properties
	private final Property getProperty(BusinessGroup group, OLATResourceable courseResource, ViteroBooking booking) {
		String propertyName = Integer.toString(booking.getBookingId());
		return propertyManager.findProperty(null, group, courseResource, VMS_CATEGORY, propertyName);
	}
	
	private final Property getOrCreateProperty(BusinessGroup group, OLATResourceable courseResource, String subIdentifier, ViteroBooking booking) {
		Property property = getProperty(group, courseResource, booking);
		if(property == null) {
			property = createProperty(group, courseResource, subIdentifier, booking);
			propertyManager.saveProperty(property);
		}
		return property;
	}
	
	private final Property updateProperty(BusinessGroup group, OLATResourceable courseResource, String subIdentifier, ViteroBooking booking) {
		Property property = getProperty(group, courseResource, booking);
		if(property == null) {
			property = createProperty(group, courseResource, subIdentifier, booking);
			propertyManager.saveProperty(property);
		} else {
			String serialized = serializeViteroBooking(booking);
			property.setTextValue(serialized);
			propertyManager.updateProperty(property);
		}
		return property;
	}
	
	private final Property createProperty(final BusinessGroup group, final OLATResourceable courseResource, String subIdentifier, ViteroBooking booking) {
		String serialized = serializeViteroBooking(booking);
		String bookingId = Integer.toString(booking.getBookingId());
		Long groupId = new Long(booking.getGroupId());
		return propertyManager.createPropertyInstance(null, group, courseResource, VMS_CATEGORY, bookingId, null, groupId, subIdentifier, serialized);
	}
	
	private final void deleteProperty(ViteroBooking vBooking) {
		String bookingId = Integer.toString(vBooking.getBookingId());
		propertyManager.deleteProperties(null, null, null, VMS_CATEGORY, bookingId);
	}
	
	private final String serializeViteroBooking(ViteroBooking booking) {
		StringWriter writer = new StringWriter();
		xStream.marshal(booking, new CompactWriter(writer));
		writer.flush();
		return writer.toString();
	}
	
	private final ViteroBooking deserializeViteroBooking(String booking) {
		return (ViteroBooking)xStream.fromXML(booking);
	}
	
	//Factories for service stubs
	private final  Booking getBookingWebService() {
		BookingService ss = new BookingService();
        ss.setHandlerResolver(new VmsSecurityHandlerResolver());
        Booking port = ss.getBookingSoap11();
        String endPoint = getVmsEndPoint("BookingService");
        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);
		return port;
	}
	
	private final Licence getLicenceWebService() {
		LicenceService ss = new LicenceService();
        ss.setHandlerResolver(new VmsSecurityHandlerResolver());
        Licence port = ss.getLicenceSoap11();
        String endPoint = getVmsEndPoint("LicenceService");
        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);
		return port;
	}
	
	private final Group getGroupWebService() {
		GroupService ss = new GroupService();
        ss.setHandlerResolver(new VmsSecurityHandlerResolver());
        Group port = ss.getGroupSoap11();
        String endPoint = getVmsEndPoint("GroupService");
        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);
		return port;
	}
	
	private final de.vitero.schema.user.User getUserWebService() {
		UserService ss = new UserService();
        ss.setHandlerResolver(new VmsSecurityHandlerResolver());
        de.vitero.schema.user.User port = ss.getUserSoap11();
        String endPoint = getVmsEndPoint("UserService");
        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);
		return port;
	}
	
	private final Mtom getMtomWebService() {
		MtomService ss = new MtomService();
        ss.setHandlerResolver(new VmsSecurityHandlerResolver());
        Mtom port = ss.getMtomSoap11();
        String endPoint = getVmsEndPoint("MtomService");
        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);
		return port;
	}
	
	private final SessionCode getSessionCodeWebService() {
		SessionCodeService ss = new SessionCodeService();
        ss.setHandlerResolver(new VmsSecurityHandlerResolver());
        SessionCode port = ss.getSessionCodeSoap11();
        String endPoint = getVmsEndPoint("SessionCodeService");
        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);
		return port;
	}

	private final String getVmsEndPoint(String service) {
	    UriBuilder builder = UriBuilder.fromUri(viteroModule.getVmsURI());
	    builder.path("services").path(service);
	    return builder.build().toString();
	}
	
	private final String getStartPoint(String sessionCode) {
		UriBuilder builder = UriBuilder.fromUri(viteroModule.getVmsURI() );
	    builder.path("start.htm");
	    if(StringHelper.containsNonWhitespace(sessionCode)) {
	    	builder.queryParam("sessionCode", sessionCode);
	    }
	    return builder.build().toString();
	}
	
	private final String getGroupURL(String sessionCode, int groupId) {
		UriBuilder builder = UriBuilder.fromUri(viteroModule.getVmsURI() );
	    builder.path("/user/cms/groupfolder.htm");
	    builder.queryParam("code", sessionCode);
    	builder.queryParam("fl", "1");
    	builder.queryParam("groupId", Integer.toString(groupId));
	    return builder.build().toString();
	}
	
	private final synchronized String format(Date date) {
		return dateFormat.format(date);
	}
	
	private final synchronized Date parse(String dateString) {
		try {
			return dateFormat.parse(dateString);
		} catch (ParseException e) {
			logError("Cannot parse a date: " + dateString, e);
			return null;
		}
	}
	
	private final class VmsSecurityHandlerResolver implements HandlerResolver {
		
		@SuppressWarnings("rawtypes")
		@Override
		public List<Handler> getHandlerChain(PortInfo portInfo) {
			List<Handler> handlerList = new ArrayList<Handler>();
			handlerList.add(new ViteroSecurityHandler(viteroModule.getAdminLogin(), viteroModule.getAdminPassword()));
			return handlerList;
		}
	}
}
