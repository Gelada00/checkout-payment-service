package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.AcquiringBankException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class BankClient {

  private final RestTemplate restTemplate;
  private final String bankUrl;

  public BankClient(RestTemplate restTemplate, @Value("${bank.url}") String bankUrl) {
    this.restTemplate = restTemplate;
    this.bankUrl = bankUrl;
  }

  public boolean authorize(PostPaymentRequest request) {
    Map<String, Object> bankRequest = new HashMap<>();

    bankRequest.put("card_number", request.getCardNumber());
    bankRequest.put("expiry_date", request.getExpiryDate());
    bankRequest.put("currency", request.getCurrency());
    bankRequest.put("amount", request.getAmount());
    bankRequest.put("cvv", request.getCvv());

    try {
      Map<String, Object> bankResponse = restTemplate.postForObject(bankUrl, bankRequest,
          Map.class);

      if (bankResponse == null) {
        throw new AcquiringBankException("Acquiring bank returned an empty response");
      }
      return Boolean.TRUE.equals(bankResponse.get("authorized"));

    } catch (RestClientException exception) {
      throw new AcquiringBankException("Unable to communicate with acquiring bank");
    }
  }
}
