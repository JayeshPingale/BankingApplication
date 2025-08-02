package com.testlab.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Authentication {

    private Connection connection;

    public Authentication(Connection connection) {
        this.connection = connection;
    }

    public String loginByUserId(Scanner scanner) {
        String regex = "^[a-z]+\\.[a-z]+@\\d{4}$";
        String userId = "";

        while (true) {
            System.out.print("Enter User ID: ");
            userId = scanner.nextLine().trim();
            if (!userId.matches(regex)) {
                System.out.println("Please enter User ID in correct format: name.surname@xxxx (e.g., john.doe@1234)");
                continue;
            }
            // Check DB existence
            String query = "SELECT password FROM account WHERE userid = ?";
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String dbPass = rs.getString("password");
                    // 3 tries logic for password
                    for (int i = 0; i < 3; ++i) {
                        System.out.print("Enter Password: ");
                        String password = scanner.nextLine();
                        if (password.equals(dbPass)) {
                            System.out.println("Login successful! Welcome, " + userId);
                            return userId;
                        } else if (i < 2) {
                            System.out.println("Invalid password. Attempts left: " + (2-i));
                        }
                    }
                    System.out.println("Login failed: 3 incorrect password attempts.");
                    return null;
                } else {
                    System.out.println("User ID not found.");
                    return null;
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
                return null;
            }
        }
    }

    public Integer loginByAccountNumber(Scanner scanner) {
        int accountNumber = -1;
        while (true) {
            System.out.print("Enter 4-digit Account Number: ");
            String input = scanner.nextLine().trim();
            try {
                accountNumber = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid format! Please enter a 4-digit number.");
                continue;
            }
            // Check existence and get password
            String query = "SELECT password FROM account WHERE accountnumber = ?";
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setInt(1, accountNumber);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String dbPass = rs.getString("password");
                    // 3 tries logic for password
                    for (int i = 0; i < 3; ++i) {
                        System.out.print("Enter Password: ");
                        String password = scanner.nextLine();
                        if (password.equals(dbPass)) {
                            System.out.println("Login successful! Welcome, Account " + String.format("%04d", accountNumber));
                            return accountNumber;
                        } else if (i < 2) {
                            System.out.println("Invalid password. Attempts left: " + (2-i));
                        }
                    }
                    System.out.println("Login failed: 3 incorrect password attempts.");
                    return null;
                } else {
                    System.out.println("Account number not found.");
                    return null;
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
                return null;
            }
        }
        
        
    }
  
}

