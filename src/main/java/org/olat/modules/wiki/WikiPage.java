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
 * <p>
 */

package org.olat.modules.wiki;

import org.olat.modules.wiki.gui.components.wikiToHtml.FilterUtil;

/**
 * Description:<br>
 * Data object for a single wiki page with metadata.
 * <P>
 * Initial Date: May 7, 2006 <br>
 * 
 * @author guido
 */
public class WikiPage {
	public static final String WIKI_INDEX_PAGE = "Index";
	public static final String WIKI_MENU_PAGE = "Menu";
	public static final String WIKI_RECENT_CHANGES_PAGE = "O_recent_changes";
	public static final String WIKI_A2Z_PAGE = "O_a_to_z";
	public static final String WIKI_ERROR = "O_error";
	private String content = "";
	private String pageName;
	private String pageId;
	private long forumKey = 0;
	private boolean dirty = false;
	private int version = 0;
	private String updateComment = "";
	private long initalAuthor = 0;
	private long modifyAuthor = 0;
	private long modificationTime = 0;
	private long creationTime;
	// TODO:gs viewCout does not work properly as it would save the property file
	// on each click which would invalidate
	// the cache for each page view which is nonsense.
	// remove or find better solution which works also in a cluster cache scenario
	private int viewCount = 0;

	/**
	 * @param id
	 * @param name
	 */
	protected WikiPage(String name) {
		this.pageName = FilterUtil.normalizeWikiLink(name);
		this.pageId = WikiManager.generatePageId(pageName);
	}

	public String getContent() {
		return content;
	}

	protected void setContent(String content) {
		this.content = content;
		dirty = true;
	}

	protected long getForumKey() {
		return forumKey;
	}

	protected void setForumKey(String forumKey) {
		this.forumKey = Long.parseLong(forumKey);
	}

	public void setForumKey(long forumKey) {
		this.forumKey = forumKey;
	}

	/**
	 * used by velocity
	 * 
	 * @return the pageName
	 */
	public String getPageName() {
		return pageName;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean b) {
		this.dirty = b;
	}

	/**
	 * @return a calculated one way hash used for persisting the pages to the
	 *         filesystem.
	 */
	protected String getPageId() {
		if (pageId == null) pageId = WikiManager.generatePageId(pageName);
		return pageId;
	}

	/**
	 * 
	 * @return
	 */
	public int getVersion() {
		return this.version;
	}

	protected void incrementVersion() {
		this.version++;
	}

	public long getInitalAuthor() {
		return initalAuthor;
	}

	protected void setInitalAuthor(String initalAuthor) {
		this.initalAuthor = Long.parseLong(initalAuthor);
	}

	protected void setInitalAuthor(long initalAuthor) {
		this.initalAuthor = initalAuthor;
	}

	public long getModificationTime() {
		return modificationTime;
	}

	protected void setModificationTime(String modificationTime) {
		this.modificationTime = Long.parseLong(modificationTime);
	}

	protected void setModificationTime(long modificationTime) {
		this.modificationTime = modificationTime;
	}

	public long getModifyAuthor() {
		return modifyAuthor;
	}

	protected void setModifyAuthor(String modifyAuthor) {
		this.modifyAuthor = Long.parseLong(modifyAuthor);
	}

	protected void setModifyAuthor(long modifyAuthor) {
		this.modifyAuthor = modifyAuthor;
	}

	public int getViewCount() {
		return viewCount;
	}

	protected void incrementViewCount() {
		this.viewCount++;
	}

	protected void setViewCount(String viewCount) {
		this.viewCount = Integer.parseInt(viewCount);
	}

	public void setViewCount(int viewCount) {
		this.viewCount = viewCount;
	}

	public long getCreationTime() {
		return creationTime;
	}

	protected void setCreationTime(String creationTime) {
		this.creationTime = Long.parseLong(creationTime);
	}

	protected void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	protected void setVersion(String version) {
		this.version = Integer.parseInt(version);
	}

	public String getUpdateComment() {
		return updateComment;
	}

	protected void setUpdateComment(String updateComment) {
		this.updateComment = updateComment;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("PageName: ").append(this.pageName).append("\n");
		sb.append("pageId: ").append(this.pageId).append("\n");
		sb.append("forumKey: ").append(this.forumKey).append("\n");
		sb.append("version: ").append(this.version).append("\n");
		sb.append("modifyAuthor: ").append(this.modifyAuthor).append("\n");
		sb.append("modificationTime: ").append(this.modificationTime).append("\n");
		sb.append("creationTime: ").append(this.creationTime).append("\n");
		sb.append("viewCount: ").append(this.viewCount).append("\n");
		sb.append("up-comment: ").append(this.updateComment).append("\n");
		sb.append("\n");
		return sb.toString();
	}

	public void resetCopiedPage() {
		this.version = 0;
		this.modificationTime = 0;
		this.forumKey = 0;
		this.modifyAuthor = 0;
		this.updateComment = "";
		this.viewCount = 0;
	}
}