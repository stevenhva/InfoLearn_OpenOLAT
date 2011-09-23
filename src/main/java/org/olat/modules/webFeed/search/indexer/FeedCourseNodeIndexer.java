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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package org.olat.modules.webFeed.search.indexer;

import java.io.IOException;

import org.olat.core.commons.services.search.OlatDocument;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.filter.Filter;
import org.olat.core.util.filter.FilterFactory;
import org.olat.course.ICourse;
import org.olat.course.nodes.CourseNode;
import org.olat.modules.webFeed.dispatching.Path;
import org.olat.modules.webFeed.managers.FeedManager;
import org.olat.modules.webFeed.models.Feed;
import org.olat.modules.webFeed.models.Item;
import org.olat.modules.webFeed.search.document.FeedItemDocument;
import org.olat.modules.webFeed.search.document.FeedNodeDocument;
import org.olat.repository.RepositoryEntry;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.indexer.OlatFullIndexer;
import org.olat.search.service.indexer.repository.CourseIndexer;
import org.olat.search.service.indexer.repository.course.CourseNodeIndexer;

/**
 * Indexer for feed course nodes
 * 
 * <P>
 * Initial Date: Aug 18, 2009 <br>
 * 
 * @author gwassmann
 */
public abstract class FeedCourseNodeIndexer implements CourseNodeIndexer {

	private static final OLog log = Tracing.createLoggerFor(FeedRepositoryIndexer.class);

	/**
	 * @see org.olat.search.service.indexer.Indexer#checkAccess(org.olat.core.id.context.ContextEntry,
	 *      org.olat.core.id.context.BusinessControl, org.olat.core.id.Identity,
	 *      org.olat.core.id.Roles)
	 */
	public boolean checkAccess(ContextEntry contextEntry, BusinessControl businessControl, Identity identity, Roles roles) {
		return true;
	}

	/**
	 * @see org.olat.search.service.indexer.Indexer#doIndex(org.olat.search.service.SearchResourceContext,
	 *      java.lang.Object, org.olat.search.service.indexer.OlatFullIndexer)
	 */
	public void doIndex(SearchResourceContext searchResourceContext, ICourse course, CourseNode node, OlatFullIndexer indexer)
			throws IOException, InterruptedException {
		RepositoryEntry repositoryEntry = node.getReferencedRepositoryEntry();
		// used for log messages
		String repoEntryName = "*name not available*";
		try {
			repoEntryName = repositoryEntry.getDisplayname();
			if (log.isDebug()) {
				log.info("Indexing: " + repoEntryName);
			}
			Feed feed = FeedManager.getInstance().getFeed(repositoryEntry.getOlatResource());

			// Set the document type, e.g. type.repository.entry.FileResource.BLOG
			SearchResourceContext nodeSearchContext = new SearchResourceContext(searchResourceContext);
			nodeSearchContext.setBusinessControlFor(node);
			nodeSearchContext.setDocumentType(getDocumentType());

			// Create the olatDocument for the feed course node itself
			OlatDocument feedNodeDoc = new FeedNodeDocument(feed, nodeSearchContext);
			indexer.addDocument(feedNodeDoc.getLuceneDocument());
			
			// Make sure images are displayed properly
			String mapperBaseURL = Path.getFeedBaseUri(feed, null, course.getResourceableId(), node.getIdent());
			Filter mediaUrlFilter = FilterFactory.getBaseURLToMediaRelativeURLFilter(mapperBaseURL);

			// Only index items. Feed itself is indexed by RepositoryEntryIndexer.
			for (Item item : feed.getPublishedItems()) {
				OlatDocument itemDoc = new FeedItemDocument(item, nodeSearchContext, mediaUrlFilter);
				indexer.addDocument(itemDoc.getLuceneDocument());
			}
		} catch (NullPointerException e) {
			log.error("Error indexing feed:" + repoEntryName, e);
		}

		// go further, index my child nodes
		CourseIndexer courseIdexer = new CourseIndexer();
		courseIdexer.doIndexCourse(searchResourceContext, course, node, indexer);
	}

	/**
	 * @see org.olat.search.service.indexer.Indexer#getSupportedTypeName()
	 */
	public abstract String getSupportedTypeName();

	/**
	 * @return The I18n key representing the document type
	 */
	protected abstract String getDocumentType();
}