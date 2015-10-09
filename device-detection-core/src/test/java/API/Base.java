package API;

import common.Utils;
import fiftyone.mobile.detection.Dataset;
import fiftyone.mobile.detection.Match;
import fiftyone.mobile.detection.Provider;
import fiftyone.mobile.detection.entities.Property;
import fiftyone.mobile.detection.factories.StreamFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/* *********************************************************************
 * This Source Code Form is copyright of 51Degrees Mobile Experts Limited. 
 * Copyright © 2014 51Degrees Mobile Experts Limited, 5 Charlotte Close,
 * Caversham, Reading, Berkshire, United Kingdom RG4 7BY
 * 
 * This Source Code Form is the subject of the following patent 
 * applications, owned by 51Degrees Mobile Experts Limited of 5 Charlotte
 * Close, Caversham, Reading, Berkshire, United Kingdom RG4 7BY: 
 * European Patent Application No. 13192291.6; and
 * United States Patent Application Nos. 14/085,223 and 14/085,301.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0.
 * 
 * If a copy of the MPL was not distributed with this file, You can obtain
 * one at http://mozilla.org/MPL/2.0/.
 * 
 * This Source Code Form is "Incompatible With Secondary Licenses", as
 * defined by the Mozilla Public License, v. 2.0.
 * ********************************************************************* */

public abstract class Base {
    
    private Dataset dataset;
    private Provider provider;
    private final String dataFile;
    
    public Base(String dataFile) {
        this.dataFile = dataFile;
        Utils.checkFileExists(dataFile);

    }

    @Before
    public void createDataset() {
        try {
            this.dataset = StreamFactory.create(this.dataFile, false);
            this.provider = new Provider(dataset);
        } catch (IOException ex) {
            Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @After
    public void dispose() {
        dispose(true);
        System.gc();
    }

    private void dispose(boolean disposing) {
        if (this.dataset != null) {
            dataset.dispose();
        }
    }

    @Test
    public void API_AllHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        for (String header : dataset.getHttpHeaders()) {
            headers.put(header, common.UserAgentGenerator.getRandomUserAgent(0));
        }
        try {
            fetchAllProperties(provider.match(headers));
        } catch (IOException ex) {
            Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Test
    public void API_AllHeadersNull() {
        Map<String, String> headers = new HashMap<String, String>();
        for (String header : dataset.getHttpHeaders()) {
            headers.put(header, null);
        }
        try {
            fetchAllProperties(provider.match(headers));
        } catch (IOException ex) {
            Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Test
    public void API_DuplicateHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        for (int i = 0; i < 5; i++) {
            for (String header : dataset.getHttpHeaders()) {
                headers.put(header, common.UserAgentGenerator.getRandomUserAgent(0));
            }
        }
        try {
            fetchAllProperties(provider.match(headers));
        } catch (IOException ex) {
            Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Test
    public void API_DuplicateHeadersNull() {
        Map<String, String> headers = new HashMap<String, String>();
        for (int i = 0; i < 5; i++) {
            for (String header : dataset.getHttpHeaders()) {
                headers.put(header, null);
            }
        }
        try {
            fetchAllProperties(provider.match(headers));
        } catch (IOException ex) {
            Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Test
    public void API_EmptyHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        try {
            fetchAllProperties(provider.match(headers));
        } catch (IOException ex) {
            Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Test
    public void API_EmptyUserAgent() {
        String empty = "";
        try {
            fetchAllProperties(provider.match(empty));
        } catch (IOException ex) {
            Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Test
    public void API_LongUserAgent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(common.UserAgentGenerator.getRandomUserAgent(10));
        }
        String userAgent = sb.toString();
        try {
            fetchAllProperties(provider.match(userAgent));
        } catch (IOException ex) {
            Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Test
    public void API_NullHeaders() {
        Map<String, String> headers = null;
        try {
            fetchAllProperties(provider.match(headers));
        } catch (IOException ex) {
            Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Test
    public void API_NullUserAgent() {
        String userAgent = null;
        try {
            fetchAllProperties(provider.match(userAgent));
        } catch (IOException ex) {
            Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void fetchAllProperties(Match match) throws IOException {
        long checkSum = 0;
        for (Property property : match.dataSet.getProperties()) {
            String propName = property.getName();
            System.out.println("Property: "+propName);
            if (match.getValues(property) != null) {
                System.out.print(match.getValues(property)+" ");
                checkSum += match.getValues(propName).hashCode();
            }
            System.out.println("");
        }
        System.out.println("Check sum: "+checkSum);
    }
}
