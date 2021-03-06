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

package org.olat.core.commons.services.notifications.manager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;

import org.hibernate.FlushMode;
import org.olat.ControllerFactory;
import org.olat.basesecurity.BaseSecurity;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.commons.persistence.PersistenceHelper;
import org.olat.core.commons.services.notifications.NotificationHelper;
import org.olat.core.commons.services.notifications.NotificationsHandler;
import org.olat.core.commons.services.notifications.NotificationsManager;
import org.olat.core.commons.services.notifications.Publisher;
import org.olat.core.commons.services.notifications.PublisherData;
import org.olat.core.commons.services.notifications.Subscriber;
import org.olat.core.commons.services.notifications.SubscriptionContext;
import org.olat.core.commons.services.notifications.SubscriptionInfo;
import org.olat.core.commons.services.notifications.SubscriptionItem;
import org.olat.core.commons.services.notifications.model.NoSubscriptionInfo;
import org.olat.core.commons.services.notifications.model.PublisherImpl;
import org.olat.core.commons.services.notifications.model.SubscriberImpl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.WorkThreadInformations;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.SyncerCallback;
import org.olat.core.util.event.EventFactory;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.event.MultiUserEvent;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.mail.MailBundle;
import org.olat.core.util.mail.MailManager;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.resource.OresHelper;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;
import org.olat.user.UserDataDeletable;

/**
 * Description: <br>
 * see org.olat.core.commons.services.notifications.NotificationsManager
 * 
 * Initial Date: 21.10.2004 <br>
 * @author Felix Jost
 */
public class NotificationsManagerImpl extends NotificationsManager implements UserDataDeletable {
	private static final OLog log = Tracing.createLoggerFor(NotificationsManagerImpl.class);

	private static final int PUB_STATE_OK = 0;
	private static final int PUB_STATE_NOT_OK = 1;
	private static final int BATCH_SIZE = 100;
	private static final String LATEST_EMAIL_USER_PROP = "noti_latest_email";
	private final SubscriptionInfo NOSUBSINFO = new NoSubscriptionInfo();

	private final OLATResourceable oresMyself = OresHelper.lookupType(NotificationsManagerImpl.class);

	private Map<String, NotificationsHandler> notificationHandlers;
	
	private List<String> notificationIntervals;
	private String defaultNotificationInterval;
	private static final Map<String, Integer> INTERVAL_DEF_MAP = buildIntervalMap();
	private Object lockObject = new Object();
	
	private DB dbInstance;
	private BaseSecurity securityManager;
	private PropertyManager propertyManager;
	
	/**
	 * [used by spring]
	 * @param userDeletionManager
	 */
	private NotificationsManagerImpl() {
		// private since singleton
		INSTANCE = this;
	}

	/**
	 * [used by Spring]
	 * @param dbInstance
	 */
	public void setDbInstance(DB dbInstance) {
		this.dbInstance = dbInstance;
	}
	
	/**
	 * [user by Spring]
	 * @param securityManager
	 */
	public void setSecurityManager(BaseSecurity securityManager) {
		this.securityManager = securityManager;
	}
	
	/**
	 * [used by Spring]
	 * @param propertyManager
	 */
	public void setPropertyManager(PropertyManager propertyManager) {
		this.propertyManager = propertyManager;
	}

	/**
	 * @param resName
	 * @param resId
	 * @param subidentifier
	 * @param type
	 * @param data
	 * @return a persisted publisher with ores/subidentifier as the composite
	 *         primary key
	 */
	private Publisher createAndPersistPublisher(String resName, Long resId, String subidentifier, String type, String data, String businessPath) {
		if (resName == null || resId == null || subidentifier == null) throw new AssertException(
				"resName, resId, and subidentifier must not be null");
		
		if(businessPath != null && businessPath.length() > 230) {
			log.error("Businesspath too long for publisher: " + resName + " with business path: " + businessPath);
			businessPath = businessPath.substring(0, 230);
		}
		PublisherImpl pi = new PublisherImpl(resName, resId, subidentifier, type, data, businessPath, new Date(), PUB_STATE_OK);
		pi.setCreationDate(new Date());
		dbInstance.getCurrentEntityManager().persist(pi);
		return pi;
	}

	/**
	 * @param persistedPublisher
	 * @param listener
	 * @param subscriptionContext the context of the object we subscribe to
	 * @return a subscriber with a db key
	 */
	protected Subscriber doCreateAndPersistSubscriber(Publisher persistedPublisher, Identity listener) {
		SubscriberImpl si = new SubscriberImpl(persistedPublisher, listener);
		si.setCreationDate(new Date());
		si.setLastModified(new Date());
		si.setLatestEmailed(new Date());
		dbInstance.getCurrentEntityManager().persist(si);
		return si;
	}

	/**
	 * subscribers for ONE person (e.g. subscribed to 5 forums -> 5 subscribers
	 * belonging to this person)
	 * 
	 * @param identity
	 * @return List of Subscriber Objects which belong to the identity
	 */
	public List<Subscriber> getSubscribers(Identity identity) {
		return getSubscribers(identity, Collections.<String>emptyList());
	}

