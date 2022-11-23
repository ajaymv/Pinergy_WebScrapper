import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class WebScrapper {

    public static final String STR_SPACE = " ";
    private static final String STR_Y = "Y";
    private static final String STR_N = "N";
    public static final String XPATH_EMAIL = "//a[contains(@href,'mailto:')]";
    public static final String XPATH_COMPANY_NAME = "//a[contains(@href,'office.asp?')]";
    public static final String XPATH_OFFICE_ID = "//strong[contains(text(),'Office Id:')]";
    public static final String STR_SEMI_COLON = ":";
    public static final String XPATH_SUBS = "//table[@id='agentTable']/tbody/tr[@class='agentRow']/td[2]";
    public static final String XPATH_LOGOUT = "//a[contains(.,'Sign Out')]";
    public static final String XPATH_COOKIE_CONSENT_OK = "//button[@class='btn btn-sm btn-primary mls-js-cookie-consent-action']";
    public static final String XPATH_SUBMIT = "//button[@class='btn btn-sm btn-primary mls-js-submit-btn']";
    public static final String USERNAME = "CN205418";
    public static final String PASSWORD = "OffshoreOnsite#2";
    public static final String PINERGY_SIGN_IN_URL = "https://h3c.mlspin.com/tools/roster/agent.asp?aid=CN250694&nomenu=";
    public static final String STR_COMMA = ",";
    public static final String CHROME_WEB_DRIVER_PATH = "C:\\Users\\ajay.vishweshwara\\Downloads\\chromedriver_win32\\chromedriver.exe";

    //Output file of known agents
    public static final String UPDATED_AGENT_FILE_PATH = "C:\\Users\\ajay.vishweshwara\\Downloads\\AgentInfo_new.csv";

    //Output file of unknown agents
    public static final String UNKNOWN_AGENT_FILE_PATH = "C:\\Users\\ajay.vishweshwara\\Downloads\\AgentInfo_unknown.csv";

    public static void main(String args[]) throws IOException {

        AtomicInteger recordCounter= new AtomicInteger(0);
        WebDriver driver = getWebDriver();

        try {
            List<String> updatedAgentInfo = new ArrayList<>();
            List<String> unknownAgents = new ArrayList<>();

            AtomicInteger counter = new AtomicInteger(0);

            //Input file name
            String fileName = "C:\\Users\\ajay.vishweshwara\\Downloads\\AgentInfo.csv";
            Stream<String> stream = Files.lines(Paths.get(fileName));

            //Login to Pinergy
            signInToPinergy(driver);

            stream.forEach(e -> {
                recordCounter.incrementAndGet();

                //Write every 100 records processed to file, don't want to write large chunk to file.
                if (counter.getAndIncrement() == 100) {
                    System.out.println("Writing 100 records to file");
                    //Write to file
                    writeToCSVFile(updatedAgentInfo, false);
                    writeToCSVFile(unknownAgents, true);
                    clearLists(updatedAgentInfo, unknownAgents);
                    counter.set(0);
                }

                //Uncomment to gracefully logout of Pinergy
                /*if(recordCounter.get()>10000){
                    logOut(driver);
                }*/

                AgentInfo agent = getAgentInfo(e);
                System.out.println(String.format("INFO: Extracting Info for agent %s", agent.getAgentName()));
                driver.get(agent.getAllLinksToQuery());
                if (scrapeAgentInfo(driver, agent)) {
                    updatedAgentInfo.add(agent.toString());
                } else {
                    unknownAgents.add(e);
                }
            });

            //Write remaining records to file
            writeToCSVFile(updatedAgentInfo, false);
            writeToCSVFile(unknownAgents, true);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //Logout from Pinergy and close the driver
            logOut(driver);
        }

    }

    private static void clearLists(List<String> updatedAgentInfo, List<String> unknownAgents) {
        updatedAgentInfo.clear();
        unknownAgents.clear();
    }

    private static WebDriver getWebDriver() {
        System.setProperty("webdriver.chrome.driver", CHROME_WEB_DRIVER_PATH);
        ChromeOptions options = new ChromeOptions();
        //Comment below line to see actions on browser
        options.addArguments("--headless");
        WebDriver driver = new ChromeDriver(options);
        return driver;
    }
    private static void writeToCSVFile(List<String> agentInfos, boolean unknown) {
        try (FileOutputStream csvUpdatedAgentInfoFile = new FileOutputStream(new File(UPDATED_AGENT_FILE_PATH), true);
             FileOutputStream csvUnknownAgentsFile = new FileOutputStream(new File(UNKNOWN_AGENT_FILE_PATH), true);
             PrintWriter pw = new PrintWriter(unknown ? csvUnknownAgentsFile : csvUpdatedAgentInfoFile)) {

            FileOutputStream file = null;
            if (unknown) {
                file = csvUnknownAgentsFile;
            } else {
                file = csvUpdatedAgentInfoFile;
            }
            agentInfos.stream()
                    .forEach(pw::println);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static AgentInfo getAgentInfo(String e) {
        String[] agentLine = e.split(STR_COMMA);

        AgentInfo agent = new AgentInfo();
        agent.setAgentId(agentLine[1]);
        agent.setAgentName(agentLine[0]);
        //agent.setManager(agentLine[4]);
        //agent.setEmailId(agentLine[3]);
        //agent.setCompanyName(agentLine[5]);
        agent.setAllLinksToQuery(agentLine[2]);
        return agent;
    }

    private static void signInToPinergy(WebDriver driver) {

        driver.get(PINERGY_SIGN_IN_URL);

        System.out.println("INFO:Sign In to " + driver.getTitle());

        //Click ok for cookie consent
        driver.findElement(By.xpath(XPATH_COOKIE_CONSENT_OK)).click();

        WebElement username = driver.findElement(By.name("user_name"));
        WebElement password = driver.findElement(By.name("pass"));

        username.sendKeys(USERNAME);
        password.sendKeys(PASSWORD);

        driver.findElement(By.xpath(XPATH_SUBMIT)).click();
    }

    private static void logOut(WebDriver driver) {
        driver.findElement(By.xpath(XPATH_LOGOUT)).click();
        driver.close();
    }

    private static boolean scrapeAgentInfo(WebDriver driver, AgentInfo agent) {
        try {
            WebElement emailId = driver.findElement(By.xpath(XPATH_EMAIL));
            agent.setEmailId(emailId.getText());

            WebElement companyName = driver.findElement(By.xpath(XPATH_COMPANY_NAME));
            agent.setCompanyName(companyName.getText());

            WebElement officeId = driver.findElement(By.xpath(XPATH_OFFICE_ID));
            agent.setOfficeId(officeId.getText().split(STR_SPACE)[2]);

            boolean isAgent = findIsAgent(driver,companyName,agent.getAgentName());
            agent.setIsAgent(getIsAgent(isAgent));

            return true;
        } catch (NoSuchElementException e) {
            System.out.println("ERROR: Unable to find agent " + agent.getAgentName());
        }
        return false;
    }

    private static String getIsAgent(boolean isAgent) {
        return isAgent ? STR_Y : STR_N;
    }

    /**
     * Check if agent's name appears in list of subscribers, if not then mark him as not an agent
     * @param driver
     * @param companyName
     * @param agentName
     * @return
     */
    public static boolean  findIsAgent(WebDriver driver, WebElement companyName, String agentName){
        companyName.click();
        List<WebElement> elements = driver.findElements(By.xpath(XPATH_SUBS));
        for(WebElement ele:elements) {
            if (agentName.equals(ele.getText())) {
                return true;
            }
        }
        return false;
    }


}
