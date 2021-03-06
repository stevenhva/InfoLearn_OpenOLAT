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

package org.olat.repository.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.olat.core.dispatcher.DispatcherModule;
import org.olat.core.gui.ShortName;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.table.ColumnDescriptor;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.table.TableMultiSelectEvent;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.UserConstants;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.util.Util;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryTableModel;
import org.olat.repository.SearchForm;
import org.olat.repository.SearchRepositoryEntryParameters;

/**
*  Description:
*  This workflow is used to search for repository entries. The workflow has two steps: 
*  1) a search form and 2) the results list. After calling the constructor nothing 
*  happens. The views must be initialized manually. Using some public methods the 
*  desired view can be triggered.
*  The workflow can be limited to a specific repository type. 
*  Onother option is to set the set the enableSearchforAllReferencalbeInSearchForm
*  In this case, the search workflow can be used to find repository entires that can be
*  referenced by the user. 
*  Some doSearch... methods allow the presentation of lists without using the search
*  form at all.
*
* @author Felix Jost
*/
public class RepositorySearchController extends BasicController implements Activateable2 {

	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(RepositoryManager.class);

	protected VelocityContainer vc;
	protected Translator translator;
	protected RepositoryTableModel repoTableModel;
	protected SearchForm searchForm;
	protected TableController tableCtr;
	
	private Link backLink, cancelButton;
	private RepositoryEntry selectedEntry;
	private List<RepositoryEntry> selectedEntries;
	private Can enableSearchforAllInSearchForm;
	private Link loginLink;
	private SearchType searchType;
	private RepositoryEntryFilter filter;
	
	
	/**
	 * A generic search controller.
	 * @param selectButtonLabel
	 * @param ureq
	 * @param myWControl
	 * @param withCancel
	 * @param enableDirectLaunch
	 */
	public RepositorySearchController(String selectButtonLabel, UserRequest ureq, WindowControl myWControl,
			boolean withCancel, boolean enableDirectLaunch, boolean multiSelect) {
		//fxdiff VCRP-10: repository search with type filter
		super(ureq, myWControl, Util.createPackageTranslator(RepositoryManager.class, ureq.getLocale()));
		init(selectButtonLabel, ureq, withCancel, enableDirectLaunch, multiSelect, new String[]{}, null);
	}
	
	/**
	 * A generic search controller.
	 * @param selectButtonLabel
	 * @param ureq
	 * @param myWControl
	 * @param withCancel
	 * @param enableDirectLaunch
	 * @param limitType
	 */
	public RepositorySearchController(String selectButtonLabel, UserRequest ureq, WindowControl myWControl,
			boolean withCancel, boolean enableDirectLaunch, boolean multiSelect, String limitType) {
		this(selectButtonLabel, ureq,  myWControl,  withCancel,  enableDirectLaunch, multiSelect, new String[]{limitType}, null);
	}

	public RepositorySearchController(String selectButtonLabel, UserRequest ureq, WindowControl myWControl,
			boolean withCancel, boolean enableDirectLaunch, boolean multiSelect, String[] limitTypes, RepositoryEntryFilter filter) {
		//fxdiff VCRP-10: repository search with type filter
		super(ureq, myWControl, Util.createPackageTranslator(RepositoryManager.class, ureq.getLocale()));
		init(selectButtonLabel, ureq, withCancel, enableDirectLaunch, multiSelect, limitTypes, filter);
	}
	
	/**
	 * @param myWControl
	 */
	public RepositorySearchController(UserRequest ureq, WindowControl myWControl) {
		//fxdiff VCRP-10: repository search with type filter
		super(ureq, myWControl, Util.createPackageTranslator(RepositoryManager.class, ureq.getLocale()));
	}

