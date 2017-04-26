package org.sharedhealth.migrationservice.client;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.sharedhealth.migrationservice.exception.ConnectionException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

public class WebClient {
    public static final String ZERO_WIDTH_NO_BREAK_SPACE = "\uFEFF";
    public static final String BLANK_CHARACTER = "";

    public String get(URI url, Map<String, String> headers) throws IOException {
        HttpGet request = new HttpGet(url);
        addHeaders(request, headers);
        return execute(request);
    }

    public String post(String url, Map<String, String> data, Map<String, String> headers) {
        try {
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(getNameValuePairs(data));
            HttpPost request = new HttpPost(URI.create(url));
            addHeaders(request, headers);
            request.setEntity(entity);
            return execute(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String execute(final HttpRequestBase request) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                public String handleResponse(final HttpResponse response) throws IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? parseContentInputAsString(entity) : null;
                    } else if (status == 404) {
                        return null;
                    } else if (status == 401) {
                        throw new ConnectionException("Unauthorized request", 401, null);
                    } else {
                        throw new ConnectionException("Unexpected response " + response, status, null);
                    }
                }
            };
            return httpClient.execute(request, responseHandler);
        } finally {
            httpClient.close();
        }
    }

    private void addHeaders(HttpRequestBase request, Map<String, String> headers) {
        for (String key : headers.keySet()) {
            request.addHeader(key, headers.get(key));
        }
    }

    private String parseContentInputAsString(HttpEntity entity) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
        String inputLine;
        StringBuilder responseString = new StringBuilder();
        while ((inputLine = reader.readLine()) != null) {
            responseString.append(inputLine);
        }
        reader.close();
        return responseString.toString().replace(ZERO_WIDTH_NO_BREAK_SPACE, BLANK_CHARACTER);
    }

    private ArrayList<BasicNameValuePair> getNameValuePairs(Map<String, String> data) {
        ArrayList<BasicNameValuePair> valuePairs = new ArrayList<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            valuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        return valuePairs;
    }
}
