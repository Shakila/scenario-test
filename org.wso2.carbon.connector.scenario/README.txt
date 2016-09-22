Product: salesforce + googlespreadsheet Integration Test

 Pre-requisites:

    - Maven 3.x
    - Java 1.6 or above
    - The org.wso2.esb.integration.integration-base project is required. The test suite has been configured to download this project automatically. If the automatic download fails, download the following project and compile it using the mvn clean install command to update your local repository:
      https://github.com/wso2-extensions/esb-integration-base

    Tested Platforms:

    - Mac OS X 10.11.6
    - WSO2 ESB 4.9.0
    - java 1.7

Steps to follow in setting integration test.

 1. Download ESB 4.9.0 from our official website

 2. ESB should be configured as below.
    Please make sure that the below mentioned Axis configurations are enabled (<ESB_HOME>/repository/conf/axis2/axis2.xml)

        Message Formatters :-
        <messageFormatter contentType="application/atom+xml"
                                  class="org.apache.axis2.transport.http.ApplicationXMLFormatter"/>
        <messageformatter contenttype="text/csv"
                                   class="org.apache.axis2.format.PlainTextFormatter"/>

        Message Builders :-
        <messageBuilder contentType="application/atom+xml"
                                class="org.apache.axis2.builder.ApplicationXMLBuilder"/>
        <messagebuilder contenttype="text/csv"
                                 class="org.apache.axis2.format.PlainTextBuilder"/>
        <messageBuilder contentType="application/binary"
                                class="org.wso2.carbon.relay.BinaryRelayBuilder"/>

 3. Download the inbound org.apache.synapse.salesforce.poll.class-1.0.0.jar from https://storepreview.wso2.com/store/ and copy the jar to the <ESB_HOME>/repository/components/dropins directory.

 4. Compress modified ESB as wso2esb-4.9.0.zip and copy that zip file in to location "<SCENARIO_HOME>/repository/".


 5. Get a access token from OAuth 2.0 Playground.
       i)  Using the URL "https://developers.google.com/oauthplayground/" create a access token and refresh token.

      Note: Application needs access to user data, it asks Google for a particular scope of access.
            Here's the OAuth 2.0 scope information for the Google Sheets API:"https://spreadsheets.google.com/feeds"

 6. Do the following steps to setup google spreadsheet environment.
       i)     Create a spreadsheet for the google user and use the name in step 7 vii.
       ii)    Create a worksheet in that spreadsheet and use the name in step 7 x.
       iii)   Add a row in the worksheet with these column names (Name, Id, Description, Status, CreatedDate).

 7. Replace valid values for the following properties at <SCENARIO_HOME>/src/test/resources/artifacts/ESB/config/sequences/scenario/salesforceToGSheet.xml

        i)    refreshToken        -    Use the Refresh token got in step 5.
        ii)   clientId            -    Use the Client ID used to generate the access token.
        iii)  clientSecret        -    Use the Client Secret used to generate the access token.
        iv)   accessToken         -    Use the accessToken got in step 5.
        v)    apiVersion          -    Use appropriate API version.
        vi)   apiUrl              -    Use the API URL of the google spreadsheet.
        vii)  spreadsheetTitle    -    Use the title of the spreadsheet created in step 6 i.
        viii) rowCount            -    Use a required number of row to a new worksheet.
        ix)   colCount            -    use a required number of column to a new worksheet.
        x)    worksheetTitle      -    Use the title of worksheet created in step 6 ii.

 8. Do the following steps to setup salesforce environment.
        i)   Create a salesforce account.
        ii)  Using the URL "https://developer.salesforce.com/docs/atlas.en-us.api_streaming.meta/api_streaming/create_object.htm#create_object" create an object in salesforce.
        iii) Using the URL "https://developer.salesforce.com/docs/atlas.en-us.api_streaming.meta/api_streaming/create_a_pushtopic.htm" create a push topic.

 9. Update the googlespreadsheet properties file at location "<SCENARIO_HOME>/src/test/resources/artifacts/ESB/connector/config" as below.

        i)   sfusername           -    Use username of the salesforce account.
        ii)  sfpassword           -    Use password of the salesforce account.
        iii) loginUrl             -    Use login URL of the salesforce account.
        iv)  sfstatus             -    Use the status of the invoice e.g: Open, Closed, Negotiating & Pending .
        v)   sfdescription        -    Use description about that invoice entry.
        vi)  sObjectType          -    Use the type of the sObject created in step 8 ii.

 10. Navigate to "{SCENARIO_HOME}/" and run the following command.
       $ mvn clean install