	private void init(String selectButtonLabel, UserRequest ureq, boolean withCancel, boolean enableDirectLaunch, boolean multiSelect,
			String[] limitTypes, RepositoryEntryFilter filter) {
		
		this.filter = filter;
		translator = Util.createPackageTranslator(RepositoryManager.class, ureq.getLocale());
		Roles roles = ureq.getUserSession().getRoles();
		
		vc = new VelocityContainer("reposearch", VELOCITY_ROOT + "/search.html", translator, this);

		removeAsListenerAndDispose(searchForm);
		searchForm = new SearchForm(ureq, getWindowControl(), withCancel, roles.isOLATAdmin(), limitTypes);
		listenTo(searchForm);
		
		searchForm.setVisible(false);
		vc.put("searchform",searchForm.getInitialComponent());
		
		TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		if (selectButtonLabel != null) {
			tableConfig.setPreferencesOffered(true, "repositorySearchResult_v2");
		}
		
		//fxdiff VCRP-10: repository search with type filter
		String filterTitle = translator.translate("search.filter.type");
		String noFilterOption = translator.translate("search.filter.showAll");
		tableCtr = new TableController(tableConfig, ureq, getWindowControl(), null, null, filterTitle, noFilterOption, true, translator);
		if(multiSelect) {
			tableCtr.setMultiSelect(multiSelect);
			tableCtr.addLabeledMultiSelectAction(selectButtonLabel, "mselect");
		}
		listenTo(tableCtr);
		
		repoTableModel = new RepositoryTableModel(translator);
		ColumnDescriptor sortCol = repoTableModel.addColumnDescriptors(tableCtr, selectButtonLabel, enableDirectLaunch);
		tableCtr.setTableDataModel(repoTableModel);
		tableCtr.setSortColumn(sortCol, true);
		vc.put("repotable", tableCtr.getInitialComponent());

		vc.contextPut("isAuthor", Boolean.valueOf(roles.isAuthor()));
		vc.contextPut("withCancel", new Boolean(withCancel));
		enableBackToSearchFormLink(false); // default, must be enabled explicitly
		enableSearchforAllXXAbleInSearchForm(null); // default
		putInitialPanel(vc);
	}

	/**
	 * @param enableBack true: back link is shown, back goes to search form; false; no back link
	 */
	public void enableBackToSearchFormLink(boolean enableBack) {
		vc.contextPut("withBack", new Boolean(enableBack));
	}
	
	@Override
	//fxdiff BAKS-7 Resume function
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(entries == null || entries.isEmpty()) return;
		