	/**
	 * subscribers for ONE person (e.g. subscribed to 5 forums -> 5 subscribers
	 * belonging to this person) restricted to the specified types
	 * 
	 * @param identity
	 * @return List of Subscriber Objects which belong to the identity
	 */
	@Override
	public List<Subscriber> getSubscribers(Identity identity, List<String> types) {
		StringBuilder sb = new StringBuilder();
		sb.append("select sub from notisub as sub ")
		  .append("inner join fetch sub.publisher as publisher ")
		  .append("where sub.identity = :anIdentity");
		if(types != null && !types.isEmpty()) {
			sb.append(" and publisher.type in (:types)");
		}
		TypedQuery<Subscriber> query = dbInstance.getCurrentEntityManager().createQuery(sb.toString(), Subscriber.class);
		query.setParameter("anIdentity", identity);
		if(types != null && !types.isEmpty()) {
			query.setParameter("types", types);
		}
		return query.getResultList();
	}

	/**
	 * @param identity
	 * @return a list of all subscribers which belong to the identity and which
	 *         publishers are valid
	 */
	@Override
	public List<Subscriber> getValidSubscribers(Identity identity) {
		StringBuilder q = new StringBuilder();
		q.append("select sub from notisub sub ")
		 .append(" inner join fetch sub.publisher as pub ")
		 .append(" where sub.identity.key=:anIdentityKey and pub.state=").append(PUB_STATE_OK);
		
		return dbInstance.getCurrentEntityManager()
				.createQuery(q.toString(), Subscriber.class)
				.setParameter("anIdentityKey", identity.getKey())
				.getResultList();
	}

	/**
	 * @see org.olat.core.commons.services.notifications.NotificationsManager#getValidSubscribersOf(org.olat.core.commons.services.notifications.Publisher)
	 */
	@Override
	public List<Subscriber> getValidSubscribersOf(Publisher publisher) {
		StringBuilder q = new StringBuilder();
		q.append("select sub from notisub sub ")
		 .append(" inner join fetch sub.identity")
		 .append(" where sub.publisher = :publisher and sub.publisher.state=").append(PUB_STATE_OK);
		
		return dbInstance.getCurrentEntityManager()
				.createQuery(q.toString(), Subscriber.class)
				.setParameter("publisher", publisher)
				.getResultList();
	}
	
	@Override
	public List<SubscriptionInfo> getSubscriptionInfos(Identity identity, String publisherType) {
		StringBuilder sb = new StringBuilder();
		sb.append("select sub from notisub sub")
			.append(" inner join fetch sub.publisher as pub")
			.append(" where sub.identity=:identity and pub.type=:type and pub.state=:aState");
		
		List<Subscriber> subscribers = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Subscriber.class)
				.setParameter("aState", PUB_STATE_OK)
				.setParameter("type", publisherType)
				.setParameter("identity", identity)
				.getResultList();
		if(subscribers.isEmpty()) {
			return Collections.emptyList();
		}

