package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequiredArgsConstructor
public class CreditCardController {
    private static final Logger logger = LoggerFactory.getLogger(CreditCardController.class);

    // TODO: wire in CreditCard repository here (~1 line)
    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/credit-card")
    public ResponseEntity<?> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length
        try {
            // Find the user by the provided userId. If no user is found, return a BadRequest.
            Optional<User> userOptional = userRepository.findById(payload.getUserId());
            if (!userOptional.isPresent()) {
                logger.error("Failed to add credit card: User not found with ID {}", payload.getUserId());
                return ResponseEntity.badRequest().body("User not found with ID " + payload.getUserId());
            }

            // Validate that the card number is provided
            if (payload.getCardNumber() == null || payload.getCardNumber().trim().isEmpty()) {
                logger.error("Failed to add credit card: Card number is required");
                return ResponseEntity.badRequest().body("Card number is required");
            }

            // Log details about the credit card to be created.
            logger.info("Creating credit card with Number: {}, Issuance Bank: {}", payload.getCardNumber(), payload.getCardIssuanceBank());

            // Create a new instance of CreditCard and set its properties from the payload.
            CreditCard newCard = new CreditCard();
            newCard.setNumber(payload.getCardNumber());
            newCard.setIssuanceBank(payload.getCardIssuanceBank());
            newCard.setOwner(userOptional.get());

            // Log details just before saving the new credit card.
            logger.info("Saving credit card with Number: {}, Issuance Bank: {}", newCard.getNumber(), newCard.getIssuanceBank());

            // Save the new credit card to the repository.
            CreditCard savedCard = creditCardRepository.save(newCard);

            // Log the successful creation of the credit card.
            logger.info("Credit card created successfully with Number: {}", savedCard.getNumber());

            // Return the number of the newly created credit card in the response, indicating success.
            return ResponseEntity.ok(savedCard.getNumber());

        } catch (Exception e) {
            // Log any exceptions that occur during the process.
            logger.error("Error creating credit card", e);
            // Return a BadRequest response if an exception occurs.
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null

        try {
            // Attempt to find the user by their ID.
            User user = userRepository.findById(userId).orElse(null);

            // Check if the user object is null
            if (user == null) {
                // Log an error indicating that no user was found
                logger.error("No user found with ID {}", userId);
                return ResponseEntity.badRequest().body(Collections.emptyList());// Returning empty list in case user not found
            }

            // Convert the list of CreditCard entities associated with the user to CreditCardView
            List<CreditCardView> cards = user.getCreditCards().stream()
                    .map(card -> new CreditCardView(card.getIssuanceBank(), card.getNumber()))
                    .collect(Collectors.toList());

            // Log that the credit cards are successfully retrieved for the user.
            logger.info("Retrieving all credit cards for user ID {}", userId);
            // Return the list of CreditCardViews with an OK status, indicating successful retrieval.
            return ResponseEntity.ok(cards);

        } catch (Exception e) {
            // Log the exception details to help diagnose issues that occur during the retrieval process.
            logger.error("Error retrieving credit cards for user ID {}", userId, e);
            // Return an InternalServerError response, indicating that an unexpected error occurred.
            return ResponseEntity.internalServerError().body(Collections.emptyList());
        }
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request
        try {
            // This method uses findAll() and streams to find the correct credit card
            CreditCard creditCard = creditCardRepository.findAll()
                    .stream()
                    .filter(card -> card.getNumber().equals(creditCardNumber))
                    .findFirst()
                    .orElse(null);

            if (creditCard != null && creditCard.getOwner() != null) {
                // If credit card is found and it has an owner, return the owner's ID
                return ResponseEntity.ok(creditCard.getOwner().getId());
            } else if (creditCard != null) {
                // Found the card, but no associated user
                logger.info("Credit card found but no associated user: {}", creditCardNumber);
                return ResponseEntity.badRequest().build();
            } else {
                // No card could be found with the provided number
                logger.info("No credit card found with number: {}", creditCardNumber);
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            // Log any exceptions that occur during the process
            logger.error("Error finding user by credit card number", e);
            // Respond with an internal server error, indicating something went wrong server-side
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/credit-card:update-balance")
    @Transactional
    public ResponseEntity<String> updateCreditCardBalance(@RequestBody UpdateBalancePayload[] payload) {
        //TODO: Given a list of transactions, update credit cards' balance history.
        //      1. For the balance history in the credit card
        //      2. If there are gaps between two balance dates, fill the empty date with the balance of the previous date
        //      3. Given the payload `payload`, calculate the balance different between the payload and the actual balance stored in the database
        //      4. If the different is not 0, update all the following budget with the difference
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
        //      Given a balance amount of {date: 4/11, amount: 110}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 100}]
        //      This is because
        //      1. You would first populate 4/11 with previous day's balance (4/10), so {date: 4/11, amount: 100}
        //      2. And then you observe there is a +10 difference
        //      3. You propagate that +10 difference until today
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //        is not associated with a card.

        // Given a list of transactions, update credit cards' balance history.
        try {
            logger.info("Starting to update credit card balances with payload size: {}", payload.length);
            // Iterate through each transaction in the payload
            for (UpdateBalancePayload update : payload) {
                logger.info("Processing update for card number: {}", update.getCreditCardNumber());
                // Fetch all credit cards from the repository
                List<CreditCard> allCards = creditCardRepository.findAll();
                logger.info("Fetched {} cards from the repository", allCards.size());

                // Find the credit card associated with the current transaction
                CreditCard creditCard = allCards.stream()
                        .filter(card -> card.getNumber().equals(update.getCreditCardNumber()))
                        .findFirst()
                        .orElse(null);
                // If the credit card is not found, return a BadRequest response
                if (creditCard == null) {
                    logger.error("Credit card not found for number: {}", update.getCreditCardNumber());
                    return ResponseEntity.badRequest().body("Credit card with number " + update.getCreditCardNumber() + " not found.");
                }

                // Update the balance history for the credit card with the transaction details
                logger.info("Found credit card, updating balance for date: {}", update.getBalanceDate());
                LocalDate balanceDate = update.getBalanceDate();
                double balanceAmount = update.getBalanceAmount();
                creditCard.updateBalanceHistory(balanceDate, balanceAmount);

                // Save the updated credit card to the repository
                logger.info("Updated local balance history, now saving credit card.");
                creditCardRepository.save(creditCard);
                logger.info("Saved credit card successfully.");
            }
            logger.info("All balances updated successfully.");
            return ResponseEntity.ok("Credit card balances updated successfully.");
        } catch (Exception e) {
            // Log any exceptions that occur during the process
            logger.error("Failed to update balance due to an error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating credit card balances: " + e.getMessage());
        }
    }

    public CreditCard findCreditCardByNumber(String number) {
        return creditCardRepository.findAll()
                .stream()
                .filter(card -> card.getNumber().equals(number))
                .findFirst()
                .orElse(null);
    }

    @GetMapping("/credit-card:balances")
    public ResponseEntity<?> getCreditCardBalances(@RequestParam String cardNumber) {
        // Retrieve balances for a given credit card number
        try {
            // Check if the provided card number is valid
            if (cardNumber == null || cardNumber.trim().isEmpty()) {
                logger.warn("Invalid card number provided");
                return ResponseEntity.badRequest().body("Invalid card number provided.");
            }

            // Attempt to find the credit card associated with the provided card number
            logger.info("Attempting to retrieve balances for card number: {}", cardNumber);
            CreditCard card = findCreditCardByNumber(cardNumber);

            // If no credit card is found, return a NotFound response
            if (card == null) {
                logger.warn("No credit card found with number: {}", cardNumber);
                return ResponseEntity.notFound().build();
            }

            // Get the balances associated with the credit card
            List<Map<String, Object>> balances = card.getBalances();

            // If no balances are available, return a response indicating no balances are available
            if (balances.isEmpty()) {
                logger.info("No balances available for card number: {}", cardNumber);
                return ResponseEntity.ok("No balances available for this credit card.");
            }

            // Sort the balances by date in ascending order
            balances.sort(Comparator.comparing(b -> LocalDate.parse(((Map<String, Object>) b).get("date").toString())));

            // Return the balances in the response
            logger.info("Balances retrieved for card number: {}", cardNumber);
            return ResponseEntity.ok(balances);
        } catch (Exception e) {
            // Log any exceptions that occur during the process
            logger.error("Error retrieving balances for card number: {}", cardNumber, e);
            // Return a 500 Internal Server Error response if an exception occurs
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while retrieving balances for card number: " + cardNumber);
        }
    }

}
