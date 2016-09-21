/**
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Integration Base package for ESB Extension Scenario.
 * v1.0.1
 */

package org.wso2.carbon.scenario;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.io.FileUtils;
import org.wso2.carbon.automation.api.clients.proxy.admin.ProxyServiceAdminClient;
import org.wso2.carbon.automation.api.clients.sequences.SequenceAdminServiceClient;
import org.wso2.carbon.automation.api.clients.utils.AuthenticateStub;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.utils.axis2client.ConfigurationContextProvider;
import org.wso2.carbon.mediation.library.stub.MediationLibraryAdminServiceStub;
import org.wso2.carbon.mediation.library.stub.upload.MediationLibraryUploaderStub;
import org.wso2.carbon.mediation.library.stub.upload.types.carbon.LibraryFileItem;
import org.wso2.carbon.proxyadmin.stub.ProxyServiceAdminProxyAdminException;
import org.wso2.carbon.sequences.stub.types.SequenceEditorException;
import org.wso2.connector.integration.test.base.ConnectorIntegrationTestBase;
import javax.xml.stream.XMLStreamException;

import org.testng.annotations.AfterClass;
import javax.activation.DataHandler;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

/**
 * This class contains the all methods which are used by this scenario.
 */
public abstract class ScenarioIntegrationTestBase extends ConnectorIntegrationTestBase {

    List<String> proxies = new ArrayList<String>();

    List<String> sequences = new ArrayList<String>();

    private static final float SLEEP_TIMER_PROGRESSION_FACTOR = 0.5f;

    private MediationLibraryUploaderStub mediationLibUploadStub;

    private MediationLibraryAdminServiceStub adminServiceStub;

    private String repoLocation;

    private String ieLocation;

    private ProxyServiceAdminClient proxyAdmin;

    private SequenceAdminServiceClient sequenceAdmin;

    protected Properties scenarioProperties;

    private String pathToProxiesDirectory;

    private String pathToRequestsDirectory;

    private String pathToSequencesDirectory;

    private String pathToInboundEndpointDirectory;

    protected String proxyUrl;

    protected String pathToResourcesDirectory;

    protected static final int MULTIPART_TYPE_RELATED = 100001;

    /**
     * Set up the integration test environment.
     *
     * @throws Exception
     */

    @Override
    protected void init() throws Exception {

        super.init();

        ConfigurationContextProvider configurationContextProvider = ConfigurationContextProvider.getInstance();
        ConfigurationContext cc = configurationContextProvider.getConfigurationContext();

        mediationLibUploadStub =
                new MediationLibraryUploaderStub(cc, esbServer.getBackEndUrl() + "MediationLibraryUploader");
        AuthenticateStub.authenticateStub("admin", "admin", mediationLibUploadStub);

        adminServiceStub =
                new MediationLibraryAdminServiceStub(cc, esbServer.getBackEndUrl() + "MediationLibraryAdminService");

        AuthenticateStub.authenticateStub("admin", "admin", adminServiceStub);

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            repoLocation = System.getProperty("scenario_repo").replace("\\", "/");
        } else {
            repoLocation = System.getProperty("scenario_repo").replace("/", "/");
        }

        proxyAdmin = new ProxyServiceAdminClient(esbServer.getBackEndUrl(), esbServer.getSessionCookie());
        scenarioProperties = getScenarioConfigProperties("scenario");

        pathToProxiesDirectory = repoLocation + scenarioProperties.getProperty("proxyDirectoryRelativePath");
        pathToRequestsDirectory = repoLocation + scenarioProperties.getProperty("requestDirectoryRelativePath");

        pathToResourcesDirectory = repoLocation + scenarioProperties.getProperty("resourceDirectoryRelativePath");