		Locale locale = new Locale(identity.getUser().getPreferences().getLanguage());
		Date compareDate = getDefaultCompareDate();
		List<SubscriptionInfo> sis = new ArrayList<SubscriptionInfo>();
		for(Subscriber subscriber : subscribers){
			Publisher pub = subscriber.getPublisher();
			NotificationsHandler notifHandler = getNotificationsHandler(pub);
			// do not create subscription item when deleted
			if (isPublisherValid(pub)) {
				SubscriptionInfo subsInfo = notifHandler.createSubscriptionInfo(subscriber, locale, compareDate);
				if (subsInfo.hasNews()) {
					sis.add(subsInfo);
				}
			}
		}
		return sis;
	}
	
	public void notifyAllSubscribersByEmail() {
		logAudit("starting notification cronjob to send email", null);
		WorkThreadInformations.setLongRunningTask("sendNotifications");
		
		int counter = 0;
		List<Identity> identities;
		do {
			identities = securityManager.loadIdentities(counter, BATCH_SIZE);
			for(Identity identity:identities) {
				processSubscribersByEmail(identity);
			}
			counter += identities.size();
			dbInstance.commitAndCloseSession();
		} while(identities.size() == BATCH_SIZE);
		
		// done, purge last entry
		WorkThreadInformations.unsetLongRunningTask("sendNotifications");
		logAudit("end notification cronjob to send email", null);
	}
	
	private void processSubscribersByEmail(Identity ident) {
		long start = System.currentTimeMillis();
		if(ident.getStatus().compareTo(Identity.STATUS_VISIBLE_LIMIT) >= 0) {
			return;//send only to active user
		}
		
		String userInterval = getUserIntervalOrDefault(ident);
		if("never".equals(userInterval)) {
			return;
		}

		Date compareDate = getCompareDateFromInterval(userInterval);
		Property p = propertyManager.findProperty(ident, null, null, null, LATEST_EMAIL_USER_PROP);
		if(p != null) {
		  	Date latestEmail = new Date(p.getLongValue());
		  	if(latestEmail.after(compareDate)) {
		  		return;//nothing to do
		  	}
		}

		Date defaultCompareDate = getDefaultCompareDate();
		List<Subscriber> subscribers = getSubscribers(ident);
		if(subscribers.isEmpty()) {
			return;
		}
		
		String langPrefs = null;
		if(ident.getUser() != null && ident.getUser().getPreferences() != null) {
			langPrefs = ident.getUser().getPreferences().getLanguage();
		}
		Locale locale = I18nManager.getInstance().getLocaleOrDefault(langPrefs);
		
		boolean veto = false;
		Subscriber latestSub = null;
		List<SubscriptionItem> items = new ArrayList<>();
		List<Subscriber> subsToUpdate = new ArrayList<>();
		for(Subscriber sub:subscribers) {
			Date latestEmail = sub.getLatestEmailed();
		
			SubscriptionItem subsitem = null;
			if (latestEmail == null || compareDate.after(latestEmail)){
				// no notif. ever sent until now
				if (latestEmail == null) {
					latestEmail = defaultCompareDate;
				}	else if (latestEmail.before(defaultCompareDate)) {
					//no notification older than a month
					latestEmail = defaultCompareDate;
				}
				subsitem = createSubscriptionItem(sub, locale, SubscriptionInfo.MIME_PLAIN, SubscriptionInfo.MIME_PLAIN, latestEmail);
			}	else if(latestEmail != null && latestEmail.after(compareDate)) {
				//already send an email within the user's settings interval
				veto = true;
			}
			if (subsitem != null) {
				items.add(subsitem);
				subsToUpdate.add(sub);
			}
			latestSub = sub;
		}
		
		Translator translator = Util.createPackageTranslator(NotificationsManagerImpl.class, locale);
		notifySubscribersByEmail(latestSub, items, subsToUpdate, translator, start, veto);
	}
	
	private void notifySubscribersByEmail(Subscriber latestSub, List<SubscriptionItem> items, List<Subscriber> subsToUpdate, Translator translator, long start, boolean veto) {
		if(veto) {
			if(latestSub != null) {
				logAudit(latestSub.getIdentity().getName() + " already received notification email within prefs interval");
			}
		} else if (items.size() > 0) {
			Identity curIdent = latestSub.getIdentity();
			boolean sentOk = sendMailToUserAndUpdateSubscriber(curIdent, items, translator, subsToUpdate);
			if (sentOk) {
				Property p = propertyManager.findProperty(curIdent, null, null, null, LATEST_EMAIL_USER_PROP);
				if(p == null) {
					p = propertyManager.createUserPropertyInstance(curIdent, null, LATEST_EMAIL_USER_PROP, null, null, null, null);
					p.setLongValue(new Date().getTime());
					propertyManager.saveProperty(p);
				} else {
					p.setLongValue(new Date().getTime());
					propertyManager.updateProperty(p);
				}
			  
				StringBuilder mailLog = new StringBuilder();
				mailLog.append("Notifications mailed for ").append(curIdent.getName()).append(' ').append(items.size()).append(' ').append((System.currentTimeMillis() - start)).append("ms");
				logAudit(mailLog.toString());
			} else {
				logAudit("Error sending notification email to : " + curIdent.getName());
			}
		}
		//collecting the SubscriptionItem can potentially make a lot of DB calls
		dbInstance.intermediateCommit();
	}

	/**
	 * @see org.olat.core.commons.services.notifications.NotificationsManager#getCompareDateFromInterval(java.lang.String)
	 */
	public Date getCompareDateFromInterval(String interval){
		Calendar calNow = Calendar.getInstance();
		// get hours to subtract from now
		Integer diffHours = INTERVAL_DEF_MAP.get(interval);
		calNow.add(Calendar.HOUR_OF_DAY, -diffHours);
		Date compareDate = calNow.getTime();
		return compareDate;		
	}
	
	/**
	 * Needs to correspond to notification-settings. 
	 * all available configs should be contained in the map below!
	 * @return
	 */
	private static final Map<String, Integer> buildIntervalMap(){
		Map<String, Integer> intervalDefMap = new HashMap<String, Integer>();		
		intervalDefMap.put("never", 0);
		intervalDefMap.put("monthly", 720);
		intervalDefMap.put("weekly", 168);
		intervalDefMap.put("daily", 24);
		intervalDefMap.put("half-daily", 12);
		intervalDefMap.put("four-hourly", 4);
		intervalDefMap.put("two-hourly", 2);
		return intervalDefMap;
	}
	
	/**
	 * @see org.olat.core.commons.services.notifications.NotificationsManager#getUserIntervalOrDefault(org.olat.core.id.Identity)
	 */
	public String getUserIntervalOrDefault(Identity ident){
		if(ident.getUser() == null || ident.getUser().getPreferences() == null) {
			logWarn("User " + ident.getName() + " has no preferences invalid", null);
			return getDefaultNotificationInterval();
		}
		
		String userInterval = ident.getUser().getPreferences().getNotificationInterval();
		if (!StringHelper.containsNonWhitespace(userInterval)) userInterval = getDefaultNotificationInterval();
		List<String> avIntvls = getEnabledNotificationIntervals();
		if (!avIntvls.contains(userInterval)) {
			logWarn("User " + ident.getName() + " has an invalid notification-interval (not found in config): " + userInterval, null);
			userInterval = getDefaultNotificationInterval();
		}
		return userInterval;
	}
	
	@Override
	public boolean sendMailToUserAndUpdateSubscriber(Identity curIdent, List<SubscriptionItem> items, Translator translator, List<Subscriber> subscribersToUpdate) {
		boolean sentOk = sendEmail(curIdent, translator.translate("rss.title", new String[] { NotificationHelper.getFormatedName(curIdent) }), items);
		// save latest email sent date for the subscription just emailed
		// do this only if the mail was successfully sent
		if (sentOk) {
			updateSubscriberLatestEmail(subscribersToUpdate);
		}
		return sentOk;
	}
	
	protected void updateSubscriberLatestEmail(List<Subscriber> subscribersToUpdate) {
		if(subscribersToUpdate == null || subscribersToUpdate.isEmpty()) {
			return;//nothing to do
		}
		
		StringBuilder q = new StringBuilder();	
		q.append("select sub from notisub sub ")
		 .append(" inner join fetch sub.publisher where sub.key in (:aKey)");
		
		EntityManager em = dbInstance.getCurrentEntityManager();
		List<Long> keys = PersistenceHelper.toKeys(subscribersToUpdate);
		List<Subscriber> subscribers = em.createQuery(q.toString(), Subscriber.class)
				.setParameter("aKey", keys)
				.getResultList();
		
		for (Subscriber subscriber :subscribers) {
			subscriber.setLastModified(new Date());
			subscriber.setLatestEmailed(new Date());
			em.merge(subscriber);
		}
	}
	
	private boolean sendEmail(Identity to, String title, List<SubscriptionItem> subItems) {
		StringBuilder plaintext = new StringBuilder();
		for (Iterator<SubscriptionItem> it_subs = subItems.iterator(); it_subs.hasNext();) {
			SubscriptionItem subitem = it_subs.next();
			plaintext.append(subitem.getTitle());
			if(StringHelper.containsNonWhitespace(subitem.getLink())) {
				plaintext.append("\n");
				plaintext.append(subitem.getLink());
			}
			plaintext.append("\n");
			if(StringHelper.containsNonWhitespace(subitem.getDescription())) {
				plaintext.append(subitem.getDescription());
			}
			plaintext.append("\n\n");
		}

		MailerResult result = null;
		try {
			MailBundle bundle = new MailBundle();
			bundle.setToId(to);
			bundle.setContent(title, plaintext.toString());
			result = CoreSpringFactory.getImpl(MailManager.class).sendExternMessage(bundle, null);
		} catch (Exception e) {
			// FXOLAT-294 :: sending the mail will throw nullpointer exception if To-Identity has no
			// valid email-address!, catch it...
		} 
		if (result == null || result.getReturnCode() > 0) {
			if(result!=null)
				log.warn("Could not send email to identity " + to.getName() + ". (returncode=" + result.getReturnCode() + ", to=" + to + ")");
			else
				log.warn("Could not send email to identity " + to.getName() + ". (returncode = null) , to=" + to + ")");
			return false;
		} else {
			return true;
		}
	}

	/**
	 * @param key
	 * @return the subscriber with this key or null if not found
	 */
	@Override
	public Subscriber getSubscriber(Long key) {
		StringBuilder q = new StringBuilder();
		q.append("select sub from notisub as sub")
		 .append(" inner join fetch sub.publisher ")
		 .append(" where sub.key=:aKey");

		List<Subscriber> res = dbInstance.getCurrentEntityManager()
				.createQuery(q.toString(), Subscriber.class)
				.setParameter("aKey", key.longValue())
				.getResultList();
		if (res.isEmpty()) return null;
		if (res.size() > 1) throw new AssertException("more than one subscriber for key " + key);
		return res.get(0);
	}

	/**
	 * @param scontext
	 * @param pdata
	 * @return the publisher
	 */
	public Publisher getOrCreatePublisher(final SubscriptionContext scontext, final PublisherData pdata) {
		return findOrCreatePublisher(scontext, pdata);
	}
	/**
	 * @param scontext
	 * @param pdata
	 * @return the publisher
	 */
	private Publisher findOrCreatePublisher(final SubscriptionContext scontext, final PublisherData pdata) {
		final OLATResourceable ores = OresHelper.createOLATResourceableInstance(scontext.getResName() + "_" + scontext.getSubidentifier(),scontext.getResId());
		//o_clusterOK by:cg
		//fxdiff VCRP-16:prevent nested doInSync
		Publisher pub = getPublisher(scontext);
		if(pub != null) {
			return pub;
		}
		
		Publisher publisher = CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(ores, new SyncerCallback<Publisher>(){
			public Publisher execute() {
				Publisher p = getPublisher(scontext);
				// if not found, create it
				if (p == null) {
					p = createAndPersistPublisher(scontext.getResName(), scontext.getResId(), scontext.getSubidentifier(), pdata.getType(), pdata
							.getData(), pdata.getBusinessPath());
				}
				return p;
			}
		});
		return publisher;
	}

	/**
	 * @param subsContext
	 * @return the publisher belonging to the given context or null
	 */
	@Override
	public Publisher getPublisher(SubscriptionContext subsContext) {
		StringBuilder q = new StringBuilder();
		q.append("select pub from notipublisher pub ")
		 .append(" where pub.resName=:resName and pub.resId = :resId");
		if(StringHelper.containsNonWhitespace(subsContext.getSubidentifier())) {
			q.append(" and pub.subidentifier=:subidentifier");
		} else {
			q.append(" and (pub.subidentifier='' or pub.subidentifier is null)");
		}
		
		TypedQuery<Publisher> query = dbInstance.getCurrentEntityManager()
				.createQuery(q.toString(), Publisher.class)
				.setParameter("resName", subsContext.getResName())
				.setParameter("resId", subsContext.getResId());

		if(StringHelper.containsNonWhitespace(subsContext.getSubidentifier())) {
			query.setParameter("subidentifier", subsContext.getSubidentifier());
		}
		List<Publisher> res = query.getResultList();
		if (res.isEmpty()) return null;
		if (res.size() != 1) throw new AssertException("only one subscriber per person and publisher!!");
		return res.get(0);
	}
	
	private Publisher getPublisherForUpdate(SubscriptionContext subsContext) {
		Publisher pub = getPublisher(subsContext);
		if(pub != null && pub.getKey() != null) {
			//prevent optimistic lock issue
			dbInstance.getCurrentEntityManager().detach(pub);
			pub = dbInstance.getCurrentEntityManager()
					.find(PublisherImpl.class, pub.getKey(), LockModeType.PESSIMISTIC_WRITE);
		}
		return pub;
	}
	
	@Override
	public List<Publisher> getAllPublisher() {
		String q = "select pub from notipublisher pub";
		return dbInstance.getCurrentEntityManager().createQuery(q, Publisher.class)
				.getResultList();
	}

	/**
	 * @param resName
	 * @param resId
	 * @return a list of publishers belonging to the resource
	 */
	private List<Publisher> getPublishers(String resName, Long resId) {
		String q = "select pub from notipublisher pub where pub.resName=:resName and pub.resId= :resId";
		return dbInstance.getCurrentEntityManager()
				.createQuery(q, Publisher.class)
				.setParameter("resName", resName)
				.setParameter("resId", resId.longValue())
				.getResultList();
	}

	/**
	 * deletes all publishers of the given olatresourceable. e.g. ores =
	 * businessgroup 123 -> deletes possible publishers: of Folder(toolfolder), of
	 * Forum(toolforum)
	 * 
	 * @param ores
	 */
	public void deletePublishersOf(OLATResourceable ores) {
		String type = ores.getResourceableTypeName();
		Long id = ores.getResourceableId();
		if (type == null || id == null) throw new AssertException("type/id cannot be null! type:" + type + " / id:" + id);
		List<Publisher> pubs = getPublishers(type, id);
		if(pubs.isEmpty()) return;

		String q1 = "delete from notisub sub where sub.publisher in (:publishers)";
		DBQuery query1 = dbInstance.createQuery(q1);
		query1.setParameterList("publishers", pubs);
		query1.executeUpdate(FlushMode.AUTO);
		
		String q2 = "delete from notipublisher pub where pub in (:publishers)";
		DBQuery query2 = dbInstance.createQuery(q2);
		query2.setParameterList("publishers", pubs);
		query2.executeUpdate(FlushMode.AUTO);
	}

	/**
	 * @param identity
	 * @param publisher
	 * @return a Subscriber object belonging to the identity and listening to the
	 *         given publisher
	 */
	@Override
	public Subscriber getSubscriber(Identity identity, Publisher publisher) {
		StringBuilder q = new StringBuilder();
		q.append("select sub from notisub as sub ")
		 .append(" where sub.publisher.key=:publisherKey and sub.identity.key=:identityKey");
		
		List<Subscriber> res = dbInstance.getCurrentEntityManager()
				.createQuery(q.toString(), Subscriber.class)
				.setParameter("publisherKey", publisher.getKey())
				.setParameter("identityKey", identity.getKey())
				.getResultList();

		if (res.size() == 0) return null;
		if (res.size() != 1) throw new AssertException("only one subscriber per person and publisher!!");
		Subscriber s = res.get(0);
		return s;
	}
	
	/**
	 * @see org.olat.core.commons.services.notifications.NotificationsManager#getSubscriber(org.olat.core.commons.services.notifications.Publisher)
	 */
	@Override
	public List<Subscriber> getSubscribers(Publisher publisher) {
		String q = "select sub notisub sub where sub.publisher = :publisher";
		return dbInstance.getCurrentEntityManager()
				.createQuery(q, Subscriber.class)
				.setParameter("publisher", publisher)
				.getResultList();
	}
	
	/**
	 * 
	 * @see org.olat.core.commons.services.notifications.NotificationsManager#getSubscriberIdentities(org.olat.core.commons.services.notifications.Publisher)
	 */
	@Override
	public List<Identity> getSubscriberIdentities(Publisher publisher) {
		String q = "select sub.identity from notisub sub where sub.publisher = :publisher";
		return dbInstance.getCurrentEntityManager()
				.createQuery(q, Identity.class)
				.setParameter("publisher", publisher)
				.getResultList();
	}

	/**
	 * @return the handler for the type
	 */
	public NotificationsHandler getNotificationsHandler(Publisher publisher) {
		String type = publisher.getType();
		if (notificationHandlers == null) {
			synchronized(lockObject) {
				if (notificationHandlers == null) { // check again in synchronized-block, only one may create list
					notificationHandlers = new HashMap<String,NotificationsHandler>();
					Map<String, NotificationsHandler> notificationsHandlerMap = CoreSpringFactory.getBeansOfType(NotificationsHandler.class);
					Collection<NotificationsHandler> notificationsHandlerValues = notificationsHandlerMap.values();
					for (NotificationsHandler notificationsHandler : notificationsHandlerValues) {
						log.debug("initNotificationUpgrades notificationsHandler=" + notificationsHandler);
						notificationHandlers.put(notificationsHandler.getType(), notificationsHandler);
					}
				}
			}	
		}
		return notificationHandlers.get(type);		
	}

	/**
	 * @param subscriber
	 */
	private void deleteSubscriber(Subscriber subscriber) {
		dbInstance.deleteObject(subscriber);
	}

	/**
	 * sets the latest visited date of the subscription to 'now' .assumes the
	 * identity is already subscribed to the publisher
	 * 
	 * @param identity
	 * @param subsContext
	 */
	@Override
	public void markSubscriberRead(Identity identity, SubscriptionContext subsContext) {
		Publisher p = getPublisher(subsContext);
		if (p == null) throw new AssertException("cannot markRead for identity " + identity.getName()
				+ ", since the publisher for the given subscriptionContext does not exist: subscontext = " + subsContext);

		markSubscriberRead(identity, p);
	}
	
	private Subscriber markSubscriberRead(Identity identity, Publisher p) {
		Subscriber sub = getSubscriber(identity, p);
		if(sub != null) {
			sub.setLastModified(new Date());
			sub = dbInstance.getCurrentEntityManager().merge(sub);
		}
		return sub;
	}

	/**
	 * @param identity
	 * @param subscriptionContext
	 * @param publisherData
	 */
	@Override
	public void subscribe(Identity identity, SubscriptionContext subscriptionContext, PublisherData publisherData) {
		//need to sync as opt-in is sometimes implemented
		Publisher toUpdate = getPublisherForUpdate(subscriptionContext);
		if(toUpdate == null) {
			//create the publisher
			findOrCreatePublisher(subscriptionContext, publisherData);
			//lock the publisher
			toUpdate = getPublisherForUpdate(subscriptionContext);
		}

		Subscriber s = getSubscriber(identity, toUpdate);
		if (s == null) {
			// no subscriber -> create.
			// s.latestReadDate >= p.latestNewsDate == no news for subscriber when no
			// news after subscription time
			doCreateAndPersistSubscriber(toUpdate, identity);
		}
		dbInstance.commit();
	}

	/**
	 * call this method to indicate that there is news for the given
	 * subscriptionContext
	 * 
	 * @param subscriptionContext
	 * @param ignoreNewsFor
	 */
	@Override
	public void markPublisherNews(final SubscriptionContext subscriptionContext, Identity ignoreNewsFor, boolean sendEvents) {
		// to make sure: ignore if no subscriptionContext
		if (subscriptionContext == null) return;

		Publisher toUpdate = getPublisherForUpdate(subscriptionContext);
		if(toUpdate == null) {
			return;
		}
		toUpdate.setLatestNewsDate(new Date());
		Publisher publisher = dbInstance.getCurrentEntityManager().merge(toUpdate);
		dbInstance.commit();//commit the select for update

		// no need to sync, since there is only one gui thread at a time from one
		// user
		if (ignoreNewsFor != null) {
			markSubscriberRead(ignoreNewsFor, publisher);
		}
		
		if(sendEvents) {
			//commit all things on the database
			dbInstance.commit();
			
			// channel-notify all interested listeners (e.g. the pnotificationsportletruncontroller)
			// 1. find all subscribers which can be affected
			List<Subscriber> subscribers = getValidSubscribersOf(publisher);
			
			Set<Long> subsKeys = new HashSet<Long>();
			// 2. collect all keys of the affected subscribers
			for (Iterator<Subscriber> it_subs = subscribers.iterator(); it_subs.hasNext();) {
				Subscriber su = it_subs.next();
				subsKeys.add(su.getKey());
			}
			// fire the event
			MultiUserEvent mue = EventFactory.createAffectedEvent(subsKeys);
			CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(mue, oresMyself);
		}
	}
	
	/**
	 * @see org.olat.core.commons.services.notifications.NotificationsManager#registerAsListener(org.olat.core.util.event.GenericEventListener, org.olat.core.id.Identity)
	 */
	public void registerAsListener(GenericEventListener gel, Identity ident) {
		CoordinatorManager.getInstance().getCoordinator().getEventBus().registerFor(gel, ident, oresMyself);
	}
	
	/**
	 * @see org.olat.core.commons.services.notifications.NotificationsManager#deregisterAsListener(org.olat.core.util.event.GenericEventListener)
	 */
	public void deregisterAsListener(GenericEventListener gel) {
		CoordinatorManager.getInstance().getCoordinator().getEventBus().deregisterFor(gel, oresMyself);
	}
	
	/**
	 * @param identity
	 * @param subscriptionContext
	 */
	public void unsubscribe(Identity identity, SubscriptionContext subscriptionContext) {
		// no need to sync, since an identity only has one gui thread / one mouse
		Publisher p = getPublisherForUpdate(subscriptionContext);
		// if no publisher yet.
		//TODO: check race condition: can p be null at all?
		if (p != null) {
			Subscriber s = getSubscriber(identity, p);
			if (s != null) {
				deleteSubscriber(s);
			} else {
				logWarn("could not unsubscribe " + identity.getName() + " from publisher:" + p.getResName() + ","	+ p.getResId() + "," + p.getSubidentifier(), null);
			}
		}
		dbInstance.commit();
	}

	/**
	 * 
	 * @see org.olat.core.commons.services.notifications.NotificationsManager#unsubscribe(org.olat.core.commons.services.notifications.Subscriber)
	 */
	@Override
	public void unsubscribe(Subscriber s) {
		Subscriber foundSub = getSubscriber(s.getKey());
		if (foundSub != null) {
			deleteSubscriber(foundSub);
		} else {
			logWarn("could not unsubscribe " + s.getIdentity().getName() + " from publisher:" + s.getPublisher().getResName() + ","	+ s.getPublisher().getResId() + "," + s.getPublisher().getSubidentifier(), null);
		}
	}

	/**
	 * @param identity
	 * @param subscriptionContext
	 * @return true if this user is subscribed
	 */
	@Override
	public boolean isSubscribed(Identity identity, SubscriptionContext subscriptionContext) {
		StringBuilder q = new StringBuilder();		
		q.append("select count(sub) from notisub as sub ")
		 .append(" inner join sub.publisher as pub ")
		 .append(" where sub.identity.key=:anIdentityKey and pub.resName=:resName and pub.resId=:resId")
		 .append(" and pub.subidentifier=:subidentifier");
		
		Number count = dbInstance.getCurrentEntityManager()
				.createQuery(q.toString(), Number.class)
				.setParameter("anIdentityKey", identity.getKey())
				.setParameter("resName", subscriptionContext.getResName())
				.setParameter("resId", subscriptionContext.getResId().longValue())
				.setParameter("subidentifier", subscriptionContext.getSubidentifier())
				.getSingleResult();

		long cnt = count.longValue();
		if (cnt == 0) return false;
		else return true;
	}

	/**
	 * delete publisher and subscribers
	 * 
	 * @param scontext the subscriptioncontext
	 */
	public void delete(SubscriptionContext scontext) {
		Publisher p = getPublisher(scontext);
		// if none found, no one has subscribed yet and therefore no publisher has
		// been generated lazily.
		// -> nothing to do
		if (p == null) return;
		//first delete all subscribers
		List<Subscriber> subscribers = getValidSubscribersOf(p);
		for (Subscriber subscriber : subscribers) {
			deleteSubscriber(subscriber);
		}
		// else:
		dbInstance.deleteObject(p);
	}

	/**
	 * delete publisher and subscribers
	 * 
	 * @param publisher the publisher to delete
	 */
	@Override
	public void deactivate(Publisher publisher) {
		EntityManager em = dbInstance.getCurrentEntityManager();
		
		PublisherImpl toDeactivate = em.find(PublisherImpl.class, publisher.getKey(), LockModeType.PESSIMISTIC_WRITE);
		toDeactivate.setState(PUB_STATE_NOT_OK);
		em.merge(toDeactivate);
		dbInstance.commit();
	}

	/**
	 * @param pub
	 * @return true if the publisher is valid (that is: has not been marked as
	 *         deleted)
	 */
	public boolean isPublisherValid(Publisher pub) {
		return pub.getState() == PUB_STATE_OK;
	}

	/**
	 * @param subscriber
	 * @param locale
	 * @param mimeType text/html or text/plain
	 * @return the item or null if there is currently no news for this subscription
	 */
	public SubscriptionItem createSubscriptionItem(Subscriber subscriber, Locale locale, String mimeTypeTitle, String mimeTypeContent) {
		// calculate the item based on subscriber.getLastestReadDate()
		// used for rss-feed, no longer than 1 month
		Date compareDate = getDefaultCompareDate(); 		
		return createSubscriptionItem(subscriber, locale, mimeTypeTitle, mimeTypeContent, compareDate);
	}

	/**
	 * if no compareDate is selected, cannot be calculated by user-interval, or no latestEmail is available => use this to get a Date 30d in the past.
	 * 
	 * maybe the latest user-login could also be used.
	 * @return Date
	 */
	private Date getDefaultCompareDate() {
		Calendar calNow = Calendar.getInstance();
		calNow.add(Calendar.DAY_OF_MONTH, -30);
		Date compareDate = calNow.getTime();
		return compareDate;
	}
	
	/**
	 * 
	 * @param subscriber
	 * @param locale
	 * @param mimeType
	 * @param latestEmailed needs to be given! SubscriptionInfo is collected from then until latestNews of publisher
	 * @return null if the publisher is not valid anymore (deleted), or if there are no news
	 */
	public SubscriptionItem createSubscriptionItem(Subscriber subscriber, Locale locale, String mimeTypeTitle, String mimeTypeContent, Date latestEmailed) {
		if (latestEmailed == null) throw new AssertException("compareDate may not be null, use a date from history");
		
		try {
			boolean debug = isLogDebugEnabled();
			
			SubscriptionItem si = null;
			Publisher pub = subscriber.getPublisher();
			NotificationsHandler notifHandler = getNotificationsHandler(pub);
			if(debug) logDebug("create subscription with handler: " + notifHandler.getClass().getName());
			// do not create subscription item when deleted
			if (isPublisherValid(pub)) {
				if(debug) logDebug("NotifHandler: " + notifHandler.getClass().getName() + " compareDate: " + latestEmailed.toString() + " now: " + new Date().toString(), null);
				SubscriptionInfo subsInfo = notifHandler.createSubscriptionInfo(subscriber, locale, latestEmailed);
				if (subsInfo.hasNews()) {
					si = createSubscriptionItem(subsInfo, subscriber, locale, mimeTypeTitle, mimeTypeContent);
				}
			}
			return si;
		} catch (Exception e) {
			log.error("Cannot generate a subscription item.", e);
			return null;
		}
	}
	
	@Override
	public SubscriptionItem createSubscriptionItem(SubscriptionInfo subsInfo, Subscriber subscriber, Locale locale, String mimeTypeTitle, String mimeTypeContent) {
		Publisher pub = subscriber.getPublisher();
		String title = getFormatedTitle(subsInfo, subscriber, locale, mimeTypeTitle); 
		
		String itemLink = null;
		if(subsInfo.getCustomUrl() != null) {
			itemLink = subsInfo.getCustomUrl();
		}
		if(itemLink == null && pub.getBusinessPath() != null) {
			itemLink = BusinessControlFactory.getInstance().getURLFromBusinessPathString(pub.getBusinessPath());
		}
		
		String description = subsInfo.getSpecificInfo(mimeTypeContent, locale);
		return new SubscriptionItem(title, itemLink, description);
	}
	
	/**
	 * format the type-title and title-details
	 * @param subscriber
	 * @param locale
	 * @param mimeType
	 * @return
	 */
	private String getFormatedTitle(SubscriptionInfo subsInfo, Subscriber subscriber, Locale locale, String mimeType){
		Publisher pub = subscriber.getPublisher();
		String innerType = pub.getType();
		String typeName = ControllerFactory.translateResourceableTypeName(innerType, locale);
		StringBuilder titleSb = new StringBuilder();
		titleSb.append(typeName);
		
		String title = subsInfo.getTitle(mimeType);
		if (StringHelper.containsNonWhitespace(title)) {
			titleSb.append(": ").append(title);
		} else {
			NotificationsHandler notifHandler = getNotificationsHandler(pub);
			String titleInfo = notifHandler.createTitleInfo(subscriber, locale);
			if (StringHelper.containsNonWhitespace(titleInfo)) {
				titleSb.append(": ").append(titleInfo);
			}
		}
		
		return titleSb.toString(); 
	}
	
	/**
	 * 
	 * @see org.olat.core.commons.services.notifications.NotificationsManager#getNoSubscriptionInfo()
	 */
	public SubscriptionInfo getNoSubscriptionInfo() {
		return NOSUBSINFO;
	}

	/**
	 * Delete all subscribers for certain identity.
	 * @param identity
	 */
	public void deleteUserData(Identity identity, String newDeletedUserName) {
		List<Subscriber> subscribers = getSubscribers(identity);
		for (Iterator<Subscriber> iter = subscribers.iterator(); iter.hasNext();) {
			deleteSubscriber( iter.next() );
		}
		logDebug("All notification-subscribers deleted for identity=" + identity, null);
	}

	/**
	 * Spring setter method
	 * 
	 * @param notificationIntervals
	 */
	public void setNotificationIntervals(Map<String, Boolean> intervals) {
		notificationIntervals = new ArrayList<String>();
		for(String key : intervals.keySet()) {
			if (intervals.get(key)) {
				if(key.length() <= 16) {
					notificationIntervals.add(key);
				} else {
					log.error("Interval notification cannot be more than 16 characters wide: " + key);
				}
			}
		}
	}

	/**
	 * Spring setter method
	 * 
	 * @param defaultNotificationInterval
	 */
	public void setDefaultNotificationInterval(String defaultNotificationInterval) {
		this.defaultNotificationInterval = defaultNotificationInterval;
	}

	/**
	 * @see org.olat.core.commons.services.notifications.NotificationsManager#getDefaultNotificationInterval()
	 */
	public String getDefaultNotificationInterval() {
		return defaultNotificationInterval;
	}

	/**
	 * @see org.olat.core.commons.services.notifications.NotificationsManager#getNotificationIntervals()
	 */
	public List<String> getEnabledNotificationIntervals() {
		return notificationIntervals;
	}
}