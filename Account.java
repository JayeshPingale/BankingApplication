package com.testlab.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Account {

	private Connection connection;

	public Account(Connection connection) {
		this.connection = connection;
	}

	public static int generateAccountNumber() {
		return (int) (Math.random() * (9999 - 1001 + 1)) + 1001;
	}

	public String createAccount(Scanner scanner) {
		try {
			String name = "", middleName = "", surname = "";
			while (true) {
			    System.out.print("Enter First Name (as per Aadhaar): ");
			    name = scanner.nextLine().trim();
			    if (!name.matches("[A-Za-z]+")) {
			        System.out.println("Invalid! Only letters allowed.");
			        continue;
			    }
			    System.out.print("Enter Middle Name (as per Aadhaar, leave blank if none): ");
			    middleName = scanner.nextLine().trim();
			    if (!middleName.isEmpty() && !middleName.matches("[A-Za-z]+")) {
			        System.out.println("Invalid! Only letters allowed.");
			        continue;
			    }
			    System.out.print("Enter Last Name (as per Aadhaar): ");
			    surname = scanner.nextLine().trim();
			    if (!surname.matches("[A-Za-z]+")) {
			        System.out.println("Invalid! Only letters allowed.");
			        continue;
			    }
			    break;
			}

			// UserID input & uniqueness
			String userId;
			while (true) {
				System.out.print(
						"Enter userId in format " + name.toLowerCase() + "." + surname.toLowerCase() + "@XXXX : ");
				userId = scanner.nextLine().trim();
				String regex = Pattern.quote(name.toLowerCase() + "." + surname.toLowerCase()) + "@\\d{4}$";
				if (!userId.matches(regex)) {
					System.out.println("Invalid format! Please enter as: " + name.toLowerCase() + "."
							+ surname.toLowerCase() + "@XXXX");
					continue;
				}

				if (!isUserIdUnique(userId)) {
					System.out.println("That userId is already taken. Please choose another.");
					continue;
				}

				break;
			}

			// Password input with validation and confirmation
			String password;
			while (true) {
				System.out.print("Enter password: ");
				password = scanner.nextLine();

				if (!password.matches("^[^.,]+$")) {
					System.out.println("Password cannot contain '.' or ',' characters. Please try again.");
					continue;
				}

				System.out.print("Confirm password: ");
				String confirmPassword = scanner.nextLine();

				if (!password.equals(confirmPassword)) {
					System.out.println("Passwords do not match. Please try again.");
					continue;
				}

				if (password.length() < 6) {
					System.out.println("Password should be at least 6 characters long.");
					continue;
				}

				break;
			}
			String transactionPin = "";
			while (true) {
			    System.out.print("Set a 4-digit numeric Transaction PIN: ");
			    transactionPin = scanner.nextLine().trim();
			    if (!transactionPin.matches("^\\d{4}$")) {
			        System.out.println("Invalid PIN! PIN must be exactly 4 numeric digits.");
			        continue;
			    }
			    System.out.print("Confirm Transaction PIN: ");
			    String confirmPin = scanner.nextLine().trim();
			    if (!transactionPin.equals(confirmPin)) {
			        System.out.println("PINs do not match. Try again.");
			        continue;
			    }
			    break;
			}


			// Balance input
			double balance = 0;
			while (true) {
				System.out.print("Balance: ");
				try {
					balance = scanner.nextDouble();
					scanner.nextLine(); // consume newline
					if (balance < 0) {
						System.out.println("Balance cannot be negative.");
						continue;
					}
					break;
				} catch (Exception e) {
					System.out.println("Invalid input! Please enter a number.");
					scanner.nextLine(); // clear buffer
				}
			}

			int accountNumber = generateAccountNumber();

			// Insert into DB
			String insertData = "INSERT INTO account (accountnumber, name, middle_name, surname, balance, userid, password, transaction_pin) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

					try (PreparedStatement ps = connection.prepareStatement(insertData)) {
					    ps.setInt(1, accountNumber);
					    ps.setString(2, name);
					    ps.setString(3, middleName);
					    ps.setString(4, surname);
					    ps.setDouble(5, balance);
					    ps.setString(6, userId);
					    ps.setString(7, password);
					    ps.setString(8, transactionPin);  // <-- THIS LINE ADDED
					    ps.executeUpdate();
					}
					System.out.println("New account created! Account No: " + String.format("%04d", accountNumber));
					System.out.println("UserId: " + userId);
					// Log opening balance as a deposit transaction
					if (balance > 0) {
					    String depositTransaction = "INSERT INTO transaction (type, amount, destination_account, destination_userid, description) VALUES (?, ?, ?, ?, ?)";
					    try (PreparedStatement tx = connection.prepareStatement(depositTransaction)) {
					        tx.setString(1, "deposit");
					        tx.setDouble(2, balance);
					        tx.setInt(3, accountNumber);
					        tx.setString(4, userId);
					        tx.setString(5, "Initial deposit during account creation");
					        tx.executeUpdate();
					    }
					}
			return userId;

		} catch (Exception e) {
			System.out.println("Account creation failed: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	private boolean isUserIdUnique(String userId) {
		String checkSql = "SELECT COUNT(*) FROM account WHERE userid=?";
		try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
			ps.setString(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getInt(1) == 0;
			}
		} catch (SQLException e) {
			System.out.println("Error checking userid uniqueness: " + e.getMessage());
			return false;
		}
	}

	public boolean doesAccountExist(int accountNumber) {
		String sql = "SELECT 1 FROM account WHERE accountnumber = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, accountNumber);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		} catch (Exception e) {
			return false;
		}
	}

	public boolean doesAccountExist(String userId) {
		String sql = "SELECT 1 FROM account WHERE userid = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		} catch (Exception e) {
			return false;
		}
	}

	public double getAccountBalance(int accountNumber) {
		String sql = "SELECT balance FROM account WHERE accountnumber = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, accountNumber);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getDouble("balance");
				}
			}
		} catch (Exception e) {
			System.out.println("Error fetching balance: " + e.getMessage());
		}
		return -1;
	}

	public double getAccountBalance(String userId) {
		String sql = "SELECT balance FROM account WHERE userid = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getDouble("balance");
				}
			}
		} catch (Exception e) {
			System.out.println("Error fetching balance: " + e.getMessage());
		}
		return -1;
	}
}
