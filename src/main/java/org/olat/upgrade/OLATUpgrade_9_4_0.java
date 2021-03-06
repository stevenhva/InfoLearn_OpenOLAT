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
package org.olat.upgrade;

import java.util.List;

import org.olat.core.commons.persistence.DB;
import org.olat.core.util.mail.MailManager;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupImpl;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 23.01.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class OLATUpgrade_9_4_0 extends OLATUpgrade {
	
	private static final int BATCH_SIZE = 50;
	private static final String TASK_DISPLAY_MEMBERS = "Upgrade display members";
	private static final String VERSION = "OLAT_9.4.0";
	
	private static final String PROP_NAME = "displayMembers";
	private static final String OLATRESOURCE_CONFIGURATION = "config";
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private MailManager mailManager;
	@Autowired
	private PropertyManager propertyManager;
	
	public OLATUpgrade_9_4_0() {
		super();
	}

	@Override
	public String getVersion() {
		return VERSION;
	}
	
	@Override
	public boolean doPreSystemInitUpgrade(UpgradeManager upgradeManager) {
		return false;
	}

	@Override
	public boolean doPostSystemInitUpgrade(UpgradeManager upgradeManager) {
		UpgradeHistoryData uhd = upgradeManager.getUpgradesHistory(VERSION);
		if (uhd == null) {
			// has never been called, initialize
			uhd = new UpgradeHistoryData();
		} else if (uhd.isInstallationComplete()) {
			return false;
		}
		
		boolean allOk = upgradeDisplayMembers(upgradeManager, uhd);
		
		uhd.setInstallationComplete(allOk);
		upgradeManager.setUpgradesHistory(uhd, VERSION);
		if(allOk) {
			log.audit("Finished OLATUpgrade_9_4_0 successfully!");
		} else {
			log.audit("OLATUpgrade_9_4_0 not finished, try to restart OpenOLAT!");
		}
		return allOk;
	}
	
	private static final int showOwnersVal      = 1;// 0x..0001
	private static final int showPartipsVal     = 2;// 0x..0010
	private static final int showWaitingListVal = 4;// 0x..0100
	
	private boolean upgradeDisplayMembers(UpgradeManager upgradeManager, UpgradeHistoryData uhd) {
		if (!uhd.getBooleanDataValue(TASK_DISPLAY_MEMBERS)) {
			int counter = 0;
			List<BusinessGroupImpl> groups;
			do {
				groups = findGroups(counter, BATCH_SIZE);
				for(BusinessGroupImpl group:groups) {
					Property prop = findProperty(group);
					if(prop != null) {
						boolean changed = false;
						
						Long internValue = prop.getLongValue();
						if(internValue != null && internValue.longValue() > 0) {
							long value = internValue.longValue();
							boolean owners = (value & showOwnersVal) == showOwnersVal;
							boolean participants = (value & showPartipsVal) == showPartipsVal;
							boolean waiting = (value & showWaitingListVal) == showWaitingListVal;
							group.setOwnersVisibleIntern(owners);
							group.setParticipantsVisibleIntern(participants);
							group.setWaitingListVisibleIntern(waiting);
							changed = true;
						}
						
						String publicValue = prop.getStringValue();
						if(publicValue != null && publicValue.length() > 0
								&& Character.isDigit(publicValue.toCharArray()[0])) {
							try {
								int value = Integer.parseInt(publicValue);
								boolean owners = (value & showOwnersVal) == showOwnersVal;
								boolean participants = (value & showPartipsVal) == showPartipsVal;
								boolean waiting = (value & showWaitingListVal) == showWaitingListVal;
								group.setOwnersVisiblePublic(owners);
								group.setParticipantsVisiblePublic(participants);
								group.setWaitingListVisiblePublic(waiting);
							} catch (NumberFormatException e) {
								log.error("", e);
							}
							changed = true;
						}
						
						Float downloadValue = prop.getFloatValue();
						if(downloadValue != null && downloadValue != 0.0f) {
							float value = downloadValue.floatValue();
							//paranoid check
							if(value > 0.9 && value < 1.1) {
								group.setDownloadMembersLists(true);
								changed = true;
							} else if(value < -0.9 && value > -1.1) {
								group.setDownloadMembersLists(true);
								changed = true;
							}
						}
						
						if(changed) {
							prop.setCategory("configMoved");
							dbInstance.getCurrentEntityManager().merge(group);
						}
					}
				}
				counter += groups.size();
				log.audit("Business groups processed: " + groups.size());
				dbInstance.commitAndCloseSession();
			} while(groups.size() == BATCH_SIZE);
			uhd.setBooleanDataValue(TASK_DISPLAY_MEMBERS, false);
			upgradeManager.setUpgradesHistory(uhd, VERSION);
		}
		return true;
	}
	
	private List<BusinessGroupImpl> findGroups(int firstResult, int maxResults) {
		StringBuilder sb = new StringBuilder();	
		sb.append("select grp from ").append(BusinessGroupImpl.class.getName()).append(" grp order by key");
		return dbInstance.getCurrentEntityManager().createQuery(sb.toString(), BusinessGroupImpl.class)
				.setFirstResult(firstResult)
				.setMaxResults(maxResults)
				.getResultList();
	}
	
	public Property findProperty(BusinessGroup group) {
		Property prop = propertyManager.findProperty(null, group, group, OLATRESOURCE_CONFIGURATION, PROP_NAME);
		return prop;
	}
}
