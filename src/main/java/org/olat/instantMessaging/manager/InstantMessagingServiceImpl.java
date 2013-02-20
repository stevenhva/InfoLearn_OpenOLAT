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
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.instantMessaging.manager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.IdentityShort;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.StringHelper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.session.UserSessionManager;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupMemberView;
import org.olat.group.BusinessGroupService;
import org.olat.group.DeletableGroupData;
import org.olat.group.manager.ContactDAO;
import org.olat.group.model.BusinessGroupOwnerViewImpl;
import org.olat.group.model.BusinessGroupParticipantViewImpl;
import org.olat.instantMessaging.ImPreferences;
import org.olat.instantMessaging.InstantMessage;
import org.olat.instantMessaging.InstantMessageNotification;
import org.olat.instantMessaging.InstantMessagingEvent;
import org.olat.instantMessaging.InstantMessagingService;
import org.olat.instantMessaging.model.Buddy;
import org.olat.instantMessaging.model.BuddyGroup;
import org.olat.instantMessaging.model.BuddyStats;
import org.olat.instantMessaging.model.InstantMessageImpl;
import org.olat.instantMessaging.model.Presence;
import org.olat.instantMessaging.model.RosterEntryView;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * Initial date: 05.12.2012<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class InstantMessagingServiceImpl extends BasicManager implements InstantMessagingService,
	ApplicationListener<ContextRefreshedEvent>, DeletableGroupData {
	
	@Autowired
	private RosterDAO rosterDao;
	@Autowired
	private InstantMessageDAO imDao;
	@Autowired
	private InstantMessagePreferencesDAO prefsDao;
	@Autowired
	private ChatLogHelper logHelper;
	@Autowired
	private CoordinatorManager coordinator;
	@Autowired
	private ContactDAO contactDao;
	@Autowired
	private UserManager userManager;
	@Autowired
	private UserSessionManager sessionManager;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private BusinessGroupService businessGroupService;
	@Autowired
	private DB dbInstance;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		//rosterDao.clear();
		businessGroupService.registerDeletableGroupDataListener(this);
	}

	@Override
	@Transactional
	public boolean deleteGroupDataFor(BusinessGroup group) {
		imDao.deleteMessages(group);
		return true;
	}

	@Override
	public String getStatus(Long identityKey) {
		return prefsDao.getStatus(identityKey);
	}

	@Override
	@Transactional
	public ImPreferences getImPreferences(Identity identity) {
		return prefsDao.getPreferences(identity);
	}

	@Override
	@Transactional
	public void updateImPreferences(Identity identity, boolean visible) {
		prefsDao.updatePreferences(identity, visible);
	}

	@Override
	@Transactional
	public void updateStatus(Identity identity, String status) {
		prefsDao.updatePreferences(identity, status);
	}

	@Override
	public OLATResourceable getPrivateChatResource(Long identityKey1, Long identityKey2) {
		String resName;
		if(identityKey1.longValue() > identityKey2.longValue()) {
			resName = identityKey2 + "-" + identityKey1;
		} else {
			resName = identityKey1 + "-" + identityKey2;
		}
		long key = identityKey1.longValue() + identityKey2.longValue();
		return OresHelper.createOLATResourceableInstance(resName, new Long(key));
	}

	@Override
	public InstantMessage getMessageById(Identity identity, Long messageId, boolean markedAsRead) {
		InstantMessageImpl msg = imDao.loadMessageById(messageId);
		if(markedAsRead && msg != null) {
			OLATResourceable ores = OresHelper.createOLATResourceableInstance(msg.getResourceTypeName(), msg.getResourceId());
			imDao.deleteNotification(identity, ores);
		}
		return msg;
	}

	@Override
	public List<InstantMessage> getMessages(Identity identity, OLATResourceable chatResource,
			Date from, int firstResult, int maxResults, boolean markedAsRead) {
		List<InstantMessage> msgs = imDao.getMessages(chatResource, from, firstResult, maxResults);
		if(markedAsRead) {
			imDao.deleteNotification(identity, chatResource);
		}
		return msgs;
	}

	@Override
	public InstantMessage sendMessage(Identity from, String fromNickName, boolean anonym, String body, OLATResourceable chatResource) {
		InstantMessage message = imDao.createMessage(from, fromNickName, anonym, body, chatResource);
		dbInstance.commit();//commit before sending event
		
		InstantMessagingEvent event = new InstantMessagingEvent("message", chatResource);
		event.setFromId(from.getKey());
		event.setName(fromNickName);
		event.setAnonym(anonym);
		event.setMessageId(message.getKey());
		coordinator.getCoordinator().getEventBus().fireEventToListenersOf(event, chatResource);
		return message;
	}
	
	@Override
	public InstantMessage sendPrivateMessage(Identity from, Long toIdentityKey, String body, OLATResourceable chatResource) {
		String name = userManager.getUserDisplayName(from.getUser());
		InstantMessage message = imDao.createMessage(from, name, false, body, chatResource);
		imDao.createNotification(from.getKey(), toIdentityKey, chatResource);
		dbInstance.commit();//commit before sending event
		
		InstantMessagingEvent event = new InstantMessagingEvent("message", chatResource);
		event.setFromId(from.getKey());
		event.setName(name);
		event.setAnonym(false);
		event.setMessageId(message.getKey());
		//general event
		coordinator.getCoordinator().getEventBus().fireEventToListenersOf(event, chatResource);
		//buddy event
		OLATResourceable buddy = OresHelper.createOLATResourceableInstance("Buddy", toIdentityKey);
		coordinator.getCoordinator().getEventBus().fireEventToListenersOf(event, buddy);
		return message;
	}

	@Override
	@Transactional
	public void deleteMessages(OLATResourceable ores) {
		imDao.deleteMessages(ores);
	}

	@Override
	public void sendPresence(Identity me, String nickName, boolean anonym, boolean vip, OLATResourceable chatResource) {
		InstantMessagingEvent event = new InstantMessagingEvent("participant", chatResource);
		event.setAnonym(anonym);
		event.setVip(vip);
		event.setFromId(me.getKey());
		if(StringHelper.containsNonWhitespace(nickName)) {
			event.setName(nickName);
		}
		String fullName = userManager.getUserDisplayName(me.getUser());
		rosterDao.updateRosterEntry(chatResource, me, fullName, nickName, anonym, vip);
		dbInstance.commit();//commit before sending event
		
		coordinator.getCoordinator().getEventBus().fireEventToListenersOf(event, chatResource);
	}
	
	@Override
	public List<InstantMessageNotification> getNotifications(Identity identity) {
		return imDao.getNotifications(identity);
	}

	@Override
	public Buddy getBuddyById(Long identityKey) {
		IdentityShort identity = securityManager.loadIdentityShortByKey(identityKey);
		String fullname = userManager.getUserDisplayName(identity);
		String status = getOnlineStatus(identityKey);
		return new Buddy(identity.getKey(), identity.getName(), fullname, false, status);
	}
	

	@Override
	public BuddyStats getBuddyStats(Identity me) {
		BuddyStats stats = new BuddyStats();
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -1);
		//count all my buddies
		Collection<Long> buddiesColl = contactDao.getDistinctGroupOwnersParticipants(me);
		List<Long> buddies = new ArrayList<Long>(buddiesColl);
		buddies.remove(me.getKey());
		stats.setOfflineBuddies(buddies.size());

		//filter online users
		for(Iterator<Long> buddyIt=buddies.iterator(); buddyIt.hasNext(); ) {
			Long buddyKey = buddyIt.next();
			boolean online = isOnline(buddyKey);
			if(!online) {
				buddyIt.remove();
			}
		}
		
		//count online users which are available
		int online = prefsDao.countAvailableBuddies(buddies);
		stats.setOnlineBuddies(online);
		return stats;
	}

	@Override
	public List<BuddyGroup> getBuddyGroups(Identity me, boolean offlineUsers) {
		List<BuddyGroup> groups = new ArrayList<BuddyGroup>(25);
		Map<Long,BuddyGroup> groupMap = new HashMap<Long,BuddyGroup>();
		Map<Long, String> identityKeyToStatus = new HashMap<Long, String>();
		List<BusinessGroupOwnerViewImpl> ownerList = contactDao.getGroupOwners(me);
		collectMembersStatus(ownerList, identityKeyToStatus);
		List<BusinessGroupParticipantViewImpl> participantList = contactDao.getParticipants(me);
		collectMembersStatus(participantList, identityKeyToStatus);
		for(BusinessGroupOwnerViewImpl owner:ownerList) {
			addBuddyToGroupList(owner, me, groupMap, groups, identityKeyToStatus, true, offlineUsers);
		}
		for(BusinessGroupParticipantViewImpl participant:participantList) {
			addBuddyToGroupList(participant, me, groupMap, groups, identityKeyToStatus, false, offlineUsers);
		}
		
		Map<Long,String> nameMap = userManager.getUserDisplayNames(identityKeyToStatus.keySet());
		for(BuddyGroup group:groups) {
			for(Buddy buddy:group.getBuddy()) {
				buddy.setName(nameMap.get(buddy.getIdentityKey()));	
			}
		}
		return groups;
	}
	
	private void collectMembersStatus(List<? extends BusinessGroupMemberView> members, Map<Long, String> identityKeyToStatus) {
		Set<Long> loadStatus = new HashSet<Long>();
		for(BusinessGroupMemberView member:members) {
			Long identityKey = member.getIdentityKey();
			if(!identityKeyToStatus.containsKey(identityKey) && !loadStatus.contains(identityKey)) {
				boolean online = isOnline(member.getIdentityKey());
				if(online) {
					loadStatus.add(identityKey);
				} else {
					identityKeyToStatus.put(identityKey, Presence.unavailable.name());
				}
			}
		}
		
		if(loadStatus.size() > 0) {
			List<Long> statusToLoadList = new ArrayList<Long>(loadStatus);
			Map<Long,String> statusMap = prefsDao.getBuddyStatus(statusToLoadList);
			for(Long toLoad:statusToLoadList) {
				String status = statusMap.get(toLoad);
				if(status == null) {
					identityKeyToStatus.put(toLoad, Presence.available.name());	
				} else {
					identityKeyToStatus.put(toLoad, status);	
				}
			}
		}	
	}
	
	private void addBuddyToGroupList(BusinessGroupMemberView member, Identity me, Map<Long,BuddyGroup> groupMap,
			List<BuddyGroup> groups, Map<Long, String> identityKeyToStatus, boolean vip, boolean offlineUsers) {
		if(me != null && me.getKey().equals(member.getIdentityKey())) {
			return;
		}
		String status = identityKeyToStatus.get(member.getIdentityKey());
		if(status == null) {
			boolean online = isOnline(member.getIdentityKey());
			if(online) {
				status = prefsDao.getStatus(member.getIdentityKey());
				if(status == null) {
					status = Presence.available.name();
				}
			} else {
				status = Presence.unavailable.name();
			}
			identityKeyToStatus.put(member.getIdentityKey(), status);
		}
		
		if(offlineUsers || Presence.available.name().equals(status)) {
			BuddyGroup group = groupMap.get(member.getGroupKey());
			if(group == null) {
				group = new BuddyGroup(member.getGroupKey(), member.getGroupName());
				groupMap.put(member.getGroupKey(), group);
				groups.add(group);
			}
			group.addBuddy(new Buddy(member.getIdentityKey(), member.getUsername(), null, false, vip, status));	
		}
	}

	@Override
	public List<Buddy> getBuddiesListenTo(OLATResourceable chatResource) {
		List<RosterEntryView> roster = rosterDao.getRosterView(chatResource, 0, -1);
		List<Buddy> buddies = new ArrayList<Buddy>();
		if(roster != null) {
			for(RosterEntryView entry:roster) {
				String name = entry.isAnonym() ? entry.getNickName() : entry.getFullName();
				String status = getOnlineStatus(entry.getIdentityKey());
				buddies.add(new Buddy(entry.getIdentityKey(), entry.getUsername(), name, entry.isAnonym(), entry.isVip(), status));
			}
		}
		return buddies;
	}
	
	private String getOnlineStatus(Long identityKey) {
		return isOnline(identityKey) ? Presence.available.name() : Presence.unavailable.name();
	}
	
	/**
	 * Return true if the identity is logged in on the instance
	 * @param identityKey
	 * @return
	 */
	private boolean isOnline(Long identityKey) {
		return sessionManager.isOnline(identityKey);
	}

	@Override
	@Transactional
	public void listenChat(Identity identity, OLATResourceable chatResource,
			boolean anonym, boolean vip, GenericEventListener listener) {
		String fullName = userManager.getUserDisplayName(identity.getUser());
		rosterDao.updateRosterEntry(chatResource, identity, fullName, null, anonym, vip);
		coordinator.getCoordinator().getEventBus().registerFor(listener, identity, chatResource);
	}

	@Override
	@Transactional
	public void unlistenChat(Identity identity, OLATResourceable chatResource, GenericEventListener listener) {
		rosterDao.deleteEntry(identity, chatResource);
		coordinator.getCoordinator().getEventBus().deregisterFor(listener, chatResource);
	}

	@Override
	public void disableChat(Identity identity) {
		//
	}

	@Override
	public void enableChat(Identity identity) {
		//
	}
}