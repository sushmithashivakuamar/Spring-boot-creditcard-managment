package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    // TODO: wire in the user repository (~ 1 line)
    @Autowired
    private UserRepository userRepository;  // Dependency injection of the UserRepository
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @PutMapping("/user")
    public ResponseEntity<Integer> createUser(@RequestBody CreateUserPayload payload) {
        // TODO: Create an user entity with information given in the payload, store it in the database
        //       and return the id of the user in 200 OK response
        // Validate the payload: name and email must not be null or empty
        if (payload.getName() == null || payload.getName().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        if (payload.getEmail() == null || payload.getEmail().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        try {
            // Create a new User instance and set its properties from the payload
            User newUser = new User();
            newUser.setName(payload.getName());
            newUser.setEmail(payload.getEmail());

            newUser = userRepository.save(newUser); // Saving the user to the database
            logger.info("User created successfully with ID: {}", newUser.getId());
            return ResponseEntity.ok(newUser.getId()); // Return the user's ID with a 200 OK response
        } catch (Exception e) {
            logger.error("Error creating user", e);
            return ResponseEntity.badRequest().body(null); // Return a 400 Bad Request response if an exception occurs
        }
    }

    @Transactional
    @DeleteMapping("/user")
    public ResponseEntity<String> deleteUser(@RequestParam Integer userId) {
        // TODO: Return 200 OK if a user with the given ID exists, and the deletion is successful
        //       Return 400 Bad Request if a user with the ID does not exist
        //       The response body could be anything you consider appropriate

        try {
            // Attempt to find the user by their ID.
            return userRepository.findById(userId)
                    .map(user -> {
                        userRepository.delete(user); // Deletes the user if found.
                        logger.info("User deleted successfully with ID: {}", userId); // Log success.
                        // Return a 200 OK response indicating successful deletion, including the user ID in the message.
                        return ResponseEntity.ok("User ID " + userId + " deleted successfully.");
                    })
                    .orElseGet(() -> {
                        logger.warn("User not found with ID: {}", userId); // Log the absence of the user.
                        // Return a 400 Bad Request response indicating the user was not found, including the user ID in the message.
                        return ResponseEntity.badRequest().body("User not found with ID: " + userId);
                    });
        } catch (Exception e) {
            logger.error("Error deleting user with ID: {}", userId, e); // Log unexpected errors.
            // Return a 500 Internal Server Error response in case of unexpected exceptions.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error occurred while deleting user.");
        }
    }
}
