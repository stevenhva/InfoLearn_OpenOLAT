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
* <p>
*/

package org.olat.restapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.olat.basesecurity.Authentication;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.util.Encoder;
import org.olat.restapi.support.vo.AuthenticationVO;
import org.olat.test.OlatJerseyTestCase;

/**
 * 
 * Description:<br>
 * Test the authentication management per user
 * 
 * <P>
 * Initial Date:  15 apr. 2010 <br>
 * @author srosse, stephane.rosse@frentix.com
 */
public class UserAuthenticationMgmtTest extends OlatJerseyTestCase {
	
	private RestConnection conn;
	
	@Before
	public void startup() {
		conn = new RestConnection();
	}
	
  @After
	public void tearDown() throws Exception {
		try {
			if(conn != null) {
				conn.shutdown();
			}
		} catch (Exception e) {
      e.printStackTrace();
      throw e;
		}
	}
	
	@Test
	public void testGetAuthentications() throws IOException, URISyntaxException {
		assertTrue(conn.login("administrator", "openolat"));
		
		URI request = UriBuilder.fromUri(getContextURI()).path("/users/administrator/auth").build();
		HttpGet method = conn.createGet(request, MediaType.APPLICATION_JSON, true);
		HttpResponse response = conn.execute(method);
		assertEquals(200, response.getStatusLine().getStatusCode());
		InputStream body = response.getEntity().getContent();
		List<AuthenticationVO> vos = parseAuthenticationArray(body);
		assertNotNull(vos);
		assertFalse(vos.isEmpty());
		
	}
	
	@Test
	public void testCreateAuthentications() throws IOException, URISyntaxException {
		BaseSecurity baseSecurity = BaseSecurityManager.getInstance();
		Identity adminIdent = baseSecurity.findIdentityByName("administrator");
		try {
			Authentication refAuth = baseSecurity.findAuthentication(adminIdent, "REST-API");
			if(refAuth != null) {
				baseSecurity.deleteAuthentication(refAuth);
			}
		} catch(Exception e) {
			//
		}
		DBFactory.getInstance().commitAndCloseSession();
		
		assertTrue(conn.login("administrator", "openolat"));

		AuthenticationVO vo = new AuthenticationVO();
		vo.setAuthUsername("administrator");
		vo.setIdentityKey(adminIdent.getKey());
		vo.setProvider("REST-API");
		vo.setCredential("credentials");
		
		URI request = UriBuilder.fromUri(getContextURI()).path("/users/administrator/auth").build();
		HttpPut method = conn.createPut(request, MediaType.APPLICATION_JSON, true);
		conn.addJsonEntity(method, vo);

    HttpResponse response = conn.execute(method);
    assertTrue(response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 201);
    AuthenticationVO savedAuth = conn.parse(response, AuthenticationVO.class);
    Authentication refAuth = baseSecurity.findAuthentication(adminIdent, "REST-API");
    

		assertNotNull(refAuth);
		assertNotNull(refAuth.getKey());
		assertTrue(refAuth.getKey().longValue() > 0);
		assertNotNull(savedAuth);
		assertNotNull(savedAuth.getKey());
		assertTrue(savedAuth.getKey().longValue() > 0);
		assertEquals(refAuth.getKey(), savedAuth.getKey());
		assertEquals(refAuth.getAuthusername(), savedAuth.getAuthUsername());
		assertEquals(refAuth.getIdentity().getKey(), savedAuth.getIdentityKey());
		assertEquals(refAuth.getProvider(), savedAuth.getProvider());
		assertEquals(refAuth.getCredential(), savedAuth.getCredential());
	}
	
	@Test
	public void testDeleteAuthentications() throws IOException, URISyntaxException {
		assertTrue(conn.login("administrator", "openolat"));
		
		//create an authentication token
		BaseSecurity baseSecurity = BaseSecurityManager.getInstance();
		Identity adminIdent = baseSecurity.findIdentityByName("administrator");
		Authentication authentication = baseSecurity.createAndPersistAuthentication(adminIdent, "REST-A-2", "administrator", "credentials", Encoder.Algorithm.sha512);
		assertTrue(authentication != null && authentication.getKey() != null && authentication.getKey().longValue() > 0);
		DBFactory.getInstance().intermediateCommit();
		
		//delete an authentication token
		URI request = UriBuilder.fromUri(getContextURI()).path("/users/administrator/auth/" + authentication.getKey()).build();
		HttpDelete method = conn.createDelete(request, MediaType.APPLICATION_XML);
		HttpResponse response = conn.execute(method);
		assertEquals(200, response.getStatusLine().getStatusCode());
		
		
		Authentication refAuth = baseSecurity.findAuthentication(adminIdent, "REST-A-2");
		assertNull(refAuth);
	}
	
	private List<AuthenticationVO> parseAuthenticationArray(InputStream body) {
		try {
			ObjectMapper mapper = new ObjectMapper(jsonFactory); 
			return mapper.readValue(body, new TypeReference<List<AuthenticationVO>>(){/* */});
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}