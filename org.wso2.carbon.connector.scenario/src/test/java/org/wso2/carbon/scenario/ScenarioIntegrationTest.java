/*
*  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.scenario;

import org.testng.Assert;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;
import org.wso2.connector.integration.test.base.RestResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Scenario integration test
 */
public class ScenarioIntegrationTest extends ScenarioIntegrationTestBase {

    private Map<String, String> esbRequestHeadersMap = new HashMap<String, String>();
    private Map<String, String> apiRequestHeadersMap = new HashMap<String, String>();
    private LogViewerClient logViewer;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        init();
        esbRequestHeadersMap.put("Accept-Charset", "UTF-8");
        esbRequestHeadersMap.put("Content-Type", "application/json");
    }

    @Test(enabled = true, groups = {"wso2.esb"}, description = "Add invoice test case")
    public void testSample() throws Exception {
        RestResponse<JSONObject> esbRestResponse =
                sendJsonRestRequest(proxyUrl, "GET", esbRequestHeadersMap);
        Thread.sleep(10000);
        logViewer = new LogViewerClient(esbServer.getBackEndUrl(), esbServer.getSessionCookie());
        LogEvent[] logs = logViewer.getAllRemoteSystemLogs();
        boolean success = false;
        for (LogEvent element : logs) {
            if (element.getMessage().contains("Success: Successfully added the row")) {
                success = true;
                break;
            }
        }
        Assert.assertEquals(esbRestResponse.getHttpStatusCode(), 200);
        Assert.assertEquals(success, true, "The invoice is stored in the spreadsheet");
    }
}