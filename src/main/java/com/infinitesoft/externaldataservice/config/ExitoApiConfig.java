package com.infinitesoft.externaldataservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "exito.api")
public class ExitoApiConfig {
    private String baseUrl = "https://www.exito.com";
    private String graphqlEndpoint = "/api/graphql";
    private String rtbhLid = "{\"eventType\":\"lid\",\"id\":\"f00LqlYcJ5kvmolbbvhL\",\"expiryDate\":\"2026-07-09T02:53:06.210Z\"}";
    private String gclLs = "{\"schema\":\"gcl\",\"version\":1,\"gcl_ctr\":{\"value\":{\"value\":0,\"creationTimeMs\":1751022790419},\"expires\":1758798790419}}";
    private String spid = "6559A001-FD1B-4EE6-A18E-B41410D9BD89";
    private String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getGraphqlEndpoint() {
        return graphqlEndpoint;
    }

    public void setGraphqlEndpoint(String graphqlEndpoint) {
        this.graphqlEndpoint = graphqlEndpoint;
    }

    public String getRtbhLid() {
        return rtbhLid;
    }

    public void setRtbhLid(String rtbhLid) {
        this.rtbhLid = rtbhLid;
    }

    public String getGclLs() {
        return gclLs;
    }

    public void setGclLs(String gclLs) {
        this.gclLs = gclLs;
    }

    public String getSpid() {
        return spid;
    }

    public void setSpid(String spid) {
        this.spid = spid;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getFullGraphqlUrl() {
        return baseUrl + graphqlEndpoint + "?operationName=QuerySearch";
    }

    public String getFormattedCookies() {
        StringBuilder cookies = new StringBuilder();
        cookies.append("__rtbh.lid=").append(rtbhLid).append("; ");
        cookies.append("_gcl_ls=").append(gclLs).append("; ");
        cookies.append("spid=").append(spid);
        return cookies.toString();
    }
}