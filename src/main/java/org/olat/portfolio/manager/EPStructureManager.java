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
package org.olat.portfolio.manager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.TypedQuery;

import org.hibernate.ObjectNotFoundException;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.NamedGroupImpl;
import org.olat.basesecurity.PolicyImpl;
import org.olat.basesecurity.SecurityGroup;
import org.olat.basesecurity.SecurityGroupImpl;
import org.olat.basesecurity.SecurityGroupMembershipImpl;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.commons.persistence.PersistentObject;
import org.olat.core.commons.services.commentAndRating.CommentAndRatingService;
import org.olat.core.commons.services.commentAndRating.impl.CommentAndRatingDefaultSecurityCallback;
import org.olat.core.commons.services.notifications.NotificationsManager;
import org.olat.core.commons.services.notifications.SubscriptionContext;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.StringHelper;
import org.olat.core.util.resource.OresHelper;
import org.olat.group.BusinessGroup;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.portfolio.model.restriction.CollectRestriction;
import org.olat.portfolio.model.restriction.RestrictionsConstants;
import org.olat.portfolio.model.structel.EPAbstractMap;
import org.olat.portfolio.model.structel.EPDefaultMap;
import org.olat.portfolio.model.structel.EPMapShort;
import org.olat.portfolio.model.structel.EPPage;
import org.olat.portfolio.model.structel.EPStructureElement;
import org.olat.portfolio.model.structel.EPStructureToArtefactLink;
import org.olat.portfolio.model.structel.EPStructureToStructureLink;
import org.olat.portfolio.model.structel.EPStructuredMap;
import org.olat.portfolio.model.structel.EPStructuredMapTemplate;
import org.olat.portfolio.model.structel.EPTargetResource;
import org.olat.portfolio.model.structel.ElementType;
import org.olat.portfolio.model.structel.PortfolioStructure;
import org.olat.portfolio.model.structel.PortfolioStructureMap;
import org.olat.portfolio.model.structel.StructureStatusEnum;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Description:<br>
 * Manager to operate ePortfolio maps, structure-elements, pages.
 * 
 * <P>
 * Initial Date:  11.06.2010 <br>
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class EPStructureManager extends BasicManager {
	
	public static final String STRUCTURE_ELEMENT_TYPE_NAME = "EPStructureElement";
	
	public static final OLATResourceable ORES_MAPOWNER = OresHelper.lookupType(EPStructureManager.class, "EPOwner");
	
	private DB dbInstance;
	private RepositoryManager repositoryManager;
	private OLATResourceManager resourceManager;
	private BaseSecurity securityManager;
	private EPPolicyManager policyManager;

	/**
	 * 
	 */
	public EPStructureManager() {
		//
	}

	/**
	 * [used by Spring]
	 * @param resourceManager
	 */
	public void setResourceManager(OLATResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}
	
	/**
	 * [used by Spring]
	 * @param db
	 */
	public void setDbInstance(DB db) {
		this.dbInstance = db;
	}
	
	/**
	 * [used by Spring]
	 * @param repositoryManager
	 */
	public void setRepositoryManager(RepositoryManager repositoryManager) {
		this.repositoryManager = repositoryManager;
	}
	
	@Autowired(required = true)
	public void setpolicyManager(final EPPolicyManager policyManager) {
		this.policyManager = policyManager;
	}
	
	/**
	 * [used by Spring]
	 * @param baseSecurity
	 */
	public void setBaseSecurity(BaseSecurity baseSecurity) {
		this.securityManager = baseSecurity;
	}

	/**
	 * Return the list of artefacts glued to this structure element
	 * @param structure
	 * @return A list of artefacts
	 */
	protected List<AbstractArtefact> getArtefacts(PortfolioStructure structure) {
		return getArtefacts(structure, -1, -1);
	}
	
	/**
	 * recursively fetches all linked artefacts in the given map.<br />
	 * ( iterates over all pages in the map, all artefacts on these pages, all
	 * artefacts in structureElements on these pages)
	 * 
	 * FXOLAT-431
	 * 
	 * @param map
	 * @return
	 
	protected List<AbstractArtefact> getAllArtefactsInMap(EPAbstractMap map){
		List<AbstractArtefact> results = new ArrayList<AbstractArtefact>();
		
		List<PortfolioStructure> children = loadStructureChildren(map);
		for (PortfolioStructure child : children) {
				// maps have pages as children, this will be true..!
				if(child instanceof EPPage){
					results.addAll(getArtefacts(child));
				}
		}
		return results;
	}
	*/
	
	protected List<PortfolioStructureMap> getOpenStructuredMapAfterDeadline() {
		StringBuilder sb = new StringBuilder();
		sb.append("select map from ").append(EPStructuredMap.class.getName()).append(" as map");
		sb.append(" where (map.status is null or not(map.status = 'closed'))")
			.append(" and map.deadLine<:currentDate");
		
		DBQuery query =	dbInstance.createQuery(sb.toString());
		query.setDate("currentDate", new Date());
		@SuppressWarnings("unchecked")
		List<PortfolioStructureMap> maps = query.list();
		return maps;
	}
	
	protected List<PortfolioStructure> getStructureElements(int firstResult, int maxResults, ElementType... types){
		StringBuilder sb = new StringBuilder();
		sb.append("select stEl from ").append(EPStructureElement.class.getName()).append(" stEl");
		sb.append(" where stEl.class in (");
		boolean first = true;
		for(ElementType type:types) {
			if(first) first = false;
			else sb.append(",");
			sb.append(getImplementation(type).getName());
		}
		sb.append(")");
		
		DBQuery query =	dbInstance.createQuery(sb.toString());
		if(firstResult > 0) {
			query.setFirstResult(firstResult);
		}
		if(maxResults > 0) {
			query.setMaxResults(maxResults);
		}
		@SuppressWarnings("unchecked")
		List<PortfolioStructure> pStructs = query.list();
		return pStructs;
	}
	
	protected List<PortfolioStructure> getStructureElementsForUser(Identity ident, ElementType... types){
		StringBuilder sb = new StringBuilder();
		sb.append("select stEl from ").append(EPStructureElement.class.getName()).append(" stEl");
		sb.append(" where ownerGroup in ( " )
			.append("select sgi.key from")
			.append(" org.olat.basesecurity.SecurityGroupImpl as sgi,") 
			.append(" org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi ")
			.append(" where sgmsi.securityGroup = sgi and sgmsi.identity =:ident")
			.append(" )");
		if(types != null && types.length > 0) {
			sb.append(" and stEl.class in (");
			boolean first = true;
			for(ElementType type:types) {
				if(first) first = false;
				else sb.append(",");
				sb.append(getImplementation(type).getName());
			}
			sb.append(")");
		}
		
		DBQuery query =	dbInstance.createQuery(sb.toString());
		query.setEntity("ident", ident);
		@SuppressWarnings("unchecked")
		List<PortfolioStructure> pStructs = query.list();
		return pStructs;
	}
	
	/**
	 * Check if the identity is owner of the map
	 * @param identity
	 * @param ores
	 * @return
	 */
	protected boolean isMapOwner(Identity identity, OLATResourceable ores) {
		StringBuilder sb = new StringBuilder();
		sb.append("select count(stEl) from ").append(EPStructureElement.class.getName()).append(" stEl ")
			.append(" where stEl.olatResource.resId=:resourceableId")
			.append(" and stEl.olatResource.resName=:resourceableTypeName")
			.append(" and stEl.ownerGroup in ( " )
			.append("  select sgi.key from")
			.append("  org.olat.basesecurity.SecurityGroupImpl as sgi,") 
			.append("  org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi ")
			.append("  where sgmsi.securityGroup = sgi and sgmsi.identity =:owner")
			.append(" )");
		
		DBQuery query =	dbInstance.createQuery(sb.toString());
		query.setEntity("owner", identity);
		query.setLong("resourceableId", ores.getResourceableId());
		query.setString("resourceableTypeName", ores.getResourceableTypeName());
	
		Number count = (Number)query.uniqueResult();
		return count.intValue() == 1;
	}
	
	/**
	 * Check if the identity is owner or is in a valid policy
	 * @param identity
	 * @param ores
	 * @return
	 */
	protected boolean isMapVisible(Identity identity, OLATResourceable ores) {
		StringBuilder sb = new StringBuilder();
		sb.append("select count(stEl) from ").append(EPStructureElement.class.getName()).append(" stEl ")
			.append(" inner join stEl.olatResource as oRes ")
			.append(" where oRes.resId=:resourceableId")
			.append(" and oRes.resName=:resourceableTypeName")
			.append(" and ( oRes in ( " )
			.append("  select policy.olatResource from")
			.append("  ").append(PolicyImpl.class.getName()).append(" as policy, ")
			.append("  ").append(SecurityGroupImpl.class.getName()).append(" as sgi,") 
			.append("  ").append(SecurityGroupMembershipImpl.class.getName()).append(" as sgmsi ")
			.append("  where sgi = policy.securityGroup")//implicit inner join
			.append("  and (sgmsi.securityGroup = sgi and sgmsi.identity =:identity) ")//member of the security group
			.append("  and (policy.from is null or policy.from<=:date)")
			.append("  and (policy.to is null or policy.to>=:date)")
			.append(" )")
			.append(" or stEl.ownerGroup in ( " )
			.append("select sgi.key from")
			.append(" org.olat.basesecurity.SecurityGroupImpl as sgi,") 
			.append(" org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi ")
			.append(" where sgmsi.securityGroup = sgi and sgmsi.identity =:identity")
			.append(" ))");

		DBQuery query =	dbInstance.createQuery(sb.toString());
		query.setEntity("identity", identity);
		query.setLong("resourceableId", ores.getResourceableId());
		query.setString("resourceableTypeName", ores.getResourceableTypeName());
		query.setDate("date", new Date());
	
		Number count = (Number)query.uniqueResult();
		return count.intValue() == 1;
	}
	
	/**
	 * 
	 * @param select
	 * @param ident
	 * @param choosenOwner
	 * @param limitFrom
	 * @param limitTo
	 * @param types
	 * @return
	 */
	private StringBuilder buildStructureElementsFromOthersLimitedQuery(String select,final Identity ident, final Identity choosenOwner ,final ElementType... types){
		StringBuilder sb = new StringBuilder();
		
		sb.append("select ").append(select).append(" from ");
		
		sb.append(EPStructureElement.class.getName()).append(" stEl ");
		sb.append(" inner join stEl.olatResource as oRes ");
		sb.append(" where oRes in ( ").append("  select policy.olatResource from").append("  ").append(PolicyImpl.class.getName()).append(" as policy, ").append("  ")
				.append(SecurityGroupImpl.class.getName()).append(" as sgi,").append("  ").append(SecurityGroupMembershipImpl.class.getName()).append(" as sgmsi ")
				.append("  where sgi = policy.securityGroup")// implicit inner join
				.append("  and (sgmsi.securityGroup = sgi and sgmsi.identity =:ident) ")// member of the security group
				.append("  and (policy.from is null or policy.from<=:date)").append("  and (policy.to is null or policy.to>=:date)").append(" )");

		// remove owner
		sb.append(" and stEl.ownerGroup not in ( ").append("select sgi2.key from").append(" org.olat.basesecurity.SecurityGroupImpl as sgi2,")
				.append(" org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi2 ").append(" where sgmsi2.securityGroup = sgi2 and sgmsi2.identity =:ident")
				.append(" )");

		if (choosenOwner != null) {
			sb.append(" and stEl.ownerGroup in ( ").append("select sgi.key from").append(" org.olat.basesecurity.SecurityGroupImpl as sgi,")
					.append(" org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi ").append(" where sgmsi.securityGroup = sgi and sgmsi.identity =:owner")
					.append(" )");
		}
		if (types != null && types.length > 0) {
			sb.append(" and stEl.class in (");
			boolean first = true;
			for (final ElementType type : types) {
				if (first) {
					first = false;
				} else {
					sb.append(",");
				}
				sb.append(getImplementation(type).getName());
			}
			sb.append(")");
		}
		
		return sb;
	}
	
	protected int countStructureElementsFromOthers(final Identity ident, final Identity choosenOwner ,final ElementType... types){
			StringBuilder sb = buildStructureElementsFromOthersLimitedQuery("count(*)", ident, choosenOwner, types);
			final DBQuery query = dbInstance.createQuery(sb.toString());
			query.setEntity("ident", ident);
			query.setDate("date", new Date());
			if (choosenOwner != null) {
				query.setEntity("owner", choosenOwner);
			}

			@SuppressWarnings("unchecked")
			final List<Long> resultList = query.list();
			if(resultList.size()>0) return resultList.get(0).intValue();
			return 0;
	}
	
	protected List<PortfolioStructure> getStructureElementsFromOthersLimited(final Identity ident, final Identity choosenOwner ,final int limitFrom, final int limitTo,final ElementType... types){
		final StringBuilder sb = buildStructureElementsFromOthersLimitedQuery("stEl", ident, choosenOwner, types);
		final DBQuery query = dbInstance.createQuery(sb.toString());
		
		//limits
		if(limitTo > 0 && (limitFrom < limitTo)){
			query.setFirstResult(limitFrom);
			query.setMaxResults(limitTo-limitFrom);
		}
		
		query.setEntity("ident", ident);
		query.setDate("date", new Date());
		if (choosenOwner != null) {
			query.setEntity("owner", choosenOwner);
		}

		@SuppressWarnings("unchecked")
		final List<PortfolioStructure> pStructs = query.list();
		return pStructs;
	}
	
	protected List<PortfolioStructure> getStructureElementsFromOthersWithoutPublic(Identity ident, Identity choosenOwner, ElementType... types){
		StringBuilder sb = new StringBuilder();
		sb.append("select stEl from ").append(EPStructureElement.class.getName()).append(" stEl ");
		sb.append(" inner join stEl.olatResource as oRes ");
		sb.append(" where oRes in ( " )
			.append("  select policy.olatResource from")
			.append("  ").append(PolicyImpl.class.getName()).append(" as policy, ")
			.append("  ").append(SecurityGroupImpl.class.getName()).append(" as sgi,")
			.append("  ").append(SecurityGroupMembershipImpl.class.getName()).append(" as sgmsi ")
			.append("  where sgi = policy.securityGroup ") //implicit inner join
			.append("  and sgi = policy.securityGroup ") 
			.append("  and (sgmsi.securityGroup = sgi and sgmsi.identity =:ident) ") //member of the security group
			.append("  and (policy.from is null or policy.from<=:date)")
			.append("  and (policy.to is null or policy.to>=:date)")
			.append("  and sgi not in (")
			.append("    select ngroup.securityGroup from ")
			.append("    ").append(NamedGroupImpl.class.getName()).append(" as ngroup ")
			.append("    where ngroup.groupName =:usersGroup")
			.append("   )")			
			.append(" )");
		
		//remove owner
		sb.append(" and stEl.ownerGroup not in ( " )
			.append("	 select sgi2.key from")
			.append("  org.olat.basesecurity.SecurityGroupImpl as sgi2,") 
			.append("  org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi2 ")
			.append("  where sgmsi2.securityGroup = sgi2 and sgmsi2.identity =:ident")
			.append(" )");
		
		if(choosenOwner != null) {
			sb.append(" and stEl.ownerGroup in ( " )
				.append("select sgi.key from")
				.append(" org.olat.basesecurity.SecurityGroupImpl as sgi,") 
				.append(" org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi ")
				.append(" where sgmsi.securityGroup = sgi and sgmsi.identity =:owner")
				.append(" )");
		}
		if(types != null && types.length > 0) {
			sb.append(" and stEl.class in (");
			boolean first = true;
			for(ElementType type:types) {
				if(first) first = false;
				else sb.append(",");
				sb.append(getImplementation(type).getName());
			}
			sb.append(")");
		}
		
		DBQuery query =	dbInstance.createQuery(sb.toString());
		query.setEntity("ident", ident);
		query.setString("usersGroup", Constants.GROUP_OLATUSERS);
		query.setDate("date", new Date());
		if(choosenOwner != null) {
			query.setEntity("owner", choosenOwner);
		}
		
		@SuppressWarnings("unchecked")
		List<PortfolioStructure> pStructs = query.list();
		return pStructs;
	}
	
	
	private Class<?> getImplementation(ElementType type) {
		switch(type) {
			case DEFAULT_MAP: return EPDefaultMap.class;
			case STRUCTURED_MAP: return EPStructuredMap.class;
			case TEMPLATE_MAP: return EPStructuredMapTemplate.class;
			default: return null;
		}
	}

	protected List<PortfolioStructure> getReferencedMapsForArtefact(AbstractArtefact artefact){
		List<PortfolioStructure> pfList = getAllReferencesForArtefact(artefact);
		List<PortfolioStructure> mapList = new ArrayList<PortfolioStructure>();
		for (Iterator<?> iterator = pfList.iterator(); iterator.hasNext();) {
			EPStructureElement portfolioStructure = (EPStructureElement) iterator.next();
			EPStructureElement actStruct = portfolioStructure;
			while (actStruct.getRoot() != null){
				EPStructureElement actRoot = actStruct.getRoot();
				if (actRoot != null) {
					actStruct = actRoot;
				} 				
			}
			if (!mapList.contains(actStruct)) mapList.add(actStruct);
		}		
		return mapList;		
	}
	
	protected List<PortfolioStructure> getAllReferencesForArtefact(AbstractArtefact artefact){
		StringBuilder sb = new StringBuilder();
		sb.append("select link.structureElement from ").append(EPStructureToArtefactLink.class.getName()).append(" link")
		.append(" where link.artefact=:artefactEl ");
		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setEntity("artefactEl", artefact);
		@SuppressWarnings("unchecked")
		List<PortfolioStructure> pfList = query.list();		
		return pfList;		
	}
	
/**
 * Return the list of artefacts glued to this structure element
 * @param structure
 * @param firstResult
 * @param maxResults
 * @return
 */
	public List<AbstractArtefact> getArtefacts(PortfolioStructure structure, int firstResult, int maxResults) {
		StringBuilder sb = new StringBuilder();
		sb.append("select link.artefact from ").append(EPStructureToArtefactLink.class.getName()).append(" link")
			.append(" where link.structureElement.key=:structureElKey order by link.order");
		
		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setLong("structureElKey", structure.getKey());
		if(firstResult > 0) {
			query.setFirstResult(firstResult);
		}
		if(maxResults > 0) {
			query.setMaxResults(maxResults);
		}
		
		@SuppressWarnings("unchecked")
		List<AbstractArtefact> artefacts = query.list();
		return artefacts;
	}
	
	/**
	 * Return the number of artefacts hold by a structure element
	 * @param structure
	 * @return
	 */
	public int countArtefacts(PortfolioStructure structure) {
		StringBuilder sb = new StringBuilder();
		sb.append("select count(link) from ").append(EPStructureToArtefactLink.class.getName()).append(" link")
			.append(" where link.structureElement=:structureEl");
		
		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setEntity("structureEl", structure);
		Number count = (Number)query.uniqueResult();
		return count.intValue();
	}
	
	/**
	 * Count all artefacts (links) in a map
	 */
	protected int countArtefactsRecursively(PortfolioStructure structure) {
		//return countArtefactsRecursively(structure, 0);
		
		StringBuilder sb = new StringBuilder();
		sb.append("select count(link) from ").append(EPStructureToArtefactLink.class.getName()).append(" link")
			.append(" inner join link.structureElement structure ")
			.append(" inner join structure.rootMap root")
			.append(" where root=:structureEl");
		
		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setEntity("structureEl", structure);
		Number count = (Number)query.uniqueResult();
		return count.intValue();
	}
	
	protected int countArtefactsRecursively(PortfolioStructure structure, int res){
		List<PortfolioStructure> childs = loadStructureChildren(structure);
		res = res + countArtefacts(structure);
		for (PortfolioStructure portfolioStructure : childs) {
			res = countArtefactsRecursively(portfolioStructure, res); 
		}
		return res;
	}
	
	protected boolean isArtefactInStructure(AbstractArtefact artefact, PortfolioStructure structure){
		StringBuilder sb = new StringBuilder();
		sb.append("select link.key from ").append(EPStructureToArtefactLink.class.getName()).append(" link")
			.append(" where link.structureElement=:structureEl and link.artefact=:artefact");
		
		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setEntity("structureEl", structure);
		query.setEntity("artefact", artefact);
		
		@SuppressWarnings("unchecked")
		List<Long> key = query.list();
		return key.size() == 1 ? true : false;
	}
	
	/**
	 * Number of children
	 */
	public int countStructureChildren(PortfolioStructure structure) {
		if (structure == null) throw new NullPointerException();

		StringBuilder sb = new StringBuilder();
		sb.append("select count(link) from ").append(EPStructureToStructureLink.class.getName()).append(" link")
			.append(" where link.parent=:structureEl");
		
		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setEntity("structureEl", structure);

		Number count = (Number)query.uniqueResult();
		return count.intValue();
	}
	
	/**
	 * Retrieve the children structures
	 * @param structure
	 * @return
	 */
	protected List<PortfolioStructure> loadStructureChildren(PortfolioStructure structure) {
		return loadStructureChildren(structure, -1, -1);
	}
	
/**
 * 
 * @param structure
 * @param firstResult
 * @param maxResults
 * @return
 */
	protected List<PortfolioStructure> loadStructureChildren(PortfolioStructure structure, int firstResult, int maxResults) {
		if (structure == null) throw new NullPointerException();

		StringBuilder sb = new StringBuilder();
		sb.append("select link.child from ").append(EPStructureToStructureLink.class.getName()).append(" link")
			.append(" where link.parent=:structureEl order by link.order");
		
		DBQuery query = dbInstance.createQuery(sb.toString());
		if(firstResult > 0) {
			query.setFirstResult(firstResult);
		}
		if(maxResults > 0) {
			query.setMaxResults(maxResults);
		}
		query.setEntity("structureEl", structure);
		
		@SuppressWarnings("unchecked")
		List<PortfolioStructure> resources = query.list();
		return resources;
	}
	
	/**
	 * Retrieve the parent of the structure
	 * @param structure
	 * @return
	 */
	protected PortfolioStructure loadStructureParent(PortfolioStructure structure) {
		if (structure == null) throw new NullPointerException();

		StringBuilder sb = new StringBuilder();
		sb.append("select link.parent from ").append(EPStructureToStructureLink.class.getName()).append(" link")
			.append(" where link.child=:structureEl");
		
		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setEntity("structureEl", structure);
		
		@SuppressWarnings("unchecked")
		List<PortfolioStructure> resources = query.list();
		if(resources.isEmpty()) return null;
		if(resources.size() == 1) return resources.get(0);
		getLogger().error("A structure child has more than one parent");
		return null;
	}
	
	protected Integer[] getRestrictionStatistics(PortfolioStructure structure) {
		if (structure instanceof EPStructureElement) {
			EPStructureElement structEl = (EPStructureElement) structure;
			structEl = (EPStructureElement) reloadPortfolioStructure(structEl);
			final List<CollectRestriction> restrictions = structEl.getCollectRestrictions();

			if (restrictions != null && !restrictions.isEmpty()) {
				int todo = 0;
				int done = 0;
				List<AbstractArtefact> artefacts = getArtefacts(structEl);
				for (CollectRestriction cR : restrictions) {
					if (RestrictionsConstants.MIN.equals(cR.getRestriction()) || RestrictionsConstants.EQUAL.equals(cR.getRestriction())) {
						todo += cR.getAmount();
						int actualCRCount = countRestrictionType(artefacts, cR);
						done += actualCRCount;
					}
				}
				return new Integer[] { done, todo };
			}
		}
		return null;
	}
	
	// count recursively
	protected Integer[] getRestrictionStatisticsOfMap(PortfolioStructure structureMap, int done, int todo) {
		final List<PortfolioStructure> children = loadStructureChildren(structureMap);
		for (final PortfolioStructure child : children) {			
			Integer[] childStat = getRestrictionStatisticsOfMap(child, done, todo);
			done = childStat[0];
			todo = childStat[1];
		}	
		// summarize
		Integer[] statsArr = getRestrictionStatistics(structureMap);
		if (statsArr != null){
			done += statsArr[0];
			todo += statsArr[1];			
		}

		return new Integer[] {done, todo};
	}

	/**
	 * Add a link between a structure element and an artefact
	 * @param author
	 * @param artefact
	 * @param structure
	 * @return
	 */
	protected boolean addArtefactToStructure(Identity author, AbstractArtefact artefact, PortfolioStructure structure) {
		if (author == null || artefact == null || structure == null) throw new NullPointerException();
		if (structure instanceof EPStructureElement) {
			EPStructureElement structureEl = (EPStructureElement)structure;
			boolean canAdd = canAddArtefact(structureEl, artefact);
			if(!canAdd) {
				return false;
			}
			//save eventual changes
			//TODO update the changes before dbInstance.updateObject(structureEl);
			//reconnect to the session
			structureEl = (EPStructureElement)dbInstance.loadObject(structureEl);

			EPStructureToArtefactLink link = new EPStructureToArtefactLink();
			link.setArtefact(artefact);
			link.setStructureElement(structureEl);
			link.setAuthor(author);
			structureEl.getInternalArtefacts().add(link);
			dbInstance.updateObject(structureEl);
			return true;
		}
		return false;
	}
	
	protected boolean canAddArtefact(EPStructureElement structureEl, AbstractArtefact newArtefact) {

		List<CollectRestriction> restrictions = structureEl.getCollectRestrictions();
		if(restrictions == null || restrictions.isEmpty()) return true;

		boolean allOk = true;
		List<String> artefactTypeAllowed = new ArrayList<String>();
		List<AbstractArtefact> artefacts = getArtefacts(structureEl);
		artefacts.add(newArtefact);
		
		for(CollectRestriction restriction:restrictions) {
			String type = restriction.getArtefactType();
			int count = countRestrictionType(artefacts, restriction);
			artefactTypeAllowed.add(type);

			if(type.equals(newArtefact.getResourceableTypeName())) {
				if(RestrictionsConstants.MAX.equals(restriction.getRestriction())) {
					allOk &= (restriction.getAmount() > 0 && count <= restriction.getAmount());
				} else if(RestrictionsConstants.EQUAL.equals(restriction.getRestriction())) {
					allOk &= (restriction.getAmount() > 0 && count <= restriction.getAmount());
				}
			}
		}

		allOk &= artefactTypeAllowed.contains(newArtefact.getResourceableTypeName());
		return allOk;
	}
	
	protected boolean moveArtefactFromStructToStruct(AbstractArtefact artefact, PortfolioStructure oldParStruct, PortfolioStructure newParStruct) {
		EPStructureElement oldEPSt = (EPStructureElement)dbInstance.loadObject((EPStructureElement)oldParStruct);
		Identity author = oldEPSt.getInternalArtefacts().get(0).getAuthor();
		if (author == null) return false; // old model without author, doesn't work!
		
		String reflexion = getReflexionForArtefactToStructureLink(artefact, oldParStruct);
		
		removeArtefactFromStructure(artefact, oldParStruct);
		boolean allOk = false;
		allOk = addArtefactToStructure(author, artefact, newParStruct);
		if (allOk) return setReflexionForArtefactToStructureLink(artefact, newParStruct, reflexion);
		return allOk;
	}
	
	protected boolean moveArtefactInStruct(AbstractArtefact artefact, PortfolioStructure parStruct, int position) {
		EPStructureElement structureEl = (EPStructureElement)dbInstance.loadObject((EPStructureElement)parStruct);
		Identity author = structureEl.getInternalArtefacts().get(0).getAuthor();
		if (author == null) return false; // old model without author, doesn't work!

		List<EPStructureToArtefactLink> artefactLinks =  structureEl.getInternalArtefacts();
		int currentIndex = -1;
		for(EPStructureToArtefactLink link:artefactLinks) {
			currentIndex++;
			if(link.getArtefact().equals(artefact)) {
				break;
			}
		}
		
		if(currentIndex > -1 && currentIndex < artefactLinks.size()) {
			EPStructureToArtefactLink link = artefactLinks.remove(currentIndex);
			if(position > currentIndex) {
				position--;
			}
			artefactLinks.add(position, link);
		}
		return true;
	}
	
	/**
	 * Check the collect restriction against the structure element
	 * @param structure
	 * @return
	 */
	protected boolean checkCollectRestriction(PortfolioStructure structure) {
		if (structure instanceof EPStructureElement) {
			EPStructureElement structureEl = (EPStructureElement)structure;
			List<CollectRestriction> restrictions = structureEl.getCollectRestrictions();
			if(restrictions == null || restrictions.isEmpty()) return true;

			boolean allOk = true;
			List<String> artefactTypeAllowed = new ArrayList<String>();
			List<AbstractArtefact> artefacts = getArtefacts(structureEl);
			for(CollectRestriction restriction:restrictions) {
				int count = countRestrictionType(artefacts, restriction);
				artefactTypeAllowed.add(restriction.getArtefactType());
				boolean ok = true;
				if(RestrictionsConstants.MAX.equals(restriction.getRestriction())) {
					ok &= (restriction.getAmount() > 0 && count <= restriction.getAmount());
				} else if(RestrictionsConstants.MIN.equals(restriction.getRestriction())) {
					ok &= (restriction.getAmount() > 0 && count >= restriction.getAmount());
				} else if(RestrictionsConstants.EQUAL.equals(restriction.getRestriction())) {
					ok &= (restriction.getAmount() > 0 && count == restriction.getAmount());
				} else {
					ok &= false;
				}
				allOk &= ok;
			}
			
			for(AbstractArtefact artefact:artefacts) {
				allOk &= artefactTypeAllowed.contains(artefact.getResourceableTypeName());
			}
			return allOk;
		}
		return true;
	}
	
	private int countRestrictionType(List<AbstractArtefact> artefacts, CollectRestriction restriction) {
		int count = 0;
		if(StringHelper.containsNonWhitespace(restriction.getArtefactType())) {
			for(AbstractArtefact artefact:artefacts) {
				if(restriction.getArtefactType().equals(artefact.getResourceableTypeName())) {
					count++;
				}
			}
		}
		return count;
	}
	
/**
 * Remove a link between a structure element and an artefact.
 * @param author The author of the link
 * @param artefact The artefact to link
 * @param structure The structure element
 * @return The link
 */
	protected PortfolioStructure removeArtefactFromStructure(AbstractArtefact artefact, PortfolioStructure structure) {
		return removeArtefactFromStructure(artefact, structure, false);
	}

	private PortfolioStructure removeArtefactFromStructure(AbstractArtefact artefact, PortfolioStructure structure, boolean updateFirst) {
		if (artefact == null || structure == null) throw new NullPointerException();
		if (artefact.getKey() == null) return null;//not persisted
		if(structure instanceof EPStructureElement) {
			//save eventual changes
			if(updateFirst) {
				dbInstance.updateObject(structure);
			}
			//reconnect to the session
			EPStructureElement structureEl = (EPStructureElement)dbInstance.loadObject((EPStructureElement)structure);
			EPStructureToArtefactLink linkToDelete = null;
			for(Iterator<EPStructureToArtefactLink> linkIt=structureEl.getInternalArtefacts().iterator(); linkIt.hasNext(); ) {
				EPStructureToArtefactLink link = linkIt.next();
				if(link.getArtefact().getKey().equals(artefact.getKey())) {
					linkIt.remove();
					linkToDelete = link;
					break;
				}
			}
			//I have not set the cascade all delete 
			if(linkToDelete != null) {
				dbInstance.updateObject(structureEl);
				dbInstance.deleteObject(linkToDelete);
			}
			return structureEl;
		}
		return null;
	}
	
	/**
	 * Move up an artefact in the list
	 * @param structure
	 * @param artefact
	 */
	public void moveUp(PortfolioStructure structure, AbstractArtefact artefact) {
		move(structure, artefact, true);
	}
	
	/**
	 * Move down an artefact in the list
	 * @param structure
	 * @param artefact
	 */
	public void moveDown(PortfolioStructure structure, AbstractArtefact artefact) {
		move(structure, artefact, false);
	}
	
	private void move(PortfolioStructure structure, AbstractArtefact artefact, boolean up) {
		if (artefact == null || structure == null) throw new NullPointerException();
		if (structure instanceof EPStructureElement) {
			//save eventual changes
			dbInstance.updateObject(structure);
			//reconnect to the session
			EPStructureElement structureEl = (EPStructureElement)dbInstance.loadObject((EPStructureElement)structure);
			List<EPStructureToArtefactLink> artefactLinks = structureEl.getInternalArtefacts();
			int index = indexOf(artefactLinks, artefact);
			if(up && index > 0) {
				//swap the link with the previous link in the list
				Collections.swap(artefactLinks, index, index-1);
				dbInstance.updateObject(structureEl);
			} else if(!up && (index >= 0 && index < (artefactLinks.size() - 1))) {
				//swap the link with the next link in the list
				Collections.swap(artefactLinks, index, index+1);
				dbInstance.updateObject(structureEl);
			}
		}
	}
	
	private int indexOf(List<EPStructureToArtefactLink> artefactLinks, AbstractArtefact artefact) {
		int count = 0;
		for(EPStructureToArtefactLink link:artefactLinks) {
			if(link.getArtefact().getKey().equals(artefact.getKey())) {
				return count;
			}
			count++;
		}
		return -1;
	}

 /**
  * Add a child structure to the parent structure.
  * @param parentStructure
  * @param childStructure
  * @param destinationPos set to -1 to append at the end!
  */
	public void addStructureToStructure(PortfolioStructure parentStructure, PortfolioStructure childStructure, int destinationPos) {
		if (parentStructure == null || childStructure == null) throw new NullPointerException();
		if(childStructure instanceof EPStructureElement) {
			//save eventual changes
			dbInstance.updateObject(parentStructure);
			//reconnect to the session
			parentStructure = (EPStructureElement)dbInstance.loadObject((EPStructureElement)parentStructure);
			EPStructureToStructureLink link = new EPStructureToStructureLink();
			link.setParent(parentStructure);
			link.setChild(childStructure);

			//refresh internal link to its root element
			((EPStructureElement)childStructure).setRoot((EPStructureElement) parentStructure);
			
			if (destinationPos == -1) {
				((EPStructureElement)parentStructure).getInternalChildren().add(link);
			} else {
				((EPStructureElement)parentStructure).getInternalChildren().add(destinationPos, link);
			}
		}
	}
	
	protected boolean moveStructureToNewParentStructure(PortfolioStructure structToBeMvd,	PortfolioStructure oldParStruct, PortfolioStructure newParStruct, int destinationPos){
		if (structToBeMvd == null || oldParStruct == null || newParStruct == null) throw new NullPointerException();
		try { // try catch, as used in d&d TOC-tree, should still continue on error
			removeStructure(oldParStruct, structToBeMvd);
			addStructureToStructure(newParStruct, structToBeMvd, destinationPos);
		} catch (Exception e) {
			logError("could not move structure " + structToBeMvd.getKey() + " from " + oldParStruct.getKey() + " to " + newParStruct.getKey(), e);
			return false;
		}
		return true;
	}
	
	public void deleteRootStructure(PortfolioStructure rootStructure) {
		if (rootStructure == null) throw new NullPointerException();
		if (rootStructure.getKey() == null) return;
		if(rootStructure instanceof EPStructureElement) {
			dbInstance.deleteObject(rootStructure);
		}
		
	}
	
 /**
  * Remove a child structure from its parent structure.
  * @param parentStructure
  * @param childStructure
  * 
  * 
  */
	
	// this has to be done recursively for pages, structs also!
	// also remove the artefacts from each!
	public void removeStructure(PortfolioStructure parentStructure, PortfolioStructure childStructure) {
		if (childStructure == null) throw new NullPointerException();
		if (childStructure.getKey() == null) return;//child not persisted
		if (parentStructure == null) return; // cannot remove with no parent!
		if(childStructure instanceof EPStructureElement) {
			//save eventual changes
			dbInstance.updateObject(parentStructure);
			//reconnect to the session
			
			EPStructureToStructureLink linkToDelete = null;
			EPStructureElement parentStructureEl = (EPStructureElement)dbInstance.loadObject((EPStructureElement)parentStructure);
			for(Iterator<EPStructureToStructureLink> linkIt=parentStructureEl.getInternalChildren().iterator(); linkIt.hasNext(); ) {
				EPStructureToStructureLink link = linkIt.next();
				
//				List<AbstractArtefact> thisStructsArtefacts = getArtefacts(link.getChild());
//				for (AbstractArtefact artefact : thisStructsArtefacts) {
//					removeArtefactFromStructure(artefact, link.getChild());					
//				}
				
				if(link.getChild().getKey().equals(childStructure.getKey())) {
					linkIt.remove();
					linkToDelete = link;
					break;
				}
			}

			//I have not set the cascade all delete 
			if(linkToDelete != null) {
				dbInstance.updateObject(parentStructureEl);
				dbInstance.deleteObject(linkToDelete);
			}
		}
		if (parentStructure == childStructure) {
			deleteRootStructure(childStructure);
			return;
		}
	}
	
	/**
	 * This method is only for templates.
	 * @param res
	 */
	public void deletePortfolioMapTemplate(OLATResourceable res) {
		PortfolioStructure map = loadPortfolioStructure(res);
		if(map == null) {
			return;//nothing to delete
		}

		//already delete in removeStructureRecursively: deletePortfolioMapTemplateRecursively((EPStructureElement)map);
		removeStructureRecursively(map);
		//already delete in removeStructureRecursively: dbInstance.deleteObject(map);
	}
	
	/*private void deletePortfolioMapTemplateRecursively(EPStructureElement element) {
		element.getInternalArtefacts().clear();
		element.setRoot(null);
		element.setRootMap(null);
		List<EPStructureToStructureLink> links = element.getInternalChildren();
		for(EPStructureToStructureLink subLink:links) {
			deletePortfolioMapTemplateRecursively((EPStructureElement)subLink.getChild());
		}
		links.clear();
	}*/
	
	public void removeStructureRecursively(PortfolioStructure struct){
		List<PortfolioStructure> children = loadStructureChildren(struct); 
		for (PortfolioStructure childstruct : children) {
			removeStructureRecursively(childstruct);
		}
		// remove artefact-links
		List<AbstractArtefact> thisStructsArtefacts = getArtefacts(struct);
		for (AbstractArtefact artefact : thisStructsArtefacts) {
			removeArtefactFromStructure(artefact, struct, false);					
		}
		
		// remove from parent
		PortfolioStructure parent = loadStructureParent(struct);
		if (parent == null && struct.getRoot() != null) parent = struct.getRoot();
		removeStructure(parent, struct);
		
		// remove collect restriction
		struct.getCollectRestrictions().clear();
		
		// remove sharings
		if (struct instanceof EPAbstractMap){
			List<EPMapPolicy> noMorePol = new ArrayList<EPMapPolicy>();
			policyManager.updateMapPolicies((PortfolioStructureMap) struct, noMorePol);
		}
		
		// remove comments and ratings
		CommentAndRatingService commentAndRatingService = null;
		commentAndRatingService = (CommentAndRatingService) CoreSpringFactory.getBean(CommentAndRatingService.class);
		commentAndRatingService.init(struct.getOlatResource(), null, new CommentAndRatingDefaultSecurityCallback(null, true, false));
		commentAndRatingService.deleteAllIgnoringSubPath();
		
		
		// FXOLAT-431 remove subscriptions if the current struct is a map
		if(struct instanceof EPAbstractMap){
			SubscriptionContext subsContext = new SubscriptionContext(EPNotificationsHandler.TYPENNAME, struct.getResourceableId(), EPNotificationsHandler.TYPENNAME);
			NotificationsManager.getInstance().delete(subsContext);
		}
		
		// remove structure itself
		struct = (EPStructureElement) dbInstance.loadObject((EPStructureElement)struct);
		dbInstance.deleteObject(struct);		
		if (struct instanceof EPAbstractMap){
			EPAbstractMap esmap = (EPAbstractMap)struct;
			SecurityGroup securityGroup = esmap.getOwnerGroup();
			if (securityGroup!=null) {
				securityManager.deleteSecurityGroup(securityGroup);
				resourceManager.deleteOLATResourceable(securityGroup);
			}
		}
		
		resourceManager.deleteOLATResourceable(struct);
	}
	
	
	/**
	 * Move a structure element up in the list
	 * @param parentStructure
	 * @param childStructure
	 */
	public void moveUp(PortfolioStructure parentStructure, PortfolioStructure childStructure) {
		move(parentStructure, childStructure, true);
	}
	
	/**
	 * Move a structure element down in the list and save the parent and the list
	 * @param parentStructure
	 * @param childStructure
	 */
	public void moveDown(PortfolioStructure parentStructure, PortfolioStructure childStructure) {
		move(parentStructure, childStructure, false);
	}
	
	private void move(PortfolioStructure parentStructure, PortfolioStructure childStructure, boolean up) {
		if (childStructure == null || parentStructure == null) throw new NullPointerException();
		if (parentStructure instanceof EPStructureElement) {
			//save eventual changes
			dbInstance.updateObject(parentStructure);
			//reconnect to the session
			EPStructureElement structureEl = (EPStructureElement)dbInstance.loadObject((EPStructureElement)parentStructure);
			List<EPStructureToStructureLink> structureLinks = structureEl.getInternalChildren();
			int index = indexOf(structureLinks, childStructure);
			if(up && index > 0) {
				//swap the link with the previous link in the list
				Collections.swap(structureLinks, index, index-1);
				dbInstance.updateObject(structureEl);
			} else if(!up && (index >= 0 && index < (structureLinks.size() - 1))) {
				//swap the link with the next link in the list
				Collections.swap(structureLinks, index, index+1);
				dbInstance.updateObject(structureEl);
			}
		}
	}
	
	protected boolean reOrderStructures(PortfolioStructure parent, PortfolioStructure orderSubject, int orderDest){
		EPStructureElement structureEl = (EPStructureElement)dbInstance.loadObject((EPStructureElement)parent);
		List<EPStructureToStructureLink> structureLinks = structureEl.getInternalChildren();

		int oldPos = indexOf(structureLinks, orderSubject);		
		if (oldPos != orderDest && oldPos != -1) {
			EPStructureToStructureLink link = structureLinks.remove(oldPos);
			if(orderDest > structureLinks.size()) {
				orderDest = structureLinks.size() -1; // place at end
			} else if(oldPos < orderDest) {
				orderDest--;
			}
			structureLinks.add(orderDest, link);			
			dbInstance.updateObject(structureEl);
			return true;
		}
		return false;
	}
	
	private int indexOf(List<EPStructureToStructureLink> structLinks, PortfolioStructure structure) {
		int count = 0;
		for(EPStructureToStructureLink link:structLinks) {
			if(link.getChild().getKey().equals(structure.getKey())) {
				return count;
			}
			count++;
		}
		return -1;
	}

	
	protected void copyStructureRecursively(PortfolioStructure source, PortfolioStructure target, boolean withArtefacts) {
		//all changes are overwritten
		EPStructureElement targetEl = (EPStructureElement)target;
		if(targetEl instanceof EPStructuredMap) {
			((EPStructuredMap)targetEl).setCopyDate(new Date());
		}
		
		//update the source
		dbInstance.updateObject(source);
		//reconnect to the session
		EPStructureElement sourceEl = (EPStructureElement)source;
		targetEl.setStyle(sourceEl.getStyle());
		copyEPStructureElementRecursively(sourceEl, targetEl, withArtefacts, true);
	}
	
	private void copyEPStructureElementRecursively(EPStructureElement sourceEl, EPStructureElement targetEl, boolean withArtefacts, boolean cloneRestrictions) {
		//needed if the sourceEl come from a link. Hibernate doesn't initialize the list properly
		sourceEl = (EPStructureElement)dbInstance.loadObject(sourceEl);
		if(withArtefacts) {
			List<EPStructureToArtefactLink> artefactLinks = sourceEl.getInternalArtefacts();
			for(EPStructureToArtefactLink artefactLink:artefactLinks) {
				EPStructureToArtefactLink link = instantiateClone(artefactLink);
				link.setStructureElement(targetEl);// make the pseudo
				targetEl.getInternalArtefacts().add(link); // bidirectional relations
			}
		}
		
		//clone the links
		List<EPStructureToStructureLink> childLinks = sourceEl.getInternalChildren();
		for(EPStructureToStructureLink childLink:childLinks) {
			copy(childLink, targetEl, withArtefacts, false, cloneRestrictions); 
		}
		
		savePortfolioStructure(targetEl);
	}
	
	/**
	 * Sync the tree structure recursively with or without artefacts
	 * @param sourceEl
	 * @param targetEl
	 * @param withArtefacts
	 */
	protected void syncStructureRecursively(PortfolioStructure source, PortfolioStructure target, boolean withArtefacts) {
		//all changes are overwritten
		EPStructureElement sourceEl = (EPStructureElement)source;
		
		//update the source
		dbInstance.updateObject(target);
		//reconnect to the session
		EPStructureElement targetEl = (EPStructureElement)dbInstance.loadObject((EPStructureElement)target);
		syncEPStructureElementRecursively(sourceEl, targetEl, withArtefacts);
	}
	
	/**
	 * This sync method syncs the structure of the tree, collect restriction, title, description, representation-mode (table/miniview) 
	 * 
	 * @param sourceEl
	 * @param targetEl
	 * @param withArtefacts
	 */
	private void syncEPStructureElementRecursively(EPStructureElement sourceEl, EPStructureElement targetEl, boolean withArtefacts) {		
		List<EPStructureToStructureLink> sourceRefLinks = new ArrayList<EPStructureToStructureLink>(sourceEl.getInternalChildren());
		List<EPStructureToStructureLink> targetRefLinks = new ArrayList<EPStructureToStructureLink>(targetEl.getInternalChildren());

		Comparator<EPStructureToStructureLink> COMPARATOR = new KeyStructureToStructureLinkComparator();

		//remove deleted elements
		for(Iterator<EPStructureToStructureLink> targetIt=targetEl.getInternalChildren().iterator(); targetIt.hasNext(); ) {
			EPStructureToStructureLink targetLink = targetIt.next();
			int index = indexOf(sourceRefLinks, targetLink, COMPARATOR);
			if(index < 0) {
				targetIt.remove();
				removeStructureRecursively(targetLink.getChild());
			}
		}
		
		//add new element
		for(EPStructureToStructureLink sourceRefLink:sourceRefLinks) {
			int index = indexOf(targetRefLinks, sourceRefLink, COMPARATOR);
			if(index < 0) {
				//create a new structure element, dont clone restriction!
				copy(sourceRefLink, targetEl, withArtefacts, false, false); 
			}
		}
		
		//sync attributes, representation and collect restrictions
		copyOrUpdateCollectRestriction(sourceEl, targetEl, true);
		targetEl.setArtefactRepresentationMode(sourceEl.getArtefactRepresentationMode());
		targetEl.setStyle(sourceEl.getStyle());
		targetEl.setTitle(sourceEl.getTitle());
		targetEl.setDescription(sourceEl.getDescription());		
		
		//at this point, we must have the same content in the two list
		//but with perhaps other ordering: reorder
		List<EPStructureToStructureLink> targetLinks = targetEl.getInternalChildren();
		for(int i=0; i<sourceRefLinks.size(); i++) {
			EPStructureToStructureLink sourceRefLink = sourceRefLinks.get(i);
			int index = indexOf(targetLinks, sourceRefLink, COMPARATOR);
			if(index == i) {
				//great, right at its position
			} else if (index > i) {
				Collections.swap(targetLinks, i, index);
			} else {
				//not possible
			}
			
			//sync recursively
			if(index >= 0) {
				EPStructureElement subSourceEl = (EPStructureElement)sourceRefLink.getChild();
				EPStructureElement subTargetEl = (EPStructureElement)targetLinks.get(i).getChild();
				syncEPStructureElementRecursively(subSourceEl, subTargetEl, withArtefacts);
			}
		}
		
		targetEl = dbInstance.getCurrentEntityManager().merge(targetEl);
	}
	
	private int indexOf(List<EPStructureToStructureLink> refLinks, EPStructureToStructureLink link, Comparator<EPStructureToStructureLink> comparator) {
		int count=0;
		for(EPStructureToStructureLink refLink:refLinks) {
			if(comparator.compare(refLink, link) == 0) {
				return count;
			}
			count++;
		}
		return -1;
	}
	
	/**
	 * Copy/Import structure elements recursively
	 * @param refLink
	 * @param targetEl
	 * @param withArtefacts Copy the artefacts
	 * @param importEl Don't load elements from the DB
	 * @param cloneRestrictions should the collect-restrictions be applied? you could also do this manually by copyCollectRestriction()
	 */
	private void copy(EPStructureToStructureLink refLink, EPStructureElement targetEl, boolean withArtefacts, boolean importEl, boolean cloneRestrictions) {
		EPStructureElement childSourceEl = (EPStructureElement)refLink.getChild();
		EPStructureElement clonedChildEl = instantiateClone(refLink.getChild());
		if(clonedChildEl == null) {
			logWarn("Attempt to clone an unsupported structure type: " + refLink.getChild(), null);
		} else {
			OLATResource resource = resourceManager.createOLATResourceInstance(clonedChildEl.getClass());
			clonedChildEl.setOlatResource(resource);
			//set root
			if(targetEl.getRoot() == null) {
				//it's the root element
				clonedChildEl.setRoot(targetEl);
			} else {
				clonedChildEl.setRoot(targetEl.getRoot());
			}
			if(targetEl.getRootMap() == null && targetEl instanceof PortfolioStructureMap) {
				clonedChildEl.setRootMap((PortfolioStructureMap)targetEl);
			} else {
				clonedChildEl.setRootMap(targetEl.getRootMap());
			}
			if (!importEl) clonedChildEl.setStructureElSource(childSourceEl.getKey());
			
			if (cloneRestrictions) copyOrUpdateCollectRestriction(childSourceEl, clonedChildEl, true);
			if(importEl) {
				importEPStructureElementRecursively(childSourceEl, clonedChildEl);
			} else {
				copyEPStructureElementRecursively(childSourceEl, clonedChildEl, withArtefacts, cloneRestrictions);
			}

			EPStructureToStructureLink link = new EPStructureToStructureLink();
			link.setParent(targetEl);
			link.setChild(clonedChildEl);
			targetEl.getInternalChildren().add(link);
		}
	}
	
	private EPStructureToArtefactLink instantiateClone(EPStructureToArtefactLink link) {
		EPStructureToArtefactLink clone = new EPStructureToArtefactLink();
		clone.setArtefact(link.getArtefact());
		clone.setAuthor(link.getAuthor());
		clone.setCreationDate(new Date());
		clone.setReflexion(link.getReflexion());
		return clone;
	}
	
	private EPStructureElement instantiateClone(PortfolioStructure source) {
		EPStructureElement targetEl = null;
		//don't forget the inheritence 
		if (source instanceof EPPage) {
			targetEl = new EPPage();
			targetEl.setTitle(((EPPage) source).getTitle());
			targetEl.setDescription(((EPPage) source).getDescription());
		} else if(source instanceof EPStructureElement) {
			targetEl = new EPStructureElement();
			targetEl.setTitle(((EPStructureElement) source).getTitle());
			targetEl.setDescription(((EPStructureElement) source).getDescription());
		}
		return targetEl;
	}
	
	/**
	 * 
	 * @param source
	 * @param target
	 * @param update if true, the old existing restrictions will be overwritten
	 */
	private void copyOrUpdateCollectRestriction(PortfolioStructure source, PortfolioStructure target, boolean update) {
		if(source == null || target == null) {
			return;
		}
		List<CollectRestriction> targetRestrictions = target.getCollectRestrictions();
		if ((source.getCollectRestrictions() == null || source.getCollectRestrictions().isEmpty()) && (target.getCollectRestrictions() != null && !target.getCollectRestrictions().isEmpty()) && update){
			// remove former existing restrictions
			targetRestrictions.clear();
			return;
		}
		
		if (update) {
			targetRestrictions.clear();
		} 
		for(CollectRestriction sourceRestriction: source.getCollectRestrictions()) {
			CollectRestriction targetRestriction = new CollectRestriction();
			targetRestriction.setArtefactType(sourceRestriction.getArtefactType());
			targetRestriction.setAmount(sourceRestriction.getAmount());
			targetRestriction.setRestriction(sourceRestriction.getRestriction());
			targetRestrictions.add(targetRestriction);
		}
	}
	
	public boolean isTemplateInUse(PortfolioStructureMap template, OLATResourceable targetOres,
			String targetSubPath, String targetBusinessPath) {
		StringBuilder sb = new StringBuilder();
		sb.append("select count(map) from ").append(EPStructuredMap.class.getName()).append(" map")
			.append(" where map.structuredMapSource=:template");
		if (targetOres != null) {
			sb.append(" and map.targetResource.resourceableId=:resourceId")
				.append(" and map.targetResource.resourceableTypeName=:resourceType");
		}
		if (targetSubPath != null) {
			sb.append(" and map.targetResource.subPath=:subPath");
		}
		if (targetBusinessPath != null) {
			sb.append(" and map.targetResource.businessPath=:businessPath");
		}

		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setEntity("template", template);
		if (targetOres != null) {
			query.setLong("resourceId", targetOres.getResourceableId());
			query.setString("resourceType", targetOres.getResourceableTypeName());
		}
		if (targetSubPath != null) {
			query.setString("subPath", targetSubPath);
		}
		if (targetBusinessPath != null) {
			query.setString("businessPath", targetBusinessPath);
		}
		
		Number count = (Number)query.uniqueResult();
		return count.intValue() > 0;
	}
	
	public PortfolioStructureMap loadPortfolioStructuredMap(Identity identity, PortfolioStructureMap template,
			OLATResourceable targetOres, String targetSubPath, String targetBusinessPath) {
		if (template == null) throw new NullPointerException();
		if (!(template instanceof EPStructuredMapTemplate)) throw new AssertException("Only template are acceptable");

		StringBuilder sb = new StringBuilder();
		sb.append("select map from ").append(EPStructuredMap.class.getName()).append(" map")
			.append(" where map.structuredMapSource=:template");
		if (targetOres != null) {
			sb.append(" and map.targetResource.resourceableId=:resourceId")
				.append(" and map.targetResource.resourceableTypeName=:resourceType");
		}
		if (targetSubPath != null) {
			sb.append(" and map.targetResource.subPath=:subPath");
		}
		if (targetBusinessPath != null) {
			sb.append(" and map.targetResource.businessPath=:businessPath");
		}
		sb.append(" and map.ownerGroup in ( " )
			.append("select sgi.key from")
			.append(" org.olat.basesecurity.SecurityGroupImpl as sgi,") 
			.append(" org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi ")
			.append(" where sgmsi.securityGroup = sgi and sgmsi.identity =:ident")
			.append(" )");
		
		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setEntity("template", template);
		query.setEntity("ident", identity);
		if (targetOres != null) {
			query.setLong("resourceId", targetOres.getResourceableId());
			query.setString("resourceType", targetOres.getResourceableTypeName());
		}
		if (targetSubPath != null) {
			query.setString("subPath", targetSubPath);
		}
		if (targetBusinessPath != null) {
			query.setString("businessPath", targetBusinessPath);
		}
		
		@SuppressWarnings("unchecked")
		List<PortfolioStructureMap> maps = query.list();
		// if not found, it is an empty list
		if (maps.isEmpty()) return null;
		return maps.get(0);
	}
	
	public List<PortfolioStructureMap> loadPortfolioStructuredMaps(Identity identity,
			OLATResourceable targetOres, String targetSubPath, String targetBusinessPath) {

		StringBuilder sb = new StringBuilder();
		sb.append("select map from ").append(EPStructuredMap.class.getName()).append(" map")
			.append(" where map.targetResource.resourceableId=:resourceId")
			.append(" and map.targetResource.resourceableTypeName=:resourceType");

		if (targetSubPath != null) {
			sb.append(" and map.targetResource.subPath=:subPath");
		}
		if (targetBusinessPath != null) {
			sb.append(" and map.targetResource.businessPath=:businessPath");
		}
		sb.append(" and map.ownerGroup in ( " )
			.append("select sgi.key from ")
			.append(SecurityGroupImpl.class.getName()).append(" as sgi,")
			.append(SecurityGroupMembershipImpl.class.getName()).append(" as sgmsi ")
			.append(" where sgmsi.securityGroup = sgi and sgmsi.identity =:ident")
			.append(" )");
		
		TypedQuery<PortfolioStructureMap> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), PortfolioStructureMap.class)
				.setParameter("ident", identity)
				.setParameter("resourceId", targetOres.getResourceableId())
				.setParameter("resourceType", targetOres.getResourceableTypeName());

		if (targetSubPath != null) {
			query.setParameter("subPath", targetSubPath);
		}
		if (targetBusinessPath != null) {
			query.setParameter("businessPath", targetBusinessPath);
		}
		return query.getResultList();
	}
	
	/**
	 * Load the repository entry of a template with the map key
	 * @param key The template key
	 * @return The repository entry
	 */
	public RepositoryEntry loadPortfolioRepositoryEntryByMapKey(Long key) {
		if (key == null) throw new NullPointerException();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select repo from ").append(RepositoryEntry.class.getName()).append(" repo")
			.append(" where repo.olatResource in (select map.olatResource from ")
			.append(EPStructuredMapTemplate.class.getName())
			.append(" map where map.key=:key")
			.append(")");
		
		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setLong("key", key);
		
		@SuppressWarnings("unchecked")
		List<RepositoryEntry> entries = query.list();
		// if not found, it is an empty list
		if (entries.isEmpty()) return null;
		return entries.get(0);
	}

	/**
	 * @param olatResourceable cannot be null
	 * @return The structure element or null if not found
	 */
	public PortfolioStructure loadPortfolioStructure(OLATResourceable olatResourceable) {
		if (olatResourceable == null) throw new NullPointerException();
		
		OLATResource resource = resourceManager.findResourceable(olatResourceable);
		if (resource == null) return null;
		
		StringBuilder sb = new StringBuilder();
		sb.append("select element from ").append(EPStructureElement.class.getName()).append(" element")
			.append(" where element.olatResource=:resource");
		
		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setEntity("resource", resource);
		
		@SuppressWarnings("unchecked")
		List<PortfolioStructure> resources = query.list();
		// if not found, it is an empty list
		if (resources.isEmpty()) return null;
		return resources.get(0);
	}
	
	/**
	 * @param olatResourceable cannot be null
	 * @return The structure element or null if not found
	 */
	public EPMapShort loadMapShortByResourceId(Long resourceableId) {
		StringBuilder sb = new StringBuilder();
		sb.append("select element from ").append(EPMapShort.class.getName()).append(" element")
		  .append(" inner join fetch element.olatResource resource")
			.append(" where resource.resId=:resourceId and resource.resName in ('EPDefaultMap','EPStructuredMap','EPStructuredMapTemplate')");
		
		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setLong("resourceId", resourceableId);
		
		@SuppressWarnings("unchecked")
		List<EPMapShort> resources = query.list();
		// if not found, it is an empty list
		if (resources.isEmpty()) return null;
		return resources.get(0);
	}
	
	/**
	 * Load a portfolio structure by its primary key
	 * @param key cannot be null
	 * @return The structure element or null if not found
	 */	
	//FIXME: epf: SR: error loading structures without olatresource! 
	public PortfolioStructure loadPortfolioStructureByKey(Long key) {
		if (key == null) throw new NullPointerException();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select element from ").append(EPStructureElement.class.getName()).append(" element")
			.append(" where element.key=:key");
		
		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setLong("key", key);
		
		@SuppressWarnings("unchecked")
		List<PortfolioStructure> resources = query.list();
		// if not found, it is an empty list
		if (resources.isEmpty()) return null;
		return resources.get(0);
	}
	
	/**
	 * Reload an object
	 * @param structure
	 * @return The reloaded object or null if not found
	 */
	public PortfolioStructure reloadPortfolioStructure(PortfolioStructure structure) {
		if (structure == null) throw new NullPointerException();
		try {
			return (PortfolioStructure)dbInstance.loadObject(EPStructureElement.class, structure.getKey());
		} catch (ObjectNotFoundException e) {
			return null;
		}
	}
	
	public OLATResource loadOlatResourceFromStructureElByKey(Long key) {
		if (key == null) throw new NullPointerException();

		StringBuilder sb = new StringBuilder();
		sb.append("select element.olatResource from ").append(EPStructureElement.class.getName()).append(" element")
			.append(" where element.key=:key or element.olatResource.resId=:key ");
		
		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setLong("key", key);
		
		@SuppressWarnings("unchecked")
		List<OLATResource> resources = query.list();
		// if not found, it is an empty list
		if (resources.isEmpty()) return null;
		return resources.get(0);
	}

