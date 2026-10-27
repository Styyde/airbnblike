package com.example.demo;

import com.example.demo.Enums.*;
import com.example.demo.entities.*;
import com.example.demo.repositories.*;
import com.example.demo.services.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(properties = {"app.payment.mode=demo", "app.booking.expiry-enabled=false", "app.upload-dir=./target/test-uploads"})
@AutoConfigureMockMvc
@Testcontainers
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class BackendIT {
    @Container static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0.16");
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("app.jwt.secret", () -> "test-only-secret-012345678901234567890123456789012345678901234567890123456789");
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserService users;
    @Autowired PropertyService properties;
    @Autowired BookingService bookings;
    @Autowired UserRepository userRepository;
    @Autowired PropertyRepository propertyRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired AvailabilityRepository availabilityRepository;
    @Autowired ReviewRepository reviewRepository;
    @Autowired ConversationRepository conversationRepository;
    @Autowired MessageRepository messageRepository;
    @MockitoSpyBean PaymentGatewayService gateway;
    User host, guest, outsider;
    Property property;
    String hostToken, guestToken, outsiderToken;
    LocalDate start = LocalDate.now().plusDays(10);

    @BeforeEach void setup() throws Exception {
        reset(gateway);
        messageRepository.deleteAll(); conversationRepository.deleteAll(); reviewRepository.deleteAll();
        paymentRepository.deleteAll(); bookingRepository.deleteAll(); availabilityRepository.deleteAll();
        propertyRepository.deleteAll(); userRepository.deleteAll();
        host = users.registerUser("host", "host@example.org", "password123");
        host = users.addRole(host.getId(), Role.ROLE_OWNER);
        guest = users.registerUser("guest", "guest@example.org", "password123");
        outsider = users.registerUser("outsider", "outsider@example.org", "password123");
        hostToken = login(host); guestToken = login(guest); outsiderToken = login(outsider);
        property = properties.createProperty(Property.builder().ownerId(host.getId()).title("Maison")
                .type(PropertyType.ENTIRE_HOME).address("1 rue test").city("Paris").country("France")
                .pricePerNight(new BigDecimal("0.10")).maxGuests(2).bedrooms(1).build());
    }

    String login(User user) throws Exception {
        var response = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", user.getEmail(), "password", "password123")))).andReturn();
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        return json.readTree(response.getResponse().getContentAsString()).path("data").path("token").asText();
    }
    Map<String,Object> request(LocalDate from, LocalDate to, int guests) {
        return Map.of("propertyId", property.getId(), "userId", outsider.getId(), "startDate", from.toString(),
                "endDate", to.toString(), "numberOfGuests", guests);
    }
    JsonNode createBooking() throws Exception {
        var result = mvc.perform(post("/api/bookings").header("Authorization", "Bearer " + guestToken)
                .contentType("application/json").content(json.writeValueAsString(request(start, start.plusDays(2), 2)))).andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        return json.readTree(result.getResponse().getContentAsString());
    }
    void accept(String id) throws Exception { status(200, put("/api/bookings/" + id + "/accept").header("Authorization", "Bearer " + hostToken)); }
    JsonNode pay(String bookingId) throws Exception {
        if (bookingRepository.findById(bookingId).orElseThrow().getStatus() == BookingStatus.REQUESTED) accept(bookingId);
        var result = mvc.perform(post("/api/payments/process/" + bookingId).param("method", "CREDIT_CARD")
                .header("Authorization", "Bearer " + guestToken)).andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
        return json.readTree(result.getResponse().getContentAsString());
    }
    void status(int expected, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        var response = mvc.perform(request).andReturn().getResponse();
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(expected);
    }

    @Test void registrationHostOnboardingAndRoleValidation() throws Exception {
        var body = new LinkedHashMap<String,Object>();
        body.put("username","new-user"); body.put("email","NEW@example.org"); body.put("password","password123");
        body.put("confirmPassword","wrong");
        status(400, post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(body)));
        body.put("confirmPassword","password123");
        status(201, post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(body)));
        assertThat(userRepository.findByEmail("new@example.org")).isPresent();
        status(409, post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(body)));
        status(200, post("/api/users/me/host").header("Authorization", "Bearer " + guestToken));
        status(200, get("/api/properties/owner/me").header("Authorization", "Bearer " + guestToken));
        users.addRole(host.getId(), Role.ROLE_ADMIN);
        status(400, put("/api/admin/users/" + guest.getId() + "/role").param("role","UNKNOWN")
                .header("Authorization", "Bearer " + hostToken));
    }

    @Test void identityDecimalPricingAndCheckoutBoundary() throws Exception {
        availabilityRepository.save(new Availability(null, property.getId(), start, true, new BigDecimal("0.20")));
        availabilityRepository.save(new Availability(null, property.getId(), start.plusDays(2), false, new BigDecimal("999")));
        var booking = createBooking();
        assertThat(booking.path("userId").asText()).isEqualTo(guest.getId());
        assertThat(booking.path("totalPrice").decimalValue()).isEqualByComparingTo("0.30");
        status(409, post("/api/bookings").header("Authorization", "Bearer " + guestToken).contentType("application/json")
                .content(json.writeValueAsString(request(start, start.plusDays(2), 1))));
    }

    @Test void paymentAndRefundAreIdempotentAndProtectPrivateData() throws Exception {
        String id = createBooking().path("id").asText();
        status(403, get("/api/bookings/" + id).header("Authorization", "Bearer " + outsiderToken));
        status(403, get("/api/bookings/property/" + property.getId()).header("Authorization", "Bearer " + outsiderToken));
        status(403, post("/api/payments/process/" + id).param("method","CREDIT_CARD").header("Authorization", "Bearer " + outsiderToken));
        var payment = pay(id);
        assertThat(pay(id).path("id").asText()).isEqualTo(payment.path("id").asText());
        assertThat(paymentRepository.count()).isEqualTo(1);
        verify(gateway, times(1)).charge(any(), any(), any());
        status(403, get("/api/payments/booking/" + id).header("Authorization", "Bearer " + outsiderToken));
        status(200, get("/api/payments/booking/" + id).header("Authorization", "Bearer " + hostToken));
        status(409, put("/api/bookings/" + id + "/cancel").header("Authorization", "Bearer " + guestToken));
        String refund = "/api/payments/" + payment.path("id").asText() + "/refund";
        status(403, post(refund).header("Authorization", "Bearer " + outsiderToken));
        status(200, post(refund).header("Authorization", "Bearer " + guestToken));
        status(200, post(refund).header("Authorization", "Bearer " + guestToken));
        assertThat(bookingRepository.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(paymentRepository.findById(payment.path("id").asText()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(bookings.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test void concurrentBookingsCannotReserveTheSameNights() throws Exception {
        var barrier = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> attempt = () -> {
                barrier.await(10, TimeUnit.SECONDS);
                return mvc.perform(post("/api/bookings").header("Authorization", "Bearer " + guestToken)
                        .contentType("application/json").content(json.writeValueAsString(request(start, start.plusDays(2), 1))))
                        .andReturn().getResponse().getStatus();
            };
            var first = executor.submit(attempt); var second = executor.submit(attempt);
            assertThat(List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(bookingRepository.count()).isEqualTo(1);
        } finally { executor.shutdownNow(); }
    }

    @Test void failedGatewayRollsBackPaymentAndBookingTogether() throws Exception {
        String id = createBooking().path("id").asText();
        accept(id);
        doThrow(com.example.demo.exceptions.ApiException.conflict("Défaillance simulée")).when(gateway).charge(any(), any(), any());
        status(409, post("/api/payments/process/" + id).param("method", "CREDIT_CARD").header("Authorization", "Bearer " + guestToken));
        assertThat(paymentRepository.count()).isZero();
        assertThat(bookingRepository.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.PENDING);
        reset(gateway);
        pay(id);
        assertThat(bookings.getTotalRevenue()).isEqualByComparingTo("0.20");
    }

    @Test void invalidStaysCapacityAndSelfBookingAreRejected() throws Exception {
        for (var body : List.of(request(start,start,1), request(start,start.minusDays(1),1),
                request(start,start.plusDays(1),3), request(start,start.plusDays(1),0))) {
            status(400, post("/api/bookings").header("Authorization", "Bearer " + guestToken)
                    .contentType("application/json").content(json.writeValueAsString(body)));
        }
        status(400, post("/api/bookings").header("Authorization", "Bearer " + hostToken)
                .contentType("application/json").content(json.writeValueAsString(request(start,start.plusDays(1),1))));
        status(401, post("/api/bookings").contentType("application/json").content("{}"));
    }

    @Test void calendarRequiresOwnerAndCannotBlockBookedNights() throws Exception {
        accept(createBooking().path("id").asText());
        String body = json.writeValueAsString(Map.of("propertyId",property.getId(),"date",start.toString(),"isAvailable",false));
        status(403, put("/api/availabilities").header("Authorization", "Bearer " + outsiderToken).contentType("application/json").content(body));
        status(409, put("/api/availabilities").header("Authorization", "Bearer " + hostToken).contentType("application/json").content(body));
        status(200, put("/api/availabilities").header("Authorization", "Bearer " + hostToken).contentType("application/json")
                .content(json.writeValueAsString(Map.of("propertyId",property.getId(),"date",start.plusDays(2).toString(),"isAvailable",false))));
    }

    @Test void messagingUsesJwtAndArchivesOnlyForTheCurrentUser() throws Exception {
        var created = mvc.perform(post("/api/conversations").param("user1Id",guest.getId()).param("user2Id",host.getId())
                .header("Authorization", "Bearer " + guestToken)).andReturn();
        assertThat(created.getResponse().getStatus()).isEqualTo(200);
        String id = json.readTree(created.getResponse().getContentAsString()).path("id").asText();
        String body = json.writeValueAsString(Map.of("senderId",host.getId(),"receiverId",host.getId(),"content","Bonjour"));
        status(200, post("/api/conversations/" + id + "/messages").header("Authorization", "Bearer " + guestToken)
                .contentType("application/json").content(body));
        assertThat(messageRepository.findByConversationId(id).get(0).getSenderId()).isEqualTo(guest.getId());
        status(403, get("/api/conversations/" + id + "/messages").param("userId",guest.getId()).header("Authorization", "Bearer " + outsiderToken));
        status(403, post("/api/conversations/" + id + "/messages").header("Authorization", "Bearer " + outsiderToken)
                .contentType("application/json").content(body));
        status(403, get("/api/conversations/user/" + guest.getId()).header("Authorization", "Bearer " + outsiderToken));
        status(204, put("/api/conversations/" + id + "/archive").header("Authorization", "Bearer " + guestToken));
        var guestList = mvc.perform(get("/api/conversations/user/" + guest.getId()).header("Authorization", "Bearer " + guestToken)).andReturn();
        var hostList = mvc.perform(get("/api/conversations/user/" + host.getId()).header("Authorization", "Bearer " + hostToken)).andReturn();
        assertThat(json.readTree(guestList.getResponse().getContentAsString())).isEmpty();
        assertThat(json.readTree(hostList.getResponse().getContentAsString())).hasSize(1);
    }

    @Test void completedStaysAllowExactlyOneGuestReview() throws Exception {
        var booking = bookingRepository.save(Booking.builder().propertyId(property.getId()).userId(guest.getId())
                .startDate(LocalDate.now().minusDays(3)).endDate(LocalDate.now().minusDays(1)).numberOfGuests(1)
                .totalPrice(new BigDecimal("0.20")).status(BookingStatus.CONFIRMED).build());
        status(403, put("/api/bookings/" + booking.getId() + "/complete").header("Authorization", "Bearer " + outsiderToken));
        status(200, put("/api/bookings/" + booking.getId() + "/complete").header("Authorization", "Bearer " + hostToken));
        String body = json.writeValueAsString(Map.of("bookingId",booking.getId(),"authorId",outsider.getId(),"rating",5,"comment","Très bien"));
        status(201, post("/api/reviews").header("Authorization", "Bearer " + guestToken).contentType("application/json").content(body));
        assertThat(reviewRepository.findByBookingId(booking.getId()).orElseThrow().getAuthorId()).isEqualTo(guest.getId());
        status(409, post("/api/reviews").header("Authorization", "Bearer " + guestToken).contentType("application/json").content(body));
        status(200, get("/api/reviews/property/" + property.getId()));
    }

    @Test void deletedUsersLoseAccessAndPasswordsNeverLeaveTheApi() throws Exception {
        users.addRole(host.getId(), Role.ROLE_ADMIN);
        var result = mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + hostToken)).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("password", "$2a", "$2b");
        users.deleteUser(guest.getId());
        status(401, get("/api/users/me").header("Authorization", "Bearer " + guestToken));
    }

    @Test void pendingBookingsExpireAndReleaseTheDates() throws Exception {
        String id = createBooking().path("id").asText();
        var booking = bookingRepository.findById(id).orElseThrow();
        booking.setExpiresAt(Instant.now().minusSeconds(60));
        bookingRepository.save(booking);
        status(409, post("/api/payments/process/" + id).param("method","CREDIT_CARD").header("Authorization", "Bearer " + guestToken));
        bookings.expireBooking(id);
        assertThat(bookingRepository.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.EXPIRED);
        createBooking();
    }

    @Test void propertyInputsCannotOverwriteIdentityAndDeletedListingsAreHidden() throws Exception {
        var body = new LinkedHashMap<String,Object>();
        body.put("id",property.getId()); body.put("ownerId",outsider.getId()); body.put("status","DELETED");
        body.put("title","Appartement"); body.put("type","ENTIRE_HOME"); body.put("address","2 rue test");
        body.put("city","Paris"); body.put("country","France"); body.put("pricePerNight",50);
        body.put("maxGuests",2); body.put("bedrooms",1); body.put("imageUrls",List.of("https://example.org/photo.jpg"));
        var result = mvc.perform(post("/api/properties").header("Authorization", "Bearer " + hostToken)
                .contentType("application/json").content(json.writeValueAsString(body))).andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        var created = json.readTree(result.getResponse().getContentAsString());
        assertThat(created.path("id").asText()).isNotEqualTo(property.getId());
        assertThat(created.path("ownerId").asText()).isEqualTo(host.getId());
        assertThat(created.path("status").asText()).isEqualTo("AVAILABLE");
        status(204, delete("/api/properties/" + property.getId()).header("Authorization", "Bearer " + hostToken));
        status(404, get("/api/properties/" + property.getId()));
    }

    @Test void hostChoosesAmongRequestsAndOnlyPaymentClosesCompetitors() throws Exception {
        var first=createBooking();
        String firstId=first.path("id").asText();
        assertThat(first.path("status").asText()).isEqualTo("REQUESTED");
        assertThat(Instant.parse(first.path("expiresAt").asText())).isBetween(Instant.now().plusSeconds(172700),Instant.now().plusSeconds(172900));
        var result=mvc.perform(post("/api/bookings").header("Authorization","Bearer "+outsiderToken)
                .contentType("application/json").content(json.writeValueAsString(request(start,start.plusDays(2),1)))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        String other=json.readTree(result.getResponse().getContentAsString()).path("id").asText();
        status(409,post("/api/payments/process/"+firstId).param("method","CREDIT_CARD").header("Authorization","Bearer "+guestToken));
        verify(gateway,never()).charge(any(),any(),any());
        status(403,put("/api/bookings/"+firstId+"/accept").header("Authorization","Bearer "+guestToken));
        status(403,put("/api/bookings/"+firstId+"/reject").header("Authorization","Bearer "+outsiderToken));
        var publicQuote=mvc.perform(get("/api/properties/"+property.getId()+"/quote").param("startDate",start.toString())
                .param("endDate",start.plusDays(2).toString()).param("guests","1")).andReturn();
        assertThat(json.readTree(publicQuote.getResponse().getContentAsString()).path("available").asBoolean()).isTrue();
        accept(firstId);
        var expiry=bookingRepository.findById(firstId).orElseThrow().getExpiresAt();
        assertThat(expiry).isBetween(Instant.now().plusSeconds(1700),Instant.now().plusSeconds(1810));
        accept(firstId);
        assertThat(bookingRepository.findById(firstId).orElseThrow().getExpiresAt()).isEqualTo(expiry);
        status(409,put("/api/bookings/"+other+"/accept").header("Authorization","Bearer "+hostToken));
        assertThat(bookingRepository.findById(other).orElseThrow().getStatus()).isEqualTo(BookingStatus.REQUESTED);
        pay(firstId);
        assertThat(bookingRepository.findById(other).orElseThrow().getStatus()).isEqualTo(BookingStatus.REJECTED);
        var hostList=mvc.perform(get("/api/bookings/host/me").header("Authorization","Bearer "+hostToken)).andReturn();
        assertThat(hostList.getResponse().getStatus()).isEqualTo(200);
        var list=json.readTree(hostList.getResponse().getContentAsString());
        assertThat(list).hasSize(2);
        assertThat(list.get(0).path("travelerName").asText()).isNotBlank();
        assertThat(list.get(0).path("propertyTitle").asText()).isEqualTo("Maison");
    }

    @Test void simultaneousHostAcceptancesHaveOnlyOneWinner() throws Exception {
        String first=createBooking().path("id").asText();
        var result=mvc.perform(post("/api/bookings").header("Authorization","Bearer "+outsiderToken)
                .contentType("application/json").content(json.writeValueAsString(request(start,start.plusDays(2),1)))).andReturn();
        String second=json.readTree(result.getResponse().getContentAsString()).path("id").asText();
        var barrier=new CyclicBarrier(2);
        var executor=Executors.newFixedThreadPool(2);
        try {
            var a=executor.submit(()->{barrier.await(10,TimeUnit.SECONDS);return mvc.perform(put("/api/bookings/"+first+"/accept").header("Authorization","Bearer "+hostToken)).andReturn().getResponse().getStatus();});
            var b=executor.submit(()->{barrier.await(10,TimeUnit.SECONDS);return mvc.perform(put("/api/bookings/"+second+"/accept").header("Authorization","Bearer "+hostToken)).andReturn().getResponse().getStatus();});
            assertThat(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
            assertThat(bookingRepository.countByStatus(BookingStatus.PENDING)).isEqualTo(1);
            assertThat(bookingRepository.countByStatus(BookingStatus.REQUESTED)).isEqualTo(1);
        } finally {executor.shutdownNow();}
    }

    @Test void expiredAcceptanceReleasesDatesAndRequestedPriceIsFrozen() throws Exception {
        String id=createBooking().path("id").asText();
        var result=mvc.perform(post("/api/bookings").header("Authorization","Bearer "+outsiderToken)
                .contentType("application/json").content(json.writeValueAsString(request(start,start.plusDays(2),1)))).andReturn();
        String second=json.readTree(result.getResponse().getContentAsString()).path("id").asText();
        accept(id);
        var expired=bookingRepository.findById(id).orElseThrow();
        expired.setExpiresAt(Instant.now().minusSeconds(1));
        bookingRepository.save(expired);
        bookings.expireBooking(id);
        availabilityRepository.save(new Availability(null,property.getId(),start,true,new BigDecimal("999")));
        accept(second);
        assertThat(bookingRepository.findById(second).orElseThrow().getTotalPrice()).isEqualByComparingTo("0.20");
        assertThat(bookingRepository.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.EXPIRED);
    }

    @Test void publicSearchQuoteAndCalendarDoNotRevealTravelers() throws Exception {
        status(200,get("/api/search/properties"));
        status(200,get("/api/search/properties").param("city","par").param("guests","2"));
        status(400,get("/api/search/properties").param("startDate",start.toString()));
        status(400,get("/api/search/properties").param("minPrice","100").param("maxPrice","1"));
        String id=createBooking().path("id").asText();
        accept(id);
        var response=mvc.perform(get("/api/properties/"+property.getId()+"/calendar").param("startDate",start.toString()).param("endDate",start.plusDays(3).toString())).andReturn();
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        String body=response.getResponse().getContentAsString();
        assertThat(body).doesNotContain(guest.getId(),"userId","travelerName","bookingId",id);
        var days=json.readTree(body);
        assertThat(days).hasSize(3);
        assertThat(days.get(0).path("available").asBoolean()).isFalse();
        assertThat(days.get(2).path("available").asBoolean()).isTrue();
    }

    @Test void onlyTheHostCanRejectWithABoundedReason() throws Exception {
        String id=createBooking().path("id").asText();
        status(403,put("/api/bookings/"+id+"/reject").header("Authorization","Bearer "+guestToken));
        status(400,put("/api/bookings/"+id+"/reject").header("Authorization","Bearer "+hostToken)
                .contentType("application/json").content(json.writeValueAsString(Map.of("reason","x".repeat(501)))));
        status(200,put("/api/bookings/"+id+"/reject").header("Authorization","Bearer "+hostToken)
                .contentType("application/json").content(json.writeValueAsString(Map.of("reason","Un imprévu nous empêche de vous accueillir."))));
        assertThat(bookingRepository.findById(id).orElseThrow().getDecisionReason()).contains("imprévu");
        status(409,post("/api/payments/process/"+id).param("method","CREDIT_CARD").header("Authorization","Bearer "+guestToken));
    }

    @Test void imageUploadValidatesContentOwnershipAndServesReencodedPhotos() throws Exception {
        var bytes=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(16,16,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
        var file=new org.springframework.mock.web.MockMultipartFile("files","chambre.png","image/png",bytes.toByteArray());
        status(403,multipart("/api/uploads/images").file(file).header("Authorization","Bearer "+guestToken));
        status(400,multipart("/api/uploads/images").file(new org.springframework.mock.web.MockMultipartFile("files","fake.png","image/png","not an image".getBytes())).header("Authorization","Bearer "+hostToken));
        var response=mvc.perform(multipart("/api/uploads/images").file(file).header("Authorization","Bearer "+hostToken)).andReturn();
        assertThat(response.getResponse().getStatus()).as(response.getResponse().getContentAsString()).isEqualTo(201);
        String url=json.readTree(response.getResponse().getContentAsString()).get(0).path("url").asText();
        status(200,get(url));
        var body=Map.of("title","Une belle adresse","type","ENTIRE_HOME","address","12 rue Test","city","Rabat","country","Maroc",
                "pricePerNight",500,"maxGuests",2,"bedrooms",1,"imageUrls",List.of(url));
        users.addRole(outsider.getId(),Role.ROLE_OWNER);
        status(400,post("/api/properties").header("Authorization","Bearer "+outsiderToken).contentType("application/json").content(json.writeValueAsString(body)));
        assertThat(propertyRepository.count()).isEqualTo(1);
        status(201,post("/api/properties").header("Authorization","Bearer "+hostToken).contentType("application/json").content(json.writeValueAsString(body)));
        assertThat(propertyRepository.count()).isEqualTo(2);
    }
}
