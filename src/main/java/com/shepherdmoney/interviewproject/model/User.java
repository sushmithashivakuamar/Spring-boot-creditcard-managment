package com.shepherdmoney.interviewproject.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Table(name = "MyUser")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String name;

    private String email;

    // TODO: User's credit card
    // HINT: A user can have one or more, or none at all. We want to be able to query credit cards by user
    //       and user by a credit card.

    // One-to-Many relationship with CreditCard
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CreditCard> creditCards = new HashSet<>();//set contains all the credit cards associated with this user.

    // Adds a credit card to this user. If the creditCards set is null, a new HashSet is instantiated.
    public void addCreditCard(CreditCard creditCard) {
        if (creditCards == null) {
            creditCards = new HashSet<>();
        }
        creditCards.add(creditCard);
        creditCard.setOwner(this);
    }

    // Removes a credit card from this user. Checks if the creditCards set is not null and contains the credit card.
    // It also sets the owner of the credit card to null, breaking the bidirectional relationship.
    public void removeCreditCard(CreditCard creditCard) {
        if (creditCards != null && creditCards.contains(creditCard)) {
            creditCards.remove(creditCard);
            creditCard.setOwner(null);
        }
    }
}