/**
 * Create a basic structure element
 * @param title
 * @param description
 * @return The structure element
 */
	protected PortfolioStructure createPortfolioStructure(PortfolioStructure root,  String title, String description) {
		EPStructureElement el = new EPStructureElement();
		el.setRoot((EPStructureElement)root);
		if(root != null && root.getRootMap() == null && root instanceof PortfolioStructureMap) {
			el.setRootMap((PortfolioStructureMap)root);
		} else if (root != null) {
			el.setRootMap(root.getRootMap());
		}
		return fillStructureElement(el, title, description);
	}
	
/**
 * Create a page element
 * @param title
 * @param description
 * @return The structure element
 */
	protected PortfolioStructure createPortfolioPage(PortfolioStructure root, String title, String description) {
		EPPage el = new EPPage();
		el.setRoot((EPStructureElement)root);
		if(root != null && root.getRootMap() == null && root instanceof PortfolioStructureMap) {
			el.setRootMap((PortfolioStructureMap)root);
		} else if(root != null) {
			el.setRootMap(root.getRootMap());
		}
		return fillStructureElement(el, title, description);
	}
	
	protected PortfolioStructureMap createPortfolioStructuredMap(PortfolioStructureMap template,
			Identity identity, String title, String description, OLATResourceable targetOres, String targetSubPath, String targetBusinessPath) {
		EPStructuredMap el = new EPStructuredMap();
		el.setStructuredMapSource((EPStructuredMapTemplate)template);
		el.setStructureElSource(template.getKey());
		
		if(template != null) {
			copyOrUpdateCollectRestriction(template, el, false);
		}
		
		EPTargetResource targetResource = el.getTargetResource();
		if(targetOres != null) {
			targetResource.setResourceableId(targetOres.getResourceableId());
			targetResource.setResourceableTypeName(targetOres.getResourceableTypeName());
		}
		if(StringHelper.containsNonWhitespace(targetSubPath)) {
			targetResource.setSubPath(targetSubPath);
		}
		if(StringHelper.containsNonWhitespace(targetBusinessPath)) {
			targetResource.setBusinessPath(targetBusinessPath);
		}
		
		fillStructureElement(el, title, description);
		
		//create security group
		SecurityGroup ownerGroup = createSecurityGroup(el, identity);
		el.setOwnerGroup(ownerGroup);
		return el;
	}
	
	protected PortfolioStructureMap createPortfolioDefaultMap(Identity identity, String title, String description) {
		EPDefaultMap el = new EPDefaultMap();
		
		fillStructureElement(el, title, description);

		//create security group
		SecurityGroup ownerGroup = createSecurityGroup(el, identity);
		el.setOwnerGroup(ownerGroup);
		
		return el;
	}
	
	protected PortfolioStructureMap createPortfolioDefaultMap(BusinessGroup group, String title, String description) {
		EPDefaultMap el = new EPDefaultMap();

		//don't create security group for map linked to a group
		//SecurityGroup ownerGroup = group.getOwnerGroup();
		//el.setOwnerGroup(ownerGroup);
		
		fillStructureElement(el, title, description);
		return el;
	}
	
	private EPStructureElement fillStructureElement(EPStructureElement el, String title, String description) {
		el.setTitle(title);
		el.setDescription(description);
		OLATResource resource = resourceManager.createOLATResourceInstance(el.getClass());
		el.setOlatResource(resource);
		dbInstance.saveObject(resource);
		return el;
	}
	
