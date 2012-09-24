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

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

public class MockHttpServer {
    /**
     * specific header type for content-type
     */
    public static final String CONTENT_TYPE = "Content-Type";
    
    public enum Method {
        GET, POST, PUT, DELETE;
    }
    
    private class ExpectedRequest {
        private Method method = null;
        
        private String path = null;
        
        private String data = null;
        
        private boolean satisfied = false;
        
        public ExpectedRequest(final Method method, final String path) {
            this.method = method;
            this.path = path;
        }
        
        public ExpectedRequest(final Method method, final String path, final String data) {
            this.method = method;
            this.path = path;
            this.data = data;
        }
        
        public Method getMethod() {
            return this.method;
        }
        
        public String getPath() {
            return this.path;
        }
        
        public String getData() {
            return this.data;
        }
        
        public boolean isSatisfied() {
            return this.satisfied;
        }
        
        public void passed() {
            this.satisfied = true;
        }
        
        @Override
        public String toString() {
            return (this.method + " " + this.path + " " + this.data);
        }
        
        @Override
        public boolean equals(final Object obj) {
            final ExpectedRequest req = (ExpectedRequest) obj;
            return req.getMethod().equals(this.method) && req.getPath().equals(this.path)
                    && ((req.getData() == null) || (this.data == null) || req.getData().equals(this.data));
        }
        
        @Override
        public int hashCode() {
            return (this.method + " " + this.path).hashCode();
        }
    }
    
    private class ExpectedResponse {
        
        private final int statusCode;
        
        private final String body;
        
        private final Map<String, String> headers = new HashMap<String, String>();
        
        public ExpectedResponse(final int statusCode, final String contentType, final String body) {
            this.statusCode = statusCode;
            this.headers.put(MockHttpServer.CONTENT_TYPE, contentType);
            this.body = body;
        }
        
        public ExpectedResponse(final int statusCode, final Map<String, String> headers, final String body) {
            this.statusCode = statusCode;
            this.headers.putAll(headers);
            this.body = body;
        }
        
        public int getStatusCode() {
            return this.statusCode;
        }
        
        public String getBody() {
            return this.body;
        }
        
        public Map<String, String> getHeaders() {
            return Collections.unmodifiableMap(this.headers);
        }
    }
    
    public class ExpectationHandler implements Container {
        
        private final Map<ExpectedRequest, ExpectedRequest> expectedRequests;
        
        private final Map<ExpectedRequest, ExpectedResponse> responsesForRequests;
        
        private ExpectedRequest lastAddedExpectation = null;
        
        public ExpectationHandler() {
            this.responsesForRequests = new HashMap<MockHttpServer.ExpectedRequest, MockHttpServer.ExpectedResponse>();
            this.expectedRequests = new HashMap<MockHttpServer.ExpectedRequest, MockHttpServer.ExpectedRequest>();
        }
        
        public void handle(final Request req, final Response response) {
            String data = null;
            try {
                if (req.getContentLength() > 0) {
                    data = req.getContent();
                }
            } catch (final IOException e) {
            }
            
            final ExpectedRequest expectedRequest = this.expectedRequests.get(new ExpectedRequest(
                    Method.valueOf(req.getMethod()), req.getTarget(), data));
            if (this.responsesForRequests.containsKey(expectedRequest)) {
                final ExpectedResponse expectedResponse = this.responsesForRequests.get(expectedRequest);
                response.setCode(expectedResponse.getStatusCode());
                setHttpHeadersToResponse(response, expectedResponse);
                PrintStream body = null;
                try {
                    body = response.getPrintStream();
                } catch (final IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                body.print(expectedResponse.getBody());
                expectedRequest.passed();
                body.close();
            } else {
                response.setCode(500);
                response.set(MockHttpServer.CONTENT_TYPE, "text/plain;charset=utf-8");
                PrintStream body;
                try {
                    body = response.getPrintStream();
                    body.print("Received unexpected request " + req.getMethod() + ":" + req.getTarget() + " with data: " + data);
                    body.close();
                } catch (final IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
        /**
         * @param response
         * @param expectedResponse
         */
        private void setHttpHeadersToResponse(final Response response, final ExpectedResponse expectedResponse) {
            for (final Entry<String, String> headerEntry : expectedResponse.getHeaders().entrySet()) {
                response.set(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        
        public void addExpectedRequest(final ExpectedRequest request) {
            this.lastAddedExpectation = request;
        }
        
        public void addExpectedResponse(final ExpectedResponse response) {
            this.responsesForRequests.put(this.lastAddedExpectation, response);
            this.expectedRequests.put(this.lastAddedExpectation, this.lastAddedExpectation);
            this.lastAddedExpectation = null;
        }
        
        public void verify() {
            for (final ExpectedRequest expectedRequest : this.responsesForRequests.keySet()) {
                if (!expectedRequest.isSatisfied()) {
                    throw new UnsatisfiedExpectationException("Unsatisfied expectation: " + expectedRequest);
                }
            }
            
        }
    }
    
    private ExpectationHandler handler;
    
    private final int port;
    
    public static final String GET = "GET";
    
    public static final String POST = "POST";
    
    public static final String PUT = "PUT";
    
    public static final String DELETE = "DELETE";
    
    private Connection connection;
    
    public MockHttpServer(final int port) {
        this.port = port;
    }
    
    public void start() throws Exception {
        this.handler = new ExpectationHandler();
        this.connection = new SocketConnection(this.handler);
        final SocketAddress address = new InetSocketAddress(this.port);
        this.connection.connect(address);
    }
    
    public void stop() throws Exception {
        this.connection.close();
    }
    
    public MockHttpServer expect(final Method method, final String path) {
        this.handler.addExpectedRequest(new ExpectedRequest(method, path));
        return this;
    }
    
    public MockHttpServer respondWith(final int statusCode, final String contentType, final String body) {
        this.handler.addExpectedResponse(new ExpectedResponse(statusCode, contentType, body));
        return this;
    }
    
    public MockHttpServer respondWith(final int statusCode, final Map<String, String> headers, final String body) {
        this.handler.addExpectedResponse(new ExpectedResponse(statusCode, headers, body));
        return this;
    }
    
    public MockHttpServer expect(final Method method, final String path, final String data) {
        this.handler.addExpectedRequest(new ExpectedRequest(method, path, data));
        return this;
    }
    
    public void verify() {
        this.handler.verify();
    }
    
}
