package com.textile.erp.customer.gst;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gst.rapidapi")
@Getter
@Setter
public class GstProperties {

    private String key = "0004fce178msh522802d606b6a19p1a0d89jsn6ea48e2820d2";
    private String host = "gst-return-status.p.rapidapi.com";
    private String url = "https://gst-return-status.p.rapidapi.com/free/gstin/";
    private int timeoutMs = 5000;
}
