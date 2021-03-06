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
package org.olat.repository;

import java.util.ArrayList;
import java.util.List;

import org.olat.catalog.CatalogEntry;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;

/**
 * 
 * Description:<br>
 * 
 * <P>
 * Initial Date:  15 déc. 2011 <br>
 *
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class SearchRepositoryEntryParameters {
	private String displayName;
	private String author;
	private String desc;
	private List<String> resourceTypes;
	private Identity identity;
	private Roles roles;
	private Boolean marked;
	private String institution;
	private Boolean managed;
	private String externalId;
	private String externalRef;
	private boolean onlyOwnedResources;
	private boolean onlyExplicitMember;
	private List<Long> repositoryEntryKeys;
	private CatalogEntry parentEntry;
	
	public SearchRepositoryEntryParameters() {
		//
	}
	
	public SearchRepositoryEntryParameters(Identity identity, Roles roles, String... resourceTypes) {
		this.identity = identity;
		this.roles = roles;
		addResourceTypes(resourceTypes);
	}
	
	/**
	 * This constructor match exactly the old signature of the genericAND methods
	 * @param displayName
	 * @param author
	 * @param desc
	 * @param resourceTypes
	 * @param identity
	 * @param roles
	 * @param institution
	 */
	public SearchRepositoryEntryParameters(String displayName, String author, String desc, List<String> resourceTypes, Identity identity, Roles roles, String institution) {
		this.displayName = displayName;
		this.author = author;
		this.desc = desc;
		this.resourceTypes = resourceTypes;
		this.identity = identity;
		this.roles = roles;
		this.institution = institution;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public String getAuthor() {
		return author;
	}
	
	public void setAuthor(String author) {
		this.author = author;
	}
	
	public String getDesc() {
		return desc;
	}
	
	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	public Boolean getManaged() {
		return managed;
	}

	public void setManaged(Boolean managed) {
		this.managed = managed;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getExternalRef() {
		return externalRef;
	}

	public void setExternalRef(String externalRef) {
		this.externalRef = externalRef;
	}

	public CatalogEntry getParentEntry() {
		return parentEntry;
	}

	public void setParentEntry(CatalogEntry parentEntry) {
		this.parentEntry = parentEntry;
	}

	public List<String> getResourceTypes() {
		return resourceTypes;
	}
	
	public void setResourceTypes(List<String> resourceTypes) {
		this.resourceTypes = resourceTypes;
	}
	
	public void addResourceTypes(String... resourceTypes) {
		if(this.resourceTypes == null) {
			this.resourceTypes = new ArrayList<String>();
		}
		if(resourceTypes != null) {
			for(String resourceType:resourceTypes) {
				this.resourceTypes.add(resourceType);
			}
		}
	}
	
	public Identity getIdentity() {
		return identity;
	}
	
	public void setIdentity(Identity identity) {
		this.identity = identity;
	}
	
	public Roles getRoles() {
		return roles;
	}
	
	public void setRoles(Roles roles) {
		this.roles = roles;
	}
	
	public Boolean getMarked() {
		return marked;
	}

	public void setMarked(Boolean marked) {
		this.marked = marked;
	}
	
	public String getInstitution() {
		return institution;
	}
	
	public void setInstitution(String institution) {
		this.institution = institution;
	}

	/**
	 * This has effect for authors. Administrator or instituional mangers
	 * has more rights.
	 * @return
	 */
	public boolean isOnlyOwnedResources() {
		return onlyOwnedResources;
	}

	public void setOnlyOwnedResources(boolean onlyOwnedResources) {
		this.onlyOwnedResources = onlyOwnedResources;
	}

	public boolean isOnlyExplicitMember() {
		return onlyExplicitMember;
	}

	public void setOnlyExplicitMember(boolean onlyExplicitMember) {
		this.onlyExplicitMember = onlyExplicitMember;
	}

	public List<Long> getRepositoryEntryKeys() {
		return repositoryEntryKeys;
	}

	public void setRepositoryEntryKeys(List<Long> repositoryEntryKeys) {
		this.repositoryEntryKeys = repositoryEntryKeys;
	}
}