		String subType = entries.get(0).getOLATResourceable().getResourceableTypeName();
		if(RepositoryEntry.class.getSimpleName().equals(subType)) {
			//activate details
			Long resId = entries.get(0).getOLATResourceable().getResourceableId();
			selectedEntry = RepositoryManager.getInstance().lookupRepositoryEntry(resId);
			fireEvent(ureq, new Event(RepositoryTableModel.TABLE_ACTION_SELECT_LINK));
		}
	}

	/**
	 * @param enable true: searches done by the search form will find all resources
	 * that are referencable/copyable by the current user; false: searches done by the search 
	 * form will find all resources that have at least BAR setting in the BARG configuration
	 * list
	 */
	public void enableSearchforAllXXAbleInSearchForm(Can enable) {
		enableSearchforAllInSearchForm = enable;
	}
	
	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == cancelButton) {
			fireEvent(ureq, Event.CANCELLED_EVENT);
			return;
		} else if (source == backLink){
			displaySearchForm();
			return;
		}else if (source == loginLink){
			DispatcherModule.redirectToDefaultDispatcher(ureq.getHttpResp());
		}
	}
	
	private void filterRepositoryEntries(List<RepositoryEntry> entries) {
		if(filter != null && entries != null && !entries.isEmpty()) {
			for(Iterator<RepositoryEntry> entryIt=entries.iterator(); entryIt.hasNext(); ) {
				if(!filter.accept(entryIt.next())) {
					entryIt.remove();
				}
			}
		}
	}

	/**
	 * Implementation normal search: find repo entries that are public
	 * using the values from the form
	 * @param ureq
	 */
	protected void doSearch(UserRequest ureq, String limitType, boolean updateFilters) {
		doSearch(ureq, limitType, false, updateFilters);
	}
	
	protected void doSearch(UserRequest ureq, String limitType, boolean onlyOwner, boolean updateFilters) {
		searchType = SearchType.searchForm;
		RepositoryManager rm = RepositoryManager.getInstance();
		Set<String> s = searchForm.getRestrictedTypes();
		List<String> restrictedTypes;
		if(limitType != null) {
			restrictedTypes = Collections.singletonList(limitType);
		} else {
			restrictedTypes = (s == null) ? null : new ArrayList<String>(s);
		}
		
		String author = searchForm.getAuthor();
		String displayName = searchForm.getDisplayName();
		String description = searchForm.getDescription();

		SearchRepositoryEntryParameters params =
				new SearchRepositoryEntryParameters(displayName, author, description,
						restrictedTypes, getIdentity(), ureq.getUserSession().getRoles(),
						getIdentity().getUser().getProperty(UserConstants.INSTITUTIONALNAME, null));
		params.setExternalId(searchForm.getExternalId());
		params.setExternalRef(searchForm.getExternalRef());
		params.setOnlyOwnedResources(onlyOwner);
		List<RepositoryEntry> entries = rm.genericANDQueryWithRolesRestriction(params, 0, -1, true);		
		filterRepositoryEntries(entries);
		repoTableModel.setObjects(entries);
		if(updateFilters) {
			updateFilters(entries, null);
		}
		tableCtr.modelChanged();
		displaySearchResults(ureq);
	}

	/**
	 * Implementation of referencable search: find repo entries that are 
	 * owned by the uer or set to referencable and have at lease BA settings
	 * @param ureq
	 */
	private void doSearchAllReferencables(UserRequest ureq, String limitType, boolean updateFilters) {
		searchType = SearchType.searchForm;
		RepositoryManager rm = RepositoryManager.getInstance();
		List<String> restrictedTypes;
		if(limitType != null) {
			restrictedTypes = Collections.singletonList(limitType);
		} else {
			Set<String> s = searchForm.getRestrictedTypes();
			restrictedTypes = (s == null) ? null : new ArrayList<String>(s);
		}
		Roles roles = ureq.getUserSession().getRoles();
		Identity ident = ureq.getIdentity();
		String name = searchForm.getDisplayName();
		String author = searchForm.getAuthor();
		String desc = searchForm.getDescription();
		
		List<RepositoryEntry> entries;
		if(searchForm.isAdminSearch()) {
			entries = rm.queryResourcesLimitType(null, restrictedTypes, name, author, desc, true, false);
		} else if(enableSearchforAllInSearchForm == Can.referenceable){
			entries = rm.queryReferencableResourcesLimitType(ident, roles, restrictedTypes, name, author, desc);
		} else if(enableSearchforAllInSearchForm == Can.copyable){
			entries = rm.queryCopyableResourcesLimitType(ident, roles, restrictedTypes, name, author, desc);
		} else {
			entries = new ArrayList<RepositoryEntry>();
		}
		filterRepositoryEntries(entries);
		repoTableModel.setObjects(entries);
		if(updateFilters) {
			updateFilters(entries, null);
		}
		tableCtr.modelChanged();
		displaySearchResults(ureq);
	}

	
	/**
	 * Do search for all resources that the user can reference either because he 
	 * is the owner of the resource or because he has author rights and the resource
	 * is set to at least BA in the BARG settings and the resource has the flag
	 * 'canReference' set to true.
	 * @param owner The current identity
	 * @param limitType The search limitation a specific type
	 * @param roles The users roles
	 */
	public void doSearchForReferencableResourcesLimitType(Identity owner, String limitType, Roles roles) {
		doSearchForReferencableResourcesLimitType(owner, limitType.equals("")?null:new String[] {limitType}, roles);
	}

	/**
	 * Do search for all resources that the user can reference either because he 
	 * is the owner of the resource or because he has author rights and the resource
	 * is set to at least BA in the BARG settings and the resource has the flag
	 * 'canReference' set to true.
	 * @param owner The current identity
	 * @param limitTypes List of Types to limit the search
	 * @param roles The users roles
	 */
	public void doSearchForReferencableResourcesLimitType(Identity owner, String[] limitTypes, Roles roles) {
		RepositoryManager rm = RepositoryManager.getInstance();
		List<String> restrictedTypes = new ArrayList<String>();
		if(limitTypes == null) {
			restrictedTypes = null;
		}
		else {
			restrictedTypes.addAll(Arrays.asList(limitTypes));
		}
		List<RepositoryEntry> entries = rm.queryReferencableResourcesLimitType(owner, roles, restrictedTypes, null, null, null);
		filterRepositoryEntries(entries);
		repoTableModel.setObjects(entries);
		tableCtr.setFilters(null, null);
		tableCtr.modelChanged();
		displaySearchResults(null);
	}
	
	/**
	 * Do search for all resources that the user can copy either because he 
	 * is the owner of the resource or because he has author rights and the resource
	 * is set to at least BA in the BARG settings and the resource has the flag
	 * 'canCopy' set to true.
	 * @param owner The current identity
	 * @param limitTypes List of Types to limit the search
	 * @param roles The users roles
	 */
	public void doSearchForCopyableResourcesLimitType(Identity owner, String[] limitTypes, Roles roles) {
		RepositoryManager rm = RepositoryManager.getInstance();
		List<String> restrictedTypes = new ArrayList<String>();
		if(limitTypes == null) {
			restrictedTypes = null;
		}
		else {
			restrictedTypes.addAll(Arrays.asList(limitTypes));
		}
		List<RepositoryEntry> entries = rm.queryCopyableResourcesLimitType(owner, roles, restrictedTypes, null, null, null);
		filterRepositoryEntries(entries);
		repoTableModel.setObjects(entries);
		tableCtr.setFilters(null, null);
		tableCtr.modelChanged();
		displaySearchResults(null);
	}
	
	/**
	 * Search for all resources where identity is owner.
	 * 
	 * @param owner
	 */
	//fxdiff VCRP-10: repository search with type filter
	public void doSearchByOwner(Identity owner) {
		doSearchByOwnerLimitTypeInternal(owner, new String[] {}, true);
	}

	/**
	 * Do search for all resources of a given type where identity is owner.
	 * @param owner
	 * @param limitType
	 */
	//fxdiff VCRP-10: repository search with type filter
	public void doSearchByOwnerLimitType(Identity owner, String limitType) {
		doSearchByOwnerLimitTypeInternal(owner, new String[]{limitType}, true);
	}
	
	public void doSearchByOwnerLimitType(Identity owner, String[] limitTypes) {
		doSearchByOwnerLimitTypeInternal(owner, limitTypes, true);
	}
	
	//fxdiff VCRP-10: repository search with type filter
	private void doSearchByOwnerLimitTypeInternal(Identity owner, String[] limitTypes, boolean updateFilters) {
		searchType = SearchType.byOwner;
		RepositoryManager rm = RepositoryManager.getInstance();
		List<RepositoryEntry> entries = rm.queryByOwner(owner, limitTypes);
		filterRepositoryEntries(entries);
		if(updateFilters) {
			updateFilters(entries, owner);
		}
		repoTableModel.setObjects(entries);
		tableCtr.modelChanged(updateFilters);
		displaySearchResults(null);
	}
	
	/**
	 * Do search for all resources of a given type where identity is owner.
	 * @param owner
	 * @param access
	 */
	public void doSearchByOwnerLimitAccess(Identity owner) {
		RepositoryManager rm = RepositoryManager.getInstance();
		List<RepositoryEntry> entries = rm.queryByOwnerLimitAccess(owner, RepositoryEntry.ACC_USERS, Boolean.TRUE);
		filterRepositoryEntries(entries);
		repoTableModel.setObjects(entries);
		tableCtr.setFilters(null, null);
		tableCtr.modelChanged();
		displaySearchResults(null);
	}

	protected void doSearchByTypeLimitAccess(String restrictedType, UserRequest ureq) {
		doSearchByTypeLimitAccess(new String[]{restrictedType}, ureq);
	}
	
	/**
	 * Used by repository main controller to execute predefined searches.
	 * 
	 * @param restrictedTypes
	 * @param ureq
	 */
	protected void doSearchByTypeLimitAccess(String[] restrictedTypes, UserRequest ureq) {
		searchType = null;
		RepositoryManager rm = RepositoryManager.getInstance();
		List<String> types = Arrays.asList(restrictedTypes);
		List<RepositoryEntry> entries = rm.queryByTypeLimitAccess(ureq.getIdentity(), ureq.getUserSession().getRoles(), types);
		filterRepositoryEntries(entries);
		repoTableModel.setObjects(entries);
		tableCtr.setFilters(null, null);
		tableCtr.modelChanged();
		displaySearchResults(ureq);
	}

	private void doSearchById(Long id) {
		RepositoryManager rm = RepositoryManager.getInstance();
		RepositoryEntry entry = rm.lookupRepositoryEntry(id);
		List<RepositoryEntry> entries = new ArrayList<RepositoryEntry>(1);
		if (entry != null) entries.add(entry);
		filterRepositoryEntries(entries);
		repoTableModel.setObjects(entries);
		tableCtr.modelChanged();
		displaySearchResults(null);
	}

	protected void doSearchMyCoursesStudent(UserRequest ureq){
		doSearchMyCoursesStudent(ureq, null, true);
	}
		
	protected void doSearchMyCoursesStudent(UserRequest ureq, String limitType, boolean updateFilters) {
		searchType = SearchType.myAsStudent;
		RepositoryManager rm = RepositoryManager.getInstance();
		List<RepositoryEntry> entries = rm.getLearningResourcesAsStudent(ureq.getIdentity(), 0, -1);
		//fxdiff VCRP-10: repository search with type filter
		filterRepositoryEntries(entries);
		doSearchMyRepositoryEntries(ureq, entries, limitType, updateFilters);
	}
	
	protected void doSearchMyCoursesTeacher(UserRequest ureq){
		doSearchMyCoursesTeacher(ureq, null, true);
	}
	
	protected void doSearchMyCoursesTeacher(UserRequest ureq, String limitType, boolean updateFilters) {
		searchType = SearchType.myAsTeacher;
		RepositoryManager rm = RepositoryManager.getInstance();
		List<RepositoryEntry> entries = rm.getLearningResourcesAsTeacher(ureq.getIdentity(), 0, -1);
		filterRepositoryEntries(entries);
		//fxdiff VCRP-10: repository search with type filter
		doSearchMyRepositoryEntries(ureq, entries, limitType, updateFilters);
	}

	//fxdiff VCRP-10: repository search with type filter
	protected void doSearchMyRepositoryEntries(UserRequest ureq, List<RepositoryEntry> entries, String limitType, boolean updateFilters) {
		if(limitType != null) {
			for(Iterator<RepositoryEntry> it=entries.iterator(); it.hasNext(); ) {
				RepositoryEntry next = it.next();
				if(!next.getOlatResource().getResourceableTypeName().equals(limitType)) {
					it.remove();
				}
			}	
		}
		repoTableModel.setObjects(entries);
		if(updateFilters) {
			updateFilters(entries, null);
		}
		tableCtr.modelChanged();
		displaySearchResults(ureq);
	}
	
	protected void updateFilters(List<RepositoryEntry> entries, Identity owner) {
		List<ShortName> restrictedTypes = new ArrayList<ShortName>();
		Set<String> uniqueTypes = new HashSet<String>();
		for(RepositoryEntry entry:entries) {
			if(entry.getOlatResource() == null) continue;//no red screen for that
			String type = entry.getOlatResource().getResourceableTypeName();
			if(type != null && !uniqueTypes.contains(type)) {
				String label = translate(type);
				restrictedTypes.add(new TypeFilter(type, label, owner));
				uniqueTypes.add(type);
			}
		}
		if(restrictedTypes.size() > 1) {
			tableCtr.setFilters(restrictedTypes, null);
		} else {
			tableCtr.setFilters(null, null);
		}
	}
	
	/**
	 * @return Returns the selectedEntry.
	 */
	public RepositoryEntry getSelectedEntry() {
		return selectedEntry;
	}
	
	public List<RepositoryEntry> getSelectedEntries() {
		if(selectedEntries == null && selectedEntry != null) {
			return Collections.singletonList(selectedEntry);
		}
		return selectedEntries;
	}

	/**
	 * Will reset the controller to display the search form again.
	 */
	public void displaySearchForm() {
		searchForm.setVisible(true);
		searchForm.setAdminSearch(false);
		vc.setPage(VELOCITY_ROOT + "/search.html");
	}
	
	/**
	 * Will reset the controller to display the search form again.
	 */
	public void displayAdminSearchForm() {
		searchForm.setVisible(true);
		searchForm.setAdminSearch(true);
		vc.setPage(VELOCITY_ROOT + "/search.html");
	}
	
	/**
	 * Present the search results page.
	 */
	public void displaySearchResults(UserRequest ureq) {
		searchForm.setVisible(false);
		if (repoTableModel.getRowCount() == 0) vc.contextPut("hasResults", Boolean.FALSE);
		else vc.contextPut("hasResults", Boolean.TRUE);
		backLink = LinkFactory.createLinkBack(vc, this);
		vc.setPage(VELOCITY_ROOT + "/results.html");
		//REVIEW:pb why can ureq be null here?
		vc.contextPut("isGuest", (ureq != null) ? new Boolean(ureq.getUserSession().getRoles().isGuestOnly()) : Boolean.FALSE);
		loginLink = LinkFactory.createLink("repo.login", vc, this);
		cancelButton = LinkFactory.createButton("cancel", vc, this);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest urequest, Controller source, Event event) {
		if (source == tableCtr) { // process table actions
			//fxdiff VCRP-10: repository search with type filter
			if(event instanceof TableEvent) {
				TableEvent te = (TableEvent)event;
				selectedEntry =  (RepositoryEntry)tableCtr.getTableDataModel().getObject(te.getRowId());
				selectedEntries = null;
				if (te.getActionId().equals(RepositoryTableModel.TABLE_ACTION_SELECT_ENTRY)) {
					fireEvent(urequest, new Event(RepositoryTableModel.TABLE_ACTION_SELECT_ENTRY));
					return;
				} else if (te.getActionId().equals(RepositoryTableModel.TABLE_ACTION_SELECT_LINK)) {
					fireEvent(urequest, new Event(RepositoryTableModel.TABLE_ACTION_SELECT_LINK));
					return;
				}
			} else if (event instanceof TableMultiSelectEvent) {
				TableMultiSelectEvent mse = (TableMultiSelectEvent)event;	
				if(!mse.getSelection().isEmpty()) {
					selectedEntry = null;
					selectedEntries = repoTableModel.getObjects(mse.getSelection());
					fireEvent(urequest, new Event(RepositoryTableModel.TABLE_ACTION_SELECT_ENTRIES));
				}
			} else if (TableController.EVENT_FILTER_SELECTED.equals(event)) {
				TypeFilter typeFilter = (TypeFilter) tableCtr.getActiveFilter();
				if(searchType == SearchType.byOwner) {
					doSearchByOwnerLimitTypeInternal(typeFilter.getOwner(), new String[]{typeFilter.getType()}, false);
				} else if(searchType == SearchType.myAsStudent) {
					doSearchMyCoursesStudent(urequest, typeFilter.getType(), false);
				} else if(searchType == SearchType.myAsTeacher) {
					doSearchMyCoursesTeacher(urequest, typeFilter.getType(), false);
				} else if(searchType == SearchType.searchForm) {
					if(enableSearchforAllInSearchForm != null) {
						doSearchAllReferencables(urequest, typeFilter.getType(), false);
					} else {
						doSearch(urequest, typeFilter.getType(), false);
					}
				}
			} else if (TableController.EVENT_NOFILTER_SELECTED.equals(event)) {
				if(searchType == SearchType.byOwner) {
					doSearchByOwnerLimitTypeInternal(getIdentity(), new String[]{}, false);
				} else if(searchType == SearchType.myAsStudent) {
					doSearchMyCoursesStudent(urequest);
				} else if(searchType == SearchType.myAsTeacher) {
					doSearchMyCoursesTeacher(urequest);
				} else if(searchType == SearchType.searchForm) {
					if(enableSearchforAllInSearchForm != null) {
						doSearchAllReferencables(urequest, null, false);
					} else {
						doSearch(urequest, null, false);
					}
				}
			}
		} 
		else if (event instanceof EntryChangedEvent) { // remove deleted entry
			EntryChangedEvent ecv = (EntryChangedEvent)event;
			if (ecv.getChange() == EntryChangedEvent.DELETED) {
				List<RepositoryEntry> newEntries = new ArrayList<RepositoryEntry>();
				for (int i = 0; i < repoTableModel.getRowCount(); i++) {
					RepositoryEntry foo = (RepositoryEntry)repoTableModel.getObject(i);
					if (!foo.getKey().equals(ecv.getChangedEntryKey()))
						newEntries.add(foo);
				}
				repoTableModel.setObjects(newEntries);
				tableCtr.modelChanged();
			} else if (ecv.getChange() == EntryChangedEvent.ADDED) {
				doSearchByOwner(urequest.getIdentity());
			}
		}	else if (source == searchForm) { // process search form events
			if (event == Event.DONE_EVENT) {
				if (searchForm.hasId())	{
					doSearchById(searchForm.getId());
				} else if (enableSearchforAllInSearchForm != null) {
					doSearchAllReferencables(urequest, null, true);
				} else {
					doSearch(urequest, null, true);
				}
			} else if (event == Event.CANCELLED_EVENT) {
				fireEvent(urequest, Event.CANCELLED_EVENT);
			}
		}
	}
	
	/**
	 * 
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {
		//
	}
	
	//fxdiff VCRP-10: repository search with type filter
	private class TypeFilter implements ShortName {
		
		private final String type;
		private final String typeName;
		private final Identity owner;

		public TypeFilter(String type, String typeName, Identity owner) {
			this.type = type;
			this.typeName = typeName;
			this.owner = owner;
		}

		public String getType() {
			return type;
		}
		
		public Identity getOwner() {
			return owner;
		}

		@Override
		public String getShortName() {
			return typeName;
		}
		
		@Override
		public int hashCode() {
			return type.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj) {
				return true;
			}
			if(obj instanceof TypeFilter) {
				TypeFilter typeobj = (TypeFilter)obj;
				return type != null && type.equals(typeobj.type);
			}
			return false;
		}
	}
	
	public enum SearchType {
		byOwner,
		myAsStudent,
		myAsTeacher,
		searchForm,
	}
	
	public enum Can {
		referenceable,
		copyable,
		all
	}
}