/**
 * Create a map template, create an OLAT resource and a repository entry with a security group
 * of type owner to the repository and add the identity has an owner.
 * @param identity
 * @param title
 * @param description
 * @return The structure element
 */
	public PortfolioStructureMap createPortfolioMapTemplate(Identity identity, String title, String description) {
		EPStructuredMapTemplate el = new EPStructuredMapTemplate();
		
		fillStructureElement(el, title, description);
		
		//create security group
		SecurityGroup ownerGroup = createSecurityGroup(el, identity);
		el.setOwnerGroup(ownerGroup);

		//create a repository entry with default security settings
		createRepositoryEntry(identity, ownerGroup, el.getOlatResource(), title);
		return el;
	}
	
	/**
	 * Import the structure.
	 * @param root
	 * @param identity
	 * @return
	 */
	public PortfolioStructureMap importPortfolioMapTemplate(PortfolioStructure root, Identity identity) {
		EPStructuredMapTemplate el = new EPStructuredMapTemplate();
		
		fillStructureElement(el, root.getTitle(), root.getDescription());
		EPStructuredMapTemplate rootTemp = (EPStructuredMapTemplate) root;
		rootTemp.setStructureElSource(null);
		
		el.setStyle(((EPStructureElement)root).getStyle()); 
		importEPStructureElementRecursively((EPStructureElement)root, el);
		
		//create an empty security group
		SecurityGroup ownerGroup = securityManager.createAndPersistSecurityGroup();
		el.setOwnerGroup(ownerGroup);
		
		return el;
	}
	
	private void importEPStructureElementRecursively(EPStructureElement sourceEl, EPStructureElement targetEl) {

		
		//clone the links
		List<EPStructureToStructureLink> childLinks = sourceEl.getInternalChildren();
		for(EPStructureToStructureLink childLink:childLinks) {
			EPStructureElement childSourceEl = (EPStructureElement) childLink.getChild();
			childSourceEl.setStructureElSource(null); // remove source-info on imports.
			copy(childLink, targetEl, false, true, true); 
		}
		
		savePortfolioStructure(targetEl);
	}

	/**
	 * Create an OLAT Resource with the type of a template map.
	 * @return
	 */
	public OLATResource createPortfolioMapTemplateResource() {
		OLATResource resource = resourceManager.createOLATResourceInstance(EPStructuredMapTemplate.class);
		return resource;
	}
	
	/**
	 * Create a template map with the given repsoitory entry and olat resource (in the repository entry).
	 * The repository entry must already be persisted.
	 * @param identity
	 * @param entry
	 * @return
	 */
	public PortfolioStructureMap createAndPersistPortfolioMapTemplateFromEntry(Identity identity, RepositoryEntry entry) {
		EPStructuredMapTemplate el = (EPStructuredMapTemplate)loadPortfolioStructure(entry.getOlatResource());
		if(el == null) {
			el = new EPStructuredMapTemplate();
		}
		el.setTitle(entry.getDisplayname());
		el.setDescription(entry.getDescription());
		el.setOlatResource(entry.getOlatResource());
		
		//create security group
		SecurityGroup ownerGroup = entry.getOwnerGroup();
		if(ownerGroup == null) {
			ownerGroup = createSecurityGroup(el,identity);
		}
		el.setOwnerGroup(ownerGroup);
		
		dbInstance.saveObject(el);
		return el;
	}
	
	/**
	 * Add an author to the repository entry linked to the map
	 * @param map
	 * @param author
	 */
	public void addAuthor(PortfolioStructureMap map, Identity author) {
		if(map instanceof EPStructuredMapTemplate) {
			EPStructuredMapTemplate mapImpl = (EPStructuredMapTemplate)map;
			RepositoryEntry re = repositoryManager.lookupRepositoryEntry(mapImpl.getOlatResource(), true);
			SecurityGroup ownerGroup = re.getOwnerGroup();
			if (!securityManager.isIdentityInSecurityGroup(author, ownerGroup)) {
				securityManager.addIdentityToSecurityGroup(author, ownerGroup);
				dbInstance.updateObject(ownerGroup);
			}
		}
	}
	
	/**
	 * Remove an author to repository entry linked to the map
	 * @param map
	 * @param author
	 */
	public void removeAuthor(PortfolioStructureMap map, Identity author) {
		if(map instanceof EPStructuredMapTemplate) {
			EPStructuredMapTemplate mapImpl = (EPStructuredMapTemplate)map;
			RepositoryEntry re = repositoryManager.lookupRepositoryEntry(mapImpl.getOlatResource(), true);
			SecurityGroup ownerGroup = re.getOwnerGroup();
			if (securityManager.isIdentityInSecurityGroup(author, ownerGroup)) {
				securityManager.removeIdentityFromSecurityGroup(author, ownerGroup);
				dbInstance.updateObject(ownerGroup);
			}
		}
	}
	
	private void createRepositoryEntry(Identity identity, SecurityGroup ownerGroup, OLATResource oresable, String title) {
		// create a repository entry
		RepositoryEntry addedEntry = repositoryManager.createRepositoryEntryInstance(identity.getName());
		addedEntry.setCanDownload(false);
		addedEntry.setCanLaunch(true);
		addedEntry.setDisplayname(title);
		addedEntry.setResourcename("-");
		// Do set access for owner at the end, because unfinished course should be invisible
		addedEntry.setAccess(RepositoryEntry.ACC_OWNERS);
		
		if(ownerGroup == null) {
			//create security group
			ownerGroup = securityManager.createAndPersistSecurityGroup();
			//create olat resource for the security group
			OLATResource ownerGroupResource =  resourceManager.createOLATResourceInstance(ownerGroup);
			resourceManager.saveOLATResource(ownerGroupResource);
			// member of this group may modify member's membership
			securityManager.createAndPersistPolicy(ownerGroup, Constants.PERMISSION_ACCESS, ownerGroup);
			// members of this group are always authors also
			securityManager.createAndPersistPolicy(ownerGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_AUTHOR);
			securityManager.addIdentityToSecurityGroup(identity, ownerGroup);
		}
		addedEntry.setOwnerGroup(ownerGroup);

		// Set the resource on the repository entry and save the entry.
		// bind resource and repository entry
		addedEntry.setOlatResource(oresable);
		resourceManager.saveOLATResource(oresable);
		repositoryManager.saveRepositoryEntry(addedEntry);
	}
	
	private SecurityGroup createSecurityGroup(EPAbstractMap map, Identity author) {
		//create security group
		SecurityGroup ownerGroup = securityManager.createAndPersistSecurityGroup();
		//create olat resource for the security group
		OLATResource ownerGroupResource =  resourceManager.createOLATResourceInstance(ownerGroup);
		resourceManager.saveOLATResource(ownerGroupResource);
		// member of this group may modify member's membership
		securityManager.createAndPersistPolicyWithResource(ownerGroup, Constants.PERMISSION_ACCESS, map.getOlatResource());
		securityManager.addIdentityToSecurityGroup(author, ownerGroup);
		return ownerGroup;
	}

	/**
	 * Add or update a restriction to the collection of artefacts for a given structure element
	 * @param structure
	 * @param artefactType
	 * @param restriction
	 * @param amount
	 */
	public void addCollectRestriction(PortfolioStructure structure, String artefactType, String restriction, int amount) {
		if(structure == null) throw new NullPointerException("Structure cannot be null");
		
		EPStructureElement structEl = (EPStructureElement)structure;
		List<CollectRestriction> restrictions = structEl.getCollectRestrictions();

		CollectRestriction cr = new CollectRestriction();
		cr.setArtefactType(artefactType);
		cr.setRestriction(restriction);
		cr.setAmount(amount);
		restrictions.add(cr);
	}
	
	protected void submitMap(EPStructuredMap map) {
		map.setStatus(StructureStatusEnum.CLOSED);
		map.setReturnDate(new Date());
		dbInstance.updateObject(map);
	}

	public void savePortfolioStructure(PortfolioStructure portfolioStructure) {
		if(portfolioStructure instanceof PersistentObject) {
			PersistentObject persistentStructure = (PersistentObject)portfolioStructure;
			if(persistentStructure.getKey() == null) {
				dbInstance.saveObject(portfolioStructure.getOlatResource());
				dbInstance.saveObject(portfolioStructure);
			} else {
				dbInstance.updateObject(portfolioStructure);
			}
		}
	}
	

	private static class KeyStructureToStructureLinkComparator implements Comparator<EPStructureToStructureLink>, Serializable {

		private static final long serialVersionUID = 366101659547497002L;

		public KeyStructureToStructureLinkComparator() {
			//
		}
		
		@Override
		public int compare(EPStructureToStructureLink o1, EPStructureToStructureLink o2) {
			if(o1 == null) return -1;
			if(o2 == null) return 1;
			
			PortfolioStructure ps1 = o1.getChild();
			PortfolioStructure ps2 = o2.getChild();
			if(ps1 instanceof EPStructureElement && ps2 instanceof EPStructureElement) {
				EPStructureElement eps1 = (EPStructureElement)ps1;
				EPStructureElement eps2 = (EPStructureElement)ps2;
				
				Long t1 = eps1.getStructureElSource() == null ? eps1.getKey() : eps1.getStructureElSource();
				Long t2 = eps2.getStructureElSource() == null ? eps2.getKey() : eps2.getStructureElSource();
				
				if(t1 == null) return -1;
				if(t2 == null) return 1;
				return t1.compareTo(t2);
			}
			return -1;
		}
	}


	protected boolean setReflexionForArtefactToStructureLink(AbstractArtefact artefact, PortfolioStructure structure, String reflexion) {
		EPStructureElement structureEl = (EPStructureElement)dbInstance.loadObject((EPStructureElement)structure);
		List<EPStructureToArtefactLink> links = structureEl.getInternalArtefacts();
		boolean changed = false;
		for (EPStructureToArtefactLink epStructureToArtefactLink : links) {
			if (epStructureToArtefactLink.getArtefact().getKey().equals(artefact.getKey())){
				epStructureToArtefactLink.setReflexion(reflexion);
				if(epStructureToArtefactLink.getKey() == null) {
					dbInstance.saveObject(epStructureToArtefactLink);
				} else {
					dbInstance.updateObject(epStructureToArtefactLink);
				}
				changed = true;
				break;
			}
		}
		//savePortfolioStructure(structure);
		return changed;
	}
	
	protected String getReflexionForArtefactToStructureLink(AbstractArtefact artefact, PortfolioStructure structure){
		if (structure == null) return null;
		EPStructureElement structureEl = (EPStructureElement)dbInstance.loadObject((EPStructureElement)structure);
		List<EPStructureToArtefactLink> links = structureEl.getInternalArtefacts();
		for (EPStructureToArtefactLink epStructureToArtefactLink : links) {
			if (epStructureToArtefactLink.getArtefact().getKey().equals(artefact.getKey())){
				return epStructureToArtefactLink.getReflexion();
			}
		}
		return null;
	}
}
