package com.textile.erp.customer.gst;

public interface GstService {

    GstLookupResponse lookupGstin(String gstin);
}
