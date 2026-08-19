package com.checkout.payment.gateway.controller;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = new PostPaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2024);
    payment.setCardNumberLastFour(4321);

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Page not found"));
  }

  @Test
  void whenValidPaymentIsAuthorizedThenPaymentIsReturned() throws Exception {
    String request = """
      {
        "card_number": "2222405343248877",
        "expiry_month": 12,
        "expiry_year": 2027,
        "currency": "GBP",
        "amount": 100,
        "cvv": 123
      }
      """;

    mvc.perform(
            post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(8877))
        .andExpect(jsonPath("$.expiryMonth").value(12))
        .andExpect(jsonPath("$.expiryYear").value(2027))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100));
  }

  @Test
  void whenValidPaymentIsDeclinedThenDeclinedStatusIsReturned() throws Exception {
    String request = """
      {
        "card_number": "2222405343248876",
        "expiry_month": 12,
        "expiry_year": 2027,
        "currency": "GBP",
        "amount": 100,
        "cvv": 123
      }
      """;

    mvc.perform(
            post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(8876));
  }

  @Test
  void whenPaymentHasInvalidCardNumberThenRejectedStatusIsReturned() throws Exception {
    String request = """
      {
        "card_number": "123",
        "expiry_month": 12,
        "expiry_year": 2027,
        "currency": "GBP",
        "amount": 100,
        "cvv": 123
      }
      """;

    mvc.perform(
            post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.status")
                .value(PaymentStatus.REJECTED.getName())
        );
  }

  @Test
  void whenRequiredFieldIsMissingThenPaymentIsRejected() throws Exception {
    String request = """
      {
        "card_number": "2222405343248877",
        "expiry_month": 12,
        "expiry_year": 2027,
        "currency": "GBP",
        "amount": 100
      }
      """;

    mvc.perform(
            post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.status")
                .value(PaymentStatus.REJECTED.getName())
        );
  }
}
