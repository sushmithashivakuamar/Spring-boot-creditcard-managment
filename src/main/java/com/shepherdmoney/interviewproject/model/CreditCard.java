package com.shepherdmoney.interviewproject.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(name = "issuance_bank")
    private String issuanceBank;

    @Column(name = "number")
    private String number;

    // TODO: Credit card's owner. For detailed hint, please see User class
    // Some field here <> owner;

    // ManyToOne relationship to User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User owner;

    //Credit card's balance history
    @OneToMany(mappedBy = "creditCard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BalanceHistory> balanceHistory = new ArrayList<>();


    // TODO: Credit card's balance history. It is a requirement that the dates in the balanceHistory 
    //       list must be in chronological order, with the most recent date appearing first in the list. 
    //       Additionally, the last object in the "list" must have a date value that matches today's date, 
    //       since it represents the current balance of the credit card.
    //       This means that if today is 04-16, and the list begin as empty, you receive a payload for 04-13,
    //       you should fill the list up until 04-16. For example:
    //       [
    //         {date: '2023-04-10', balance: 800},
    //         {date: '2023-04-11', balance: 1000},
    //         {date: '2023-04-12', balance: 1200},
    //         {date: '2023-04-13', balance: 1100},
    //         {date: '2023-04-16', balance: 900},
    //       ]
    // ADDITIONAL NOTE: For the balance history, you can use any data structure that you think is appropriate.
    //        It can be a list, array, map, pq, anything. However, there are some suggestions:
    //        1. Retrieval of a balance of a single day should be fast
    //        2. Traversal of the entire balance history should be fast
    //        3. Insertion of a new balance should be fast
    //        4. Deletion of a balance should be fast
    //        5. It is possible that there are gaps in between dates (note the 04-13 and 04-16)
    //        6. In the condition that there are gaps, retrieval of "closest **previous**" balance date should also be fast. Aka, given 4-15, return 4-13 entry tuple


    // Updates or adds a new balance on a given date, ensuring gaps are filled.
// Updates or adds a new balance on a given date, ensuring gaps are filled.

    public void updateBalanceHistory(LocalDate date, double balance) {
        // Find if there's an existing balance entry for the given date
        BalanceHistory existingBalance = findBalanceByDate(date);

        if (existingBalance != null) {
            // If an entry exists for the date, update its balance
            existingBalance.setBalance(balance);
        } else {
            // If no entry exists, create a new balance history entry and add it to the list
            BalanceHistory newBalance = new BalanceHistory();
            newBalance.setDate(date);
            newBalance.setBalance(balance);
            newBalance.setCreditCard(this);
            balanceHistory.add(newBalance);
        }
        // Ensure the balance history list is in chronological order
        ensureOrder();
    }

    private BalanceHistory findBalanceByDate(LocalDate date) {
        // Find and return the balance history entry corresponding to the given date
        return balanceHistory.stream()
                .filter(b -> b.getDate().equals(date))
                .findFirst()
                .orElse(null);
    }

    public void fillGapsAndEnsureToday() {
        LocalDate today = LocalDate.now();
        if (balanceHistory.isEmpty()) {
            // If balance history is empty, update it with today's date and zero balance
            updateBalanceHistory(today, 0.0);
        } else {
            ensureOrder(); //the list is sorted before filling gaps
            LocalDate lastDate = balanceHistory.get(balanceHistory.size() - 1).getDate();

            // Fill gaps between the last recorded date and today's date
            for (LocalDate date = lastDate.plusDays(1); !date.isAfter(today); date = date.plusDays(1)) {
                if (!hasDate(date)) {
                    // If there's no entry for a date, update it with the last recorded balance
                    Double lastBalance = getBalanceOnOrBeforeDate(date.minusDays(1));
                    updateBalanceHistory(date, lastBalance);
                }
            }
            ensureOrder(); // Ensuring the list is sorted after filling gaps
        }
    }

    private boolean hasDate(LocalDate date) {
        // Check if any balance history entry has the given date
        return balanceHistory.stream().anyMatch(b -> b.getDate().equals(date));
    }

    private void ensureOrder() {
        // Ensure that balance history entries are sorted by date in ascending order
        balanceHistory.sort(Comparator.comparing(BalanceHistory::getDate));
    }

    public Double getBalanceOnOrBeforeDate(LocalDate date) {
        // Filter balance history entries that are on or before the given date
        return balanceHistory.stream()
                .filter(b -> !b.getDate().isAfter(date))
                .max(Comparator.comparing(BalanceHistory::getDate))// Find the max date
                .map(BalanceHistory::getBalance)// Map to the balance of the max date
                .orElse(0.0);// If no balance is found, return 0.0
    }

    public List<Map<String, Object>> getBalances() {
        // Map each balance entry to a map containing date and balance
        return balanceHistory.stream()
                .map(b -> {
                    Map<String, Object> balanceData = new HashMap<>();
                    balanceData.put("date", b.getDate());
                    balanceData.put("balance", b.getBalance());
                    return balanceData;
                })
                .collect(Collectors.toList()); // Collect the mapped data into a list
    }

}
