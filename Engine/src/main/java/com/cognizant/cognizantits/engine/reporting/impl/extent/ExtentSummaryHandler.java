/*
* Copyright 2014 - 2021 Cognizant Technology Solutions
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
 */
package com.cognizant.cognizantits.engine.reporting.impl.extent;

import java.io.File;
import java.io.IOException;
import org.apache.http.client.ClientProtocolException;
import org.json.simple.parser.ParseException;
import com.cognizant.cognizantits.engine.constants.FilePath;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.cognizant.cognizantits.engine.constants.SystemDefaults;
import com.cognizant.cognizantits.engine.core.Control;
import com.cognizant.cognizantits.engine.core.RunContext;
import com.cognizant.cognizantits.engine.core.RunManager;
import com.cognizant.cognizantits.engine.reporting.SummaryReport;
import com.cognizant.cognizantits.engine.reporting.TestCaseReport;
import com.cognizant.cognizantits.engine.reporting.impl.handlers.PrimaryHandler;
import com.cognizant.cognizantits.engine.reporting.impl.handlers.SummaryHandler;
import com.cognizant.cognizantits.engine.reporting.extentreport.ExtentReport;
import com.cognizant.cognizantits.engine.support.DesktopApi;
import com.cognizant.cognizantits.engine.support.Status;
import java.util.logging.Level;
import java.util.logging.Logger;
import tech.grasshopper.reporter.ExtentPDFReporter;

public class ExtentSummaryHandler extends SummaryHandler implements PrimaryHandler {

    private static final Logger LOGGER = Logger.getLogger(ExtentSummaryHandler.class.getName());

    int FailedTestCases = 0;
    int PassedTestCases = 0;

    String testcasename = "";
    public ExtentSparkReporter sparkReporter;
    public ExtentReports extentReports;
    public ExtentTest test;
    ExtentReport extentReport = new ExtentReport();

    public ExtentSummaryHandler(SummaryReport report) {
        super(report);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void createReport(String runTime, int size) {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            try {
                startLaunch(RunManager.getGlobalSettings().getTestSet());
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    private String getExtentSetting(String property) {
        return Control.getCurrentProject().getProjectSettings().getExtentSettings().getProperty(property);
    }

    public boolean isExtentEnabled() {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            return Control.getCurrentProject().getProjectSettings()
                    .getExecSettings(RunManager.getGlobalSettings().getRelease(), RunManager.getGlobalSettings().getTestSet()).getRunSettings().isExtentReport();
        }

        return false;
    }

    private void startLaunch(String testset) {
        if (isExtentEnabled()) {
            try {
                String reportName = getExtentSetting("ReportName");
                if (reportName.equalsIgnoreCase("default")) {
                    reportName = testset + " : Execution Report";
                }
                String documentTitle = getExtentSetting("DocumentTitle");
                if (documentTitle.equalsIgnoreCase("default")) {
                    documentTitle = testset + " : Execution Report";
                }
                initiateExtentReport(getExtentSetting("HTML-Theme"), documentTitle, reportName);
            } catch (IOException | ParseException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    @Override
    public Object getData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public File getFile() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status getCurrentStatus() {
        if (FailedTestCases > 0 || PassedTestCases == 0) {
            return Status.FAIL;
        } else {
            return Status.PASS;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void updateTestCaseResults(RunContext runContext, TestCaseReport report, Status state,
            String executionTime) {
        System.out.println(report.getData());
        String status;
        if (state.equals(Status.PASS)) {
            status = "Passed";
            PassedTestCases++;
        } else {
            FailedTestCases++;
            status = "Failed";
        }
    }

    @Override
    public synchronized void finalizeReport() {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            if (isExtentEnabled()) {
                try {
                    flushExtentReport();
                    launchExtentReport();
                } catch (IOException | ParseException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }

    }

    public void flushExtentReport()
            throws ClientProtocolException, IOException, ParseException {
        extentReport.extentReports.flush();

    }

    /**
     * open the summary report when execution is finished
     */
    public synchronized void launchExtentReport() {
        if (SystemDefaults.canLaunchSummary()) {
            DesktopApi.open(new File(FilePath.getCurrentExtentReportPath()));
        }
    }

    public void initiateExtentReport(String theme, String docTitle, String reportName)
            throws ClientProtocolException, IOException, ParseException {

        extentReport.sparkReporter = new ExtentSparkReporter(FilePath.getCurrentExtentReportPath());
        if (theme.trim().equalsIgnoreCase("dark")) {
            extentReport.sparkReporter.config().setTheme(Theme.DARK);
        } else {
            extentReport.sparkReporter.config().setTheme(Theme.STANDARD);
        }
        extentReport.sparkReporter.config().setDocumentTitle(docTitle);
        extentReport.sparkReporter.config().setReportName(reportName);

        extentReport.extentReports = new ExtentReports();
        
        extentReport.extentReports.setSystemInfo("Execution Mode", Control.getCurrentProject().getProjectSettings().getExecSettings().getRunSettings().getExecutionMode());
        extentReport.extentReports.setSystemInfo("Thread count", String.valueOf(Control.exe.getExecSettings().getRunSettings().getThreadCount()));
        extentReport.extentReports.setSystemInfo("Execution Environment", Control.getCurrentProject().getProjectSettings().getExecSettings().getRunSettings().getTestEnv());
        

        
        extentReport.extentReports.attachReporter(extentReport.sparkReporter);
        if (getExtentSetting("Generate PDF").trim().equalsIgnoreCase("true")) {
            extentReport.pdfReporter = new ExtentPDFReporter(FilePath.getCurrentExtentPDFReportPath());
            extentReport.extentReports.attachReporter(extentReport.pdfReporter);
            extentReport.pdfReporter.config().setTitle(docTitle);
            extentReport.pdfReporter.config().setTitleColor("964B00");
            extentReport.pdfReporter.config().setStartTimesColor("33A532");

            extentReport.pdfReporter.config().setFinishTimesColor("6993ff");
            extentReport.pdfReporter.config().setDurationColor("9000ff");
            extentReport.pdfReporter.config().setPassCountColor("C0C0C0");
            extentReport.pdfReporter.config().setFailCountColor("A9A9A9");
            extentReport.pdfReporter.config().setPassColor("0b8c47");
            extentReport.pdfReporter.config().setFailColor("990000");
            extentReport.pdfReporter.config().setSkipColor("b17750");
            extentReport.pdfReporter.config().setWarnColor("ffc100");
            extentReport.pdfReporter.config().setInfoColor("6d9dc9");
            extentReport.pdfReporter.config().setCategoryAttributeColor("999100");
            extentReport.pdfReporter.config().setCategoryNameColor("bd8275");
            extentReport.pdfReporter.config().setAuthorAttributeColor("002288");
            extentReport.pdfReporter.config().setAuthorNameColor("3ac5b6");
            extentReport.pdfReporter.config().setDeviceAttributeColor("228800");
            extentReport.pdfReporter.config().setDeviceNameColor("cd17bf");
            extentReport.pdfReporter.config().setSystemAttributeColor("bb1144");
            extentReport.pdfReporter.config().setSystemNameColor("990000");
            extentReport.pdfReporter.config().setSystemValueColor("daa520");
            extentReport.pdfReporter.config().setExceptionAttributeColor("000000");
            extentReport.pdfReporter.config().setTestNameColor("d62d20");
            extentReport.pdfReporter.config().setTestTimesColor("ffa700");
            extentReport.pdfReporter.config().setTestTimeStampColor("08080");
            extentReport.pdfReporter.config().setTestExceptionColor("990000");

        }
    }

}
