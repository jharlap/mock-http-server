package com.github.kristofa.test.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.github.kristofa.test.http.HttpMessageHeaderField;
import com.github.kristofa.test.http.HttpRequestImpl;
import com.github.kristofa.test.http.MediaType;
import com.github.kristofa.test.http.Method;
import com.github.kristofa.test.http.QueryParameter;

public class HttpRequestImplTest {

    private final static byte[] CONTENT = "content".getBytes();
    private final static String PATH = "/test/a";
    private static final String QUERY_PARAM_KEY = "key1";
    private static final String QUERY_PARAM_VALUE = "value1";
    private static final String QUERY_PARAM_KEY2 = "key2";
    private static final String QUERY_PARAM_VALUE2 = "value2";

    private HttpRequestImpl httpRequest;

    @Before
    public void setup() {
        httpRequest = new HttpRequestImpl();
    }

    @Test
    public void testMethod() {
        assertNull(httpRequest.getMethod());
        assertSame(httpRequest, httpRequest.method(Method.DELETE));
        assertEquals(Method.DELETE, httpRequest.getMethod());

    }

    @Test
    public void testContent() {
        assertNull(httpRequest.getContent());
        assertSame(httpRequest, httpRequest.content(CONTENT));
        assertTrue(Arrays.equals(CONTENT, httpRequest.getContent()));
    }

    @Test
    public void testPath() {
        assertNull(httpRequest.getPath());
        assertSame(httpRequest, httpRequest.path(PATH));
        assertEquals(PATH, httpRequest.getPath());
    }

    @Test
    public void testQueryParameter() {
        assertTrue(httpRequest.getQueryParameters().isEmpty());
        assertSame(
            httpRequest,
            httpRequest.queryParameter(QUERY_PARAM_KEY, QUERY_PARAM_VALUE).queryParameter(QUERY_PARAM_KEY2,
                QUERY_PARAM_VALUE2));

        final Set<QueryParameter> expectedParameters = new HashSet<QueryParameter>();
        expectedParameters.add(new QueryParameter(QUERY_PARAM_KEY, QUERY_PARAM_VALUE));
        expectedParameters.add(new QueryParameter(QUERY_PARAM_KEY2, QUERY_PARAM_VALUE2));
        assertEquals(expectedParameters, httpRequest.getQueryParameters());
    }

    @Test
    public void testToString() {
        assertEquals("Method: null\nMessage Header: []\nPath: null\nQuery Parameters: []\nContent:\nnull",
            httpRequest.toString());
        httpRequest.content(CONTENT)
            .httpMessageHeader(HttpMessageHeaderField.CONTENTTYPE.getValue(), MediaType.APPLICATION_JSON_UTF8.getValue())
            .method(Method.GET).path(PATH).queryParameter(QUERY_PARAM_KEY, QUERY_PARAM_VALUE)
            .queryParameter(QUERY_PARAM_KEY2, QUERY_PARAM_VALUE2);
        assertEquals(
            "Method: GET\nMessage Header: [Content-Type: application/json; charset=UTF-8]\nPath: /test/a\nQuery Parameters: [key1=value1, key2=value2]\nContent:\ncontent",
            httpRequest.toString());
    }

    @Test
    public void testCopyConstructor() throws CloneNotSupportedException {

        final HttpRequestImpl copyNonInitializedRequest = new HttpRequestImpl(httpRequest);

        assertNotSame(httpRequest, copyNonInitializedRequest);
        assertEquals(copyNonInitializedRequest, httpRequest);

        httpRequest.content(CONTENT)
            .httpMessageHeader(HttpMessageHeaderField.CONTENTTYPE.getValue(), MediaType.APPLICATION_JSON_UTF8.getValue())
            .method(Method.GET).path(PATH).queryParameter(QUERY_PARAM_KEY, QUERY_PARAM_VALUE)
            .queryParameter(QUERY_PARAM_KEY2, QUERY_PARAM_VALUE2);

        assertFalse(httpRequest.equals(copyNonInitializedRequest));

        final HttpRequestImpl copyInitializedRequest = new HttpRequestImpl(httpRequest);
        assertNotSame(httpRequest, copyInitializedRequest);
        assertEquals(copyInitializedRequest, httpRequest);

    }
}
