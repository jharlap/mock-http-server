/**
 *   Copyright 2011 <jharlap@gitub.com>
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.harlap.test.http;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MockHttpServerBehaviour {
    
    private static final int PORT = 51234;
    
    private static final String baseUrl = "http://localhost:" + MockHttpServerBehaviour.PORT;
    
    private MockHttpServer server;
    
    private HttpClient client;
    
    @Before
    public void setUp() throws Exception {
        this.server = new MockHttpServer(MockHttpServerBehaviour.PORT);
        this.server.start();
        this.client = new DefaultHttpClient();
    }
    
    @After
    public void tearDown() throws Exception {
        this.client.getConnectionManager().shutdown();
        this.server.stop();
    }
    
    @Test
    public void testShouldHandleGetRequests() throws ClientProtocolException, IOException {
        // Given a mock server configured to respond to a GET / with "OK"
        final String contentType = "text/plain";
        this.server.expect(MockHttpServer.Method.GET, "/").respondWith(200, contentType, "OK");
        
        // When a request for GET / arrives
        final HttpGet req = new HttpGet(MockHttpServerBehaviour.baseUrl + "/");
        final HttpResponse response = this.client.execute(req);
        final String responseBody = IOUtils.toString(response.getEntity().getContent());
        final int statusCode = response.getStatusLine().getStatusCode();
        
        // Then the response is "OK"
        assertEquals("OK", responseBody);
        
        // And the status code is 200
        assertEquals(200, statusCode);
        
        // And the Content-Type header is there with the correct value
        assertEquals(contentType, response.getFirstHeader(MockHttpServer.CONTENT_TYPE).getValue());
    }
    
    @Test
    public void testShouldReplyWithCorrectHeaders() throws IllegalStateException, IOException {
        // Given a mock server configured to respond to a GET / with "OK" with specific headers
        final Map<String, String> httpHeaders = new HashMap<String, String>();
        httpHeaders.put(MockHttpServer.CONTENT_TYPE, "application/xml");
        httpHeaders.put("fakeHttpHeader", "fakeHttpValue");
        this.server.expect(MockHttpServer.Method.GET, "/").respondWith(200, httpHeaders, "OK");
        
        // When a request for GET / arrives
        final HttpGet req = new HttpGet(MockHttpServerBehaviour.baseUrl + "/");
        final HttpResponse response = this.client.execute(req);
        final String responseBody = IOUtils.toString(response.getEntity().getContent());
        final int statusCode = response.getStatusLine().getStatusCode();
        
        // Then the response is "OK"
        assertEquals("OK", responseBody);
        
        // And the status code is 200
        assertEquals(200, statusCode);
        
        // And our http headers are there
        assertEquals("fakeHttpValue", response.getFirstHeader("fakeHttpHeader").getValue());
        assertEquals("application/xml", response.getFirstHeader(MockHttpServer.CONTENT_TYPE).getValue());
    }
    
    @Test
    public void testShouldHandlePostRequests() throws ClientProtocolException, IOException {
        // Given a mock server configured to respond to a POST / with data
        // "Hello World" with an ID
        this.server.expect(MockHttpServer.Method.POST, "/", "Hello World").respondWith(200, "text/plain", "ABCD1234");
        
        // When a request for POST / arrives
        final HttpPost req = new HttpPost(MockHttpServerBehaviour.baseUrl + "/");
        req.setEntity(new StringEntity("Hello World", HTTP.UTF_8));
        final ResponseHandler<String> handler = new BasicResponseHandler();
        final String responseBody = this.client.execute(req, handler);
        
        // Then the response is "ABCD1234"
        assertEquals("ABCD1234", responseBody);
    }
    
    @Test
    public void testShouldHandleDeleteRequests() throws ClientProtocolException, IOException {
        // Given a mock server configured to respond to a DELETE /test
        this.server.expect(MockHttpServer.Method.DELETE, "/test").respondWith(204, "text/plain", "");
        
        // When a request for DELETE /test arrives
        final HttpDelete req = new HttpDelete(MockHttpServerBehaviour.baseUrl + "/test");
        final HttpResponse response = this.client.execute(req);
        
        // Then the response status is 204
        assertEquals(204, response.getStatusLine().getStatusCode());
    }
    
    @Test
    public void testShouldNotMatchDataWhenExceptedDataIsNull() throws ClientProtocolException, IOException {
        // Given a mock server configured to respond to a POST /test with no data
        this.server.expect(MockHttpServer.Method.POST, "/test").respondWith(204, "text/plain", "");
        
        // When a request for POST /test arrives with parameters
        final HttpPost req = new HttpPost(MockHttpServerBehaviour.baseUrl + "/test");
        req.setEntity(new StringEntity("Hello World", HTTP.UTF_8));
        
        final HttpResponse response = this.client.execute(req);
        
        // Then the response status is 204
        assertEquals(204, response.getStatusLine().getStatusCode());
    }
    
    @Test
    public void testShouldHandleMultipleRequests() throws ClientProtocolException, IOException {
        // Given a mock server configured to respond to a POST / with data
        // "Hello World" with an ID
        // And configured to respond to a GET /test with "Yes sir!"
        this.server.expect(MockHttpServer.Method.POST, "/", "Hello World").respondWith(200, "text/plain", "ABCD1234");
        this.server.expect(MockHttpServer.Method.GET, "/test").respondWith(200, "text/plain", "Yes sir!");
        
        // When a request for POST / arrives
        final HttpPost req = new HttpPost(MockHttpServerBehaviour.baseUrl + "/");
        req.setEntity(new StringEntity("Hello World", HTTP.UTF_8));
        ResponseHandler<String> handler = new BasicResponseHandler();
        String responseBody = this.client.execute(req, handler);
        
        // Then the response is "ABCD1234"
        assertEquals("ABCD1234", responseBody);
        
        // When a request for GET /test arrives
        final HttpGet get = new HttpGet(MockHttpServerBehaviour.baseUrl + "/test");
        handler = new BasicResponseHandler();
        responseBody = this.client.execute(get, handler);
        
        // Then the response is "Yes sir!"
        assertEquals("Yes sir!", responseBody);
    }
    
    @Test(expected = UnsatisfiedExpectationException.class)
    public void testShouldFailWhenGetExpectationNotInvoqued() throws ClientProtocolException, IOException {
        // Given a mock server configured to respond to a GET / with "OK"
        this.server.expect(MockHttpServer.Method.GET, "/").respondWith(200, "text/plain", "OK");
        
        this.server.verify();
    }
    
    @Test
    public void testShouldNotFailWhenGetExpectationIsInvoqued() throws ClientProtocolException, IOException {
        // Given a mock server configured to respond to a GET / with "OK"
        this.server.expect(MockHttpServer.Method.GET, "/").respondWith(200, "text/plain", "OK");
        
        final HttpGet req = new HttpGet(MockHttpServerBehaviour.baseUrl + "/");
        this.client.execute(req);
        
        this.server.verify();
    }
    
    @Test(expected = UnsatisfiedExpectationException.class)
    public void testShouldFailWhenPostExpectationNotInvoqued() throws ClientProtocolException, IOException {
        // Given a mock server configured to respond to a GET / with "OK"
        this.server.expect(MockHttpServer.Method.POST, "/").respondWith(200, "text/plain", "OK");
        
        this.server.verify();
    }
    
    @Test
    public void testShouldNotFailWhenPostExpectationIsInvoqued() throws ClientProtocolException, IOException {
        // Given a mock server configured to respond to a GET / with "OK"
        this.server.expect(MockHttpServer.Method.POST, "/").respondWith(200, "text/plain", "OK");
        
        final HttpPost req = new HttpPost(MockHttpServerBehaviour.baseUrl + "/");
        this.client.execute(req);
        
        this.server.verify();
    }
    
    @Test(expected = UnsatisfiedExpectationException.class)
    public void testShouldFailWhenOneOfSeveralGetExpectationsIsNotInvoqued() throws ClientProtocolException, IOException {
        // Given a mock server configured to respond to a GET / with "OK"
        this.server.expect(MockHttpServer.Method.GET, "/").respondWith(200, "text/plain", "OK");
        this.server.expect(MockHttpServer.Method.GET, "/other").respondWith(200, "text/plain", "OK");
        
        final HttpGet req = new HttpGet(MockHttpServerBehaviour.baseUrl + "/");
        this.client.execute(req);
        
        this.server.verify();
    }
    
    @Test
    public void testShouldRespondWith500OWhenNotMatchingAnyRequestExpectation() throws ClientProtocolException, IOException {
        this.server.expect(MockHttpServer.Method.GET, "/foo").respondWith(200, "text/plain", "OK");
        
        final HttpGet req = new HttpGet(MockHttpServerBehaviour.baseUrl + "/bar");
        final HttpResponse response = this.client.execute(req);
        
        assertEquals(500, response.getStatusLine().getStatusCode());
    }
    
    @Test
    public void testVerifyDoNothingWhenNoExceptations() {
        this.server.verify();
    }
    
}
