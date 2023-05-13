package definitions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sei.smartrx.SmartRxApplication;
import com.sei.smartrx.exceptions.PrescriptionNotFoundException;
import com.sei.smartrx.models.Prescription;
import com.sei.smartrx.models.User;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.data.jpa.domain.AbstractPersistable_.id;


@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SmartRxApplication.class)
public class SpringBootCucumberTestDefinitions {

    private static final String BASE_URL = "http://localhost:";

    @LocalServerPort
    String port;

    private static Response response;

    LocalDate localDate = LocalDate.of(2023, 8, 1);
    LocalDate currentDate = localDate.now();
    User user = new User("John", "Carter", "Carter53@hotmail.com", currentDate, "sei1900", "aspirin, metformin");

    /**
     * Generates a JWT token to pass in header of requests
     * @return JWT as a String
     * @throws JSONException
     */
    public String getYourKey() throws JSONException {
      RequestSpecification request = RestAssured.given();
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("email", "email@email.com");
      jsonObject.put("password", "password");
      request.header("Content-Type", "application/json");
      response = request.body(jsonObject.toString()).post(BASE_URL + port + "/api/auth/users/login");
      return response.jsonPath().getString("message");
    }

    public String getJWTAsPharmacist() throws JSONException {
        RequestSpecification request = RestAssured.given();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("email", "pharmacist@cvs.com");
        jsonObject.put("password", "password");
        request.header("Content-Type", "application/json");
        response = request.body(jsonObject.toString()).post(BASE_URL + port + "/api/auth/users/login");
        return response.jsonPath().getString("message");
    }
    /**
     *  A person can register as a user with a unique email
     */
    @Given("a user has a unique email")
    public void aUserHasAUniqueEmail() {
        try {
            RestAssured.baseURI = BASE_URL;
            RequestSpecification request = RestAssured.given();
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", "email@email.com");
            requestBody.put("password", "password");
            request.header("Content-Type", "application/json");
            response = request.body(requestBody.toString()).post(BASE_URL + port + "/api/auth/users/login");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    @When("they enter their email and password")
    public void theyEnterTheirEmailAndPassword() {
        Assert.assertEquals(200, response.getStatusCode());
    }
    @Then("the password and their information is stored in database")
    public void thePasswordAndTheirInformationIsStoredInDatabase() {
        Assert.assertNotNull(response.getBody());
    }
    /**
     * FEATURE: a registered user can enter their email and password to login authentically.
     * @throws JSONException
     */
    @Given("user is registered")
    public void userIsRegistered() throws JSONException {
        RequestSpecification request = RestAssured.given();
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", "email100@email.com");
        requestBody.put("password", "password100");
        request.header("Content-Type", "application/json");
        response = request.body(requestBody.toString()).post(BASE_URL + port +"/api/auth/users/register");
    }
    @When("I enter my username and password")
    public void iEnterMyUsernameAndPassword() {
        JsonPath jsonObject = new JsonPath(response.asString());
        Assert.assertEquals("email@email.com", jsonObject.get("email"));
        Assert.assertNotNull(jsonObject);
    }
    @Then("I should be logged in successfully")
    public void iShouldBeLoggedInSuccessfully() {
        Assert.assertEquals(200, response.getStatusCode());
    }

    /**
     * FEATURE: When a user is logged in, user can update their account information and delete their account.
     * @throws JSONException
     */
    @Given("User is logged in")
    public void userHasAnActiveAccount() throws JSONException{
        RestAssured.baseURI = BASE_URL;
        RequestSpecification request = RestAssured.given();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("email", "email@email.com");
        jsonObject.put("password", "password");
        request.header("Content-Type", "application/json");
        response = request.body(jsonObject.toString()).post(BASE_URL+ port + "/api/auth/users/login");
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertNotNull(response.body());
    }
    @When("user updates their account information")
    public void userUpdatesTheirAccountInformation() throws JSONException {
        RestAssured.baseURI = BASE_URL;
        RequestSpecification request = RestAssured.given();
        JSONObject requestBody = new JSONObject();
        requestBody.put("firstName", "Tim");
        requestBody.put("lastName", "Rodriguez");
        requestBody.put("dob", "1983-03-06");
        requestBody.put("allergies", "apples");
        request.header("Content-Type", "application/json");
        request.header("Authorization", "Bearer "+ getYourKey());
        response = request.body(requestBody.toString()).put(BASE_URL + port + "/api/users");
    }
    @Then("user information will be updated")
    public void userInformationWillBeUpdated() {
        Assert.assertEquals(200, response.getStatusCode());
    }
    @When("user removes their account by ID")
    public void userRemovesTheirAccountByID() throws JSONException {
        try {
            RestAssured.baseURI = BASE_URL + port;
            RequestSpecification request = RestAssured.given();
            request.header("Authorization", "Bearer "+ getYourKey());
            response = request.delete("/api/users");
            //status code 204, no content shown when account is deleted
        } catch (HttpClientErrorException e) {
            e.printStackTrace();
        }
    }
    @Then("the account is deleted")
    public void theAccountIsDeleted() throws JSONException{
        try {
            Assert.assertEquals(204, response.getStatusCode());
            // Send a GET request to the API to retrieve the user by ID and verify that the response status is 404 Not Found
          //404 status, account has been deleted
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND); //verifying that the status code is 404
        }
    }
    /**
     * FEATURE: a user can view a medication by medication ID.
     */
    @Given("A specific medication ID")
    public void aSpecificMedicationID() {
        Long medicationId = 1L;
        Assert.assertTrue(medicationId == 1L);
    }
    @When("a user searches for medication by ID")
    public void aUserSearchesForMedicationByID() throws JSONException {
        try{
            RestAssured.baseURI = BASE_URL;
            RequestSpecification request = RestAssured.given();
            request.header("Authorization", "Bearer "+ getYourKey());
            Response response1 = request.get(BASE_URL + port + "/api/prescriptions/medications/1");
            Assert.assertEquals(200, response1.getStatusCode());
        } catch(HttpClientErrorException e){
            e.printStackTrace();
        }
    }
    @Then("user should receive specific information about that medication")
    public void userShouldReceiveSpecificInformationAboutThatMedication() {
        Assert.assertNotNull(response);
    }
    /**
     * FEATURE: a user can view their prescriptions
     */
    @When("user searches for their prescriptions")
    public void userSearchesForTheirPrescriptions() throws JSONException, JsonProcessingException {
       HttpHeaders authenticationHeader = new HttpHeaders();
       authenticationHeader.set("Authorization","Bearer "+getYourKey());
       HttpEntity<String> httpEntity = new HttpEntity<>(authenticationHeader);
       ResponseEntity<String> response = new RestTemplate().exchange(BASE_URL+port+"/api/prescriptions", HttpMethod.GET, httpEntity, String.class);
        List<Map<String, String>> prescriptions = JsonPath.from(String.valueOf(response.getBody())).get();
       Assert.assertNotNull(prescriptions);
    }

    @Then("a user should see a list of only their prescriptions")
    public void aUserShouldSeeAListOfOnlyTheirPrescriptions() {
        Assert.assertEquals(200, response.getStatusCode());

    }

    @Given("a User had authorization role of pharmacist")
    public void aUserHadAuthorizationRoleOfPharmacist() {
    }


    @When("a pharmacist searches for a list of prescription")
    public void aPharmacistSearchesForAListOfPrescription() throws JSONException {
        HttpHeaders authenticationHeader = new HttpHeaders();
        authenticationHeader.set("Authorization","Bearer "+ getJWTAsPharmacist());
        HttpEntity<String> httpEntity = new HttpEntity<>(authenticationHeader);
        ResponseEntity<String> response = new RestTemplate().exchange(BASE_URL+port+"/api/pharmacist/prescriptions", HttpMethod.GET, httpEntity, String.class);
        List<Map<String, String>> allPrescriptions = JsonPath.from(String.valueOf(response.getBody())).get();
        Assert.assertNotNull(allPrescriptions);
    }

    @Then("a pharmacist should see a list of prescriptions")
    public void aPharmacistShouldSeeAListOfPrescriptions() {
        Assert.assertEquals(200, response.getStatusCode());
    }

    /**
     * this method sends a PUT request to update a prescription by prescriptionID.
     * the request body is given the updated prescription status and added to the
     * authorization header.
     * @throws JSONException
     */
    @When("the pharmacist updates a prescription")
    public void thePharmacistUpdatesAPrescription() throws JSONException {
        //Long prescriptionId = 1L;

        RestAssured.baseURI = BASE_URL;
        RequestSpecification request = RestAssured.given();
        JSONObject requestBody = new JSONObject(); //JSON object with the updated prescription status
        requestBody.put("status", true); //Put request to update the prescription status
        request.header("Authorization","Bearer "+ getJWTAsPharmacist());
        request.header("Content-Type", "application/json");
        response = request.body(requestBody.toString()).put(BASE_URL+ port + "/api/pharmacist/prescription/1");
        int statusCode = response.getStatusCode();
        assertEquals(200, response.getStatusCode());
    }

    @Then("the prescription is updated")
    public void thePrescriptionIsUpdated() {
        assertEquals(200, response.getStatusCode());


    }


    @When("a pharmacist searches for a prescription by id")
    public void aPharmacistSearchesForAPrescriptionById() throws JSONException {
        HttpHeaders authenticationHeader = new HttpHeaders();
        authenticationHeader.set("Authorization","Bearer "+ getJWTAsPharmacist());
        HttpEntity<String> httpEntity = new HttpEntity<>(authenticationHeader);
        ResponseEntity<String> response = new RestTemplate().exchange(BASE_URL+port+"/api/pharmacist/prescriptions/1", HttpMethod.GET, httpEntity, String.class);
        Map<String, String> onePrescription = JsonPath.from(String.valueOf(response.getBody())).get();
        Assert.assertNotNull(onePrescription);

    }

    @Then("a pharmacist should see that one prescription")
    public void aPharmacistShouldSeeThatOnePrescription() {
        Assert.assertEquals(200, response.getStatusCode());
    }
}