        File zipFolder = new File(pathToResourcesDirectory);
        File[] listOfZIPFiles = zipFolder.listFiles();
        for (File element : listOfZIPFiles) {
            if (element.isFile()) {
                String connectorName = element.getName();
                if (connectorName.endsWith(".zip") || connectorName.endsWith(".ZIP")) {
                    String connectorFileName = connectorName;
                    uploadConnector(pathToResourcesDirectory, mediationLibUploadStub, connectorFileName);

                    // Connector file name comes with version,however mediation process only with name.
                    connectorName = connectorName.split("-")[0];

                    byte maxAttempts = 3;
                    int sleepTimer = 10000;
                    for (byte attemptCount = 0; attemptCount < maxAttempts; attemptCount++) {
                        log.info("Sleeping for " + sleepTimer / 1000 + " second(s) for connector to upload.");
                        Thread.sleep(sleepTimer);
                        String[] libraries = adminServiceStub.getAllLibraries();
                        if (Arrays.asList(libraries).contains("{org.wso2.carbon.connector}" + connectorName)) {
                            break;
                        } else {
                            log.info("Connector upload incomplete. Waiting...");
                            sleepTimer *= SLEEP_TIMER_PROGRESSION_FACTOR;
                        }
                    }

                    adminServiceStub.updateStatus("{org.wso2.carbon.connector}" + connectorName, connectorName,
                            "org.wso2.carbon.connector", "enabled");
                }
            }
        }
        uploadProxies();
        uploadSequences();
        uploadIEs();
    }

    /**
     * Method to upload sequences if required from a given path.
     *
     * @throws XMLStreamException
     * @throws IOException
     * @throws SequenceEditorException
     */
    public void uploadSequences() throws IOException, SequenceEditorException, XMLStreamException {
        String sequenceDirectoryRelativePath = scenarioProperties.getProperty("sequenceDirectoryRelativePath");
        // if sequence directory relative path is available in properties, add sequences to ESB
        if (sequenceDirectoryRelativePath != null && !sequenceDirectoryRelativePath.isEmpty()) {
            pathToSequencesDirectory = repoLocation + sequenceDirectoryRelativePath;
            sequenceAdmin =
                    new SequenceAdminServiceClient(esbServer.getBackEndUrl(), esbServer.getSessionCookie());
            File sequenceFolder = new File(pathToSequencesDirectory);
            File[] listOfSequenceFiles = sequenceFolder.listFiles();
            for (File element : listOfSequenceFiles) {
                if (element.isFile()) {
                    String fileName = element.getName();
                    if (fileName.endsWith(".xml") || fileName.endsWith(".XML")) {
                        sequences.add(fileName.replaceAll("[.][x|X][m|M][l|L]", ""));
                        sequenceAdmin.addSequence(new DataHandler(new URL("file:///" + pathToSequencesDirectory
                                + fileName)));
                    }
                }
            }
        }
    }

    /**
     * Method to upload proxies if required from a given path.
     *
     * @throws XMLStreamException
     * @throws IOException
     * @throws ProxyServiceAdminProxyAdminException
     */
    public void uploadProxies() throws IOException, XMLStreamException, ProxyServiceAdminProxyAdminException {
        File folder = new File(pathToProxiesDirectory);
        File[] listOfFiles = folder.listFiles();
        for (File element : listOfFiles) {
            if (element.isFile()) {
                String fileName = element.getName();
                if (fileName.endsWith(".xml") || fileName.endsWith(".XML")) {
                    proxies.add(fileName.replaceAll("[.][x|X][m|M][l|L]", ""));
                    proxyAdmin
                            .addProxyService(new DataHandler(new URL("file:///" + pathToProxiesDirectory + fileName)));
                    proxyUrl = getProxyServiceURL(fileName);
                    proxyUrl = proxyUrl.replaceAll("[.][x|X][m|M][l|L]", "");
                }
            }
        }
    }

    /**
     * Method to upload inbound-endpoints if required from a given path.
     *
     * @throws InterruptedException
     */
    public void uploadIEs() throws InterruptedException {
        String inboundEndpointDirectoryRelativePath =
                scenarioProperties.getProperty("inboundEndpointDirectoryRelativePath");
        if (inboundEndpointDirectoryRelativePath != null && !inboundEndpointDirectoryRelativePath.isEmpty()) {
            pathToInboundEndpointDirectory = repoLocation + inboundEndpointDirectoryRelativePath;
            File inboundEndpointFolder = new File(pathToInboundEndpointDirectory);
            File[] listOfIEFiles = inboundEndpointFolder.listFiles();
            ieLocation = System.getProperty("carbon.home")
                    + scenarioProperties.getProperty("inboundEndpointRelativePath");
            for (File element : listOfIEFiles) {
                if (element.isFile()) {
                    String fileName = element.getName();
                    if (fileName.endsWith(".xml") || fileName.endsWith(".XML")) {
                        File source = new File(pathToInboundEndpointDirectory + fileName);
                        File dest = new File(ieLocation + fileName);
                        try {
                            FileUtils.copyFile(source, dest);
                            Thread.sleep(30000);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * Clean up the ESB.
     *
     * @throws ProxyServiceAdminProxyAdminException
     * @throws SequenceEditorException
     * @throws RemoteException @
     */
    @Override
    @AfterClass(alwaysRun = true)
    public void cleanUpEsb() throws RemoteException, ProxyServiceAdminProxyAdminException {
        for (String proxyName : proxies) {
            proxyAdmin.deleteProxy(proxyName);
        }
        for (String sequenceName : sequences) {
            try {
                sequenceAdmin.deleteSequence(sequenceName);
            } catch (SequenceEditorException e) {
                throw new RemoteException("Unable to delete the sequence: " + e.getMessage(), e);
            }
        }
        try {
            FileUtils.cleanDirectory(new File(ieLocation));
        } catch (IOException e) {
            throw new RemoteException("Unable to delete the inbound-endpoint configuration file: " + e.getMessage(), e);
        }
    }

    /**
     * Upload the given connector.
     *
     * @param repoLocation
     * @param mediationLibUploadStub
     * @param strFileName
     * @throws MalformedURLException
     * @throws RemoteException
     */
    private void uploadConnector(String repoLocation, MediationLibraryUploaderStub mediationLibUploadStub,
                                 String strFileName) throws MalformedURLException, RemoteException {

        List<LibraryFileItem> uploadLibraryInfoList = new ArrayList<LibraryFileItem>();
        LibraryFileItem uploadedFileItem = new LibraryFileItem();
        uploadedFileItem.setDataHandler(new DataHandler(new URL("file:" + "///" + repoLocation + "/" + strFileName)));
        uploadedFileItem.setFileName(strFileName);
        uploadedFileItem.setFileType("zip");
        uploadLibraryInfoList.add(uploadedFileItem);
        LibraryFileItem[] uploadServiceTypes = new LibraryFileItem[uploadLibraryInfoList.size()];
        uploadServiceTypes = uploadLibraryInfoList.toArray(uploadServiceTypes);
        mediationLibUploadStub.uploadLibrary(uploadServiceTypes);

    }

    /**
     * Get connector configuration properties.
     *
     * @param fileName Name of the file to load properties.
     * @return {@link Properties} object.
     */
    private Properties getScenarioConfigProperties(String fileName) {

        String connectorConfigFile = null;
        ProductConstant.init();
        try {
            connectorConfigFile =
                    ProductConstant.SYSTEM_TEST_SETTINGS_LOCATION + File.separator + "artifacts" + File.separator
                            + "ESB" + File.separator + "scenario" + File.separator + "config" + File.separator
                            + fileName + ".properties";
            File connectorPropertyFile = new File(connectorConfigFile);
            InputStream inputStream = null;
            if (connectorPropertyFile.exists()) {
                inputStream = new FileInputStream(connectorPropertyFile);
            }

            if (inputStream != null) {
                Properties prop = new Properties();
                prop.load(inputStream);
                inputStream.close();
                return prop;
            }

        } catch (IOException ignored) {
            log.error("automation.properties file not found, please check your configuration");
        }

        return null;
    }
}
