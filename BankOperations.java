package com.testlab.model;

import java.sql.*;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class BankOperations {

	private Connection connection;

	public BankOperations(Connection connection) {
		this.connection = connection;
	}

	// Generates a random 4-digit account number (1001 - 9999)
	public static int generateAccountnumber() {
		return (int) (Math.random() * (9999 - 1001 + 1)) + 1001;
	}

	// Create account with transaction PIN
	public String createAccount(Scanner scanner) {
		try {
			String name = "", surname = "";
			while (true) {
				System.out.print("Full Name (Name Surname): ");
				String input = scanner.nextLine().trim();
				String[] parts = input.split("\\s+");
				if (parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
					name = parts[0];
					surname = parts[1];
					break;
				}
				System.out.println("Please enter first and last name separated by space.");
			}

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

				if (!doesAccountExist(userId)) { // Uniqueness check
					break;
				} else {
					System.out.println("That userId is already taken. Please choose another.");
				}
			}

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

			// Transaction PIN input and validation
			String transactionPin;
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

			double balance;
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
				} catch (InputMismatchException ime) {
					System.out.println("Invalid input! Please enter a number.");
					scanner.nextLine(); // clear buffer
				}
			}

			int accountNumber = generateAccountnumber();

			String insertData = "INSERT INTO account (accountnumber, name, surname, balance, userid, password, transaction_pin) VALUES (?, ?, ?, ?, ?, ?, ?)";
			try (PreparedStatement ps = connection.prepareStatement(insertData)) {
				ps.setInt(1, accountNumber);
				ps.setString(2, name);
				ps.setString(3, surname);
				ps.setDouble(4, balance);
				ps.setString(5, userId);
				ps.setString(6, password);
				ps.setString(7, transactionPin);
				ps.executeUpdate();
			}

			System.out.println("New account created! Account No: " + String.format("%04d", accountNumber));
			System.out.println("UserId: " + userId);

			return userId;

		} catch (Exception e) {
			System.out.println("Account creation failed: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	// Verify PIN for User ID (3 attempts)
	public boolean verifyTransactionPinByUserId(String userId, Scanner scanner) throws SQLException {
		String sql = "SELECT transaction_pin FROM account WHERE userid = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					String storedPin = rs.getString("transaction_pin");
					for (int i = 0; i < 3; i++) {
						System.out.print("Enter your 4-digit Transaction PIN: ");
						String enteredPin = scanner.nextLine().trim();
						if (!enteredPin.matches("^\\d{4}$")) {
							System.out.println("PIN must be exactly 4 digits and numeric.");
							continue;
						}
						if (enteredPin.equals(storedPin))
							return true;
						else
							System.out.println("Incorrect PIN." + (i < 2 ? " Attempts left: " + (2 - i) : ""));
					}
					System.out.println("Transaction cancelled: Too many wrong attempts.");
				}
			}
		}
		return false;
	}

	// Verify PIN for Account Number (3 attempts)
	public boolean verifyTransactionPinByAccountNumber(int accountNumber, Scanner scanner) throws SQLException {
		String sql = "SELECT transaction_pin FROM account WHERE accountnumber = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, accountNumber);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					String storedPin = rs.getString("transaction_pin");
					for (int i = 0; i < 3; i++) {
						System.out.print("Enter your 4-digit Transaction PIN: ");
						String enteredPin = scanner.nextLine().trim();
						if (!enteredPin.matches("^\\d{4}$")) {
							System.out.println("PIN must be exactly 4 digits and numeric.");
							continue;
						}
						if (enteredPin.equals(storedPin))
							return true;
						else
							System.out.println("Incorrect PIN." + (i < 2 ? " Attempts left: " + (2 - i) : ""));
					}
					System.out.println("Transaction cancelled: Too many wrong attempts.");
				}
			}
		}
		return false;
	}

	// Check if an account exists by account number
	public boolean doesAccountExist(int accountnumber) {
		String sql = "SELECT 1 FROM account WHERE accountnumber = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, accountnumber);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException e) {
			return false;
		}
	}

	// Check if an account exists by userId
	public boolean doesAccountExist(String userid) {
		String sql = "SELECT 1 FROM account WHERE userid = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, userid);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException e) {
			return false;
		}
	}

	// Get balance by account number
	public double getAccountBalance(int accountnumber) {
		String sql = "SELECT balance FROM account WHERE accountnumber = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, accountnumber);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getDouble("balance");
			}
		} catch (SQLException e) {
			System.out.println("Error getting balance: " + e.getMessage());
		}
		return -1;
	}

	// Get balance by userId
	public double getAccountBalance(String userid) {
		String sql = "SELECT balance FROM account WHERE userid = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, userid);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getDouble("balance");
			}
		} catch (SQLException e) {
			System.out.println("Error getting balance: " + e.getMessage());
		}
		return -1;
	}

	// Deposit money by UserId, with PIN validation
	public void depositMoneyUsingUserID(Scanner scanner, String userId) throws SQLException {
		if (!verifyTransactionPinByUserId(userId, scanner))
			return;

		double amount;
		while (true) {
			System.out.print("Enter the amount to deposit: ");
			try {
				amount = Double.parseDouble(scanner.nextLine().trim());
				if (amount <= 0) {
					System.out.println("Deposit amount must be positive!");
					continue;
				}
				break;
			} catch (NumberFormatException e) {
				System.out.println("Invalid amount! Please enter a valid number.");
			}
		}

		String sql = "UPDATE account SET balance = balance + ? WHERE userid = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setDouble(1, amount);
			ps.setString(2, userId);
			int updated = ps.executeUpdate();
			if (updated == 1)
				System.out.println("Deposit successful! Amount deposited: " + amount + " ₹.");
			else
				System.out.println("Deposit failed. Please try again.");
		}
	}

	// Deposit money by Account Number, with PIN validation
	public void depositMoneyUsingAccountNumber(Scanner scanner, int accountNumber) throws SQLException {

		double amount;
		while (true) {
			System.out.print("Enter the amount to deposit: ");
			try {
				amount = Double.parseDouble(scanner.nextLine().trim());
				if (amount <= 0) {
					System.out.println("Deposit amount must be positive!");
					continue;
				}
				break;
			} catch (NumberFormatException e) {
				System.out.println("Invalid amount! Please enter a valid number.");
			}
		}
		if (!verifyTransactionPinByAccountNumber(accountNumber, scanner))
			return;
		String sql = "UPDATE account SET balance = balance + ? WHERE accountnumber = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setDouble(1, amount);
			ps.setInt(2, accountNumber);
			int updated = ps.executeUpdate();
			if (updated == 1)
				System.out.println("Deposit successful! Amount deposited: " + amount + " ₹.");
			else
				System.out.println("Deposit failed. Please try again.");
		}
	}

	// Withdraw money by UserId, with PIN validation
	public void withdrawMoneyUsingUserID(Scanner scanner, String userId) throws SQLException {
		if (!verifyTransactionPinByUserId(userId, scanner))
			return;

		double balance = getAccountBalance(userId);
		double amount;

		while (true) {
			System.out.print("Enter the amount to withdraw: ");
			try {
				amount = Double.parseDouble(scanner.nextLine().trim());
				if (amount <= 0) {
					System.out.println("Withdrawal amount must be positive!");
					continue;
				}
				if (balance < amount) {
					System.out.println("Insufficient funds! Current balance: " + balance + " ₹.");
					continue;
				}
				break;
			} catch (NumberFormatException e) {
				System.out.println("Invalid amount! Please enter a valid number.");
			}
		}

		String sql = "UPDATE account SET balance = balance - ? WHERE userid = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setDouble(1, amount);
			ps.setString(2, userId);
			int updated = ps.executeUpdate();
			if (updated == 1)
				System.out.println("Withdrawal successful! Amount withdrawn: " + amount + " ₹.");
			else
				System.out.println("Withdrawal failed. Please try again.");
		}
	}

	// Withdraw money by Account Number, with PIN validation
	public void withdrawMoneyUsingAccountNumber(Scanner scanner, int accountNumber) throws SQLException {

		double balance = getAccountBalance(accountNumber);
		double amount;

		while (true) {
			System.out.print("Enter the amount to withdraw: ");
			try {
				amount = Double.parseDouble(scanner.nextLine().trim());
				if (amount <= 0) {
					System.out.println("Withdrawal amount must be positive!");
					continue;
				}
				if (balance < amount) {
					System.out.println("Insufficient funds! Current balance: " + balance + " ₹.");
					continue;
				}
				break;
			} catch (NumberFormatException e) {
				System.out.println("Invalid amount! Please enter a valid number.");
			}
		}
		if (!verifyTransactionPinByAccountNumber(accountNumber, scanner))
			return;
		String sql = "UPDATE account SET balance = balance - ? WHERE accountnumber = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setDouble(1, amount);
			ps.setInt(2, accountNumber);
			int updated = ps.executeUpdate();
			if (updated == 1)
				System.out.println("Withdrawal successful! Amount withdrawn: " + amount + " ₹.");
			else
				System.out.println("Withdrawal failed. Please try again.");
		}
	}

	// Transfer money by UserId session with PIN
	public void transferMoneyUsingUserID(Scanner scanner, String senderUserId) throws SQLException {

		String receiverUserId;
		while (true) {
			System.out.print("Enter Receiver User ID (name.surname@xxxx): ");
			receiverUserId = scanner.nextLine().trim();
			String regex = "^[a-z]+\\.[a-z]+@\\d{4}$";
			if (!receiverUserId.matches(regex)) {
				System.out.println("Invalid format! Please enter as name.surname@xxxx.");
				continue;
			}
			if (receiverUserId.equals(senderUserId)) {
				System.out.println("Sender and Receiver IDs must be different!");
				continue;
			}
			if (!doesAccountExist(receiverUserId)) {
				System.out.println("Receiver ID not found!");
				continue;
			}
			break;
		}

		double senderBalance = getAccountBalance(senderUserId);
		double amount;

		while (true) {
			System.out.print("Enter amount to transfer: ");
			try {
				amount = Double.parseDouble(scanner.nextLine().trim());
				if (amount <= 0) {
					System.out.println("Amount must be positive!");
					continue;
				}
				if (senderBalance < amount) {
					System.out.println("Insufficient funds!");
					continue;
				}
				break;
			} catch (NumberFormatException e) {
				System.out.println("Invalid amount!");
			}
		}
		if (!verifyTransactionPinByUserId(senderUserId, scanner))
			return;
		connection.setAutoCommit(false);
		try {
			String deduct = "UPDATE account SET balance = balance - ? WHERE userid = ?";
			String add = "UPDATE account SET balance = balance + ? WHERE userid = ?";
			try (PreparedStatement ps1 = connection.prepareStatement(deduct);
					PreparedStatement ps2 = connection.prepareStatement(add)) {
				ps1.setDouble(1, amount);
				ps1.setString(2, senderUserId);
				int updated1 = ps1.executeUpdate();

				ps2.setDouble(1, amount);
				ps2.setString(2, receiverUserId);
				int updated2 = ps2.executeUpdate();

				if (updated1 == 1 && updated2 == 1) {
					connection.commit();
					System.out.println("Transfer successful! Amount sent: " + amount + " ₹.");
				} else {
					connection.rollback();
					System.out.println("Transfer failed! Try again.");
				}
			}
		} catch (Exception e) {
			connection.rollback();
			System.out.println("Transaction failed: " + e.getMessage());
		}
		connection.setAutoCommit(true);
	}

	// Transfer money by Account Number session with PIN
	public void transferMoneyUsingAccountNumber(Scanner scanner, int senderAccountNumber) throws SQLException {

		int receiverAccountNumber;
		while (true) {
			System.out.print("Enter Receiver Account Number: ");
			try {
				receiverAccountNumber = Integer.parseInt(scanner.nextLine().trim());
				if (receiverAccountNumber == senderAccountNumber) {
					System.out.println("Sender and Receiver accounts must be different!");
					continue;
				}
				if (!doesAccountExist(receiverAccountNumber)) {
					System.out.println("Account not found!");
					continue;
				}
				break;
			} catch (NumberFormatException e) {
				System.out.println("Invalid account number!");
			}
		}

		double senderBalance = getAccountBalance(senderAccountNumber);
		double amount;

		while (true) {
			System.out.print("Enter amount to transfer: ");
			try {
				amount = Double.parseDouble(scanner.nextLine().trim());
				if (amount <= 0) {
					System.out.println("Amount must be positive!");
					continue;
				}
				if (senderBalance < amount) {
					System.out.println("Insufficient funds!");
					continue;
				}
				break;
			} catch (NumberFormatException e) {
				System.out.println("Invalid amount!");
			}
		}
		if (!verifyTransactionPinByAccountNumber(senderAccountNumber, scanner))
			return;
		connection.setAutoCommit(false);
		try {
			String deduct = "UPDATE account SET balance = balance - ? WHERE accountnumber = ?";
			String add = "UPDATE account SET balance = balance + ? WHERE accountnumber = ?";
			try (PreparedStatement ps1 = connection.prepareStatement(deduct);
					PreparedStatement ps2 = connection.prepareStatement(add)) {
				ps1.setDouble(1, amount);
				ps1.setInt(2, senderAccountNumber);
				int updated1 = ps1.executeUpdate();

				ps2.setDouble(1, amount);
				ps2.setInt(2, receiverAccountNumber);
				int updated2 = ps2.executeUpdate();

				if (updated1 == 1 && updated2 == 1) {
					connection.commit();
					System.out.println("Transfer successful! Amount sent: " + amount + " ₹.");
				} else {
					connection.rollback();
					System.out.println("Transfer failed! Try again.");
				}
			}
		} catch (Exception e) {
			connection.rollback();
			System.out.println("Transaction failed: " + e.getMessage());
		}
		connection.setAutoCommit(true);
	}

	// Existing check balance on userId
	public void checkBalanceonUserid(String accuserid) throws SQLException {
		String balanceQuery = "SELECT balance FROM account WHERE userid = ?";
		try (PreparedStatement ps = connection.prepareStatement(balanceQuery)) {
			ps.setString(1, accuserid);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					double balance = rs.getDouble("balance");
					System.out.println("Current balance for user " + accuserid + " is: " + balance + " ₹.");
					System.out.println("-------------------------------------------------------------------------");
				} else {
					System.out.println("User Id " + accuserid + " not found.");
				}
			}
		}
	}

	// Displays the balance for given account number
	public void checkBalanceonAccountnumber(int accountnumber) throws SQLException {
		String balanceQuery = "SELECT balance FROM account WHERE accountnumber = ?";
		try (PreparedStatement ps = connection.prepareStatement(balanceQuery)) {
			ps.setInt(1, accountnumber);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					double balance = rs.getDouble("balance");
					System.out.println("Current balance for account " + String.format("%04d", accountnumber) + " is: "
							+ balance + " ₹.");
					System.out.println("-------------------------------------------------------------------------");
				} else {
					System.out.println("Account number " + accountnumber + " not found.");
				}
			}
		}
	}

	public void viewTransactionsForAccountNumber(Scanner scanner, int accountNumber) {
		while (true) {
			System.out.println("\nView Transactions Menu");
			System.out.println("1. View latest 5 transactions");
			System.out.println("2. View last 30 days transactions");
			System.out.println("3. Back");
			System.out.print("Input: ");
			String input = scanner.nextLine().trim();
			int choice;
			try {
				choice = Integer.parseInt(input);
			} catch (NumberFormatException e) {
				System.out.println("Please enter 1, 2 or 3.");
				continue;
			}

			if (choice == 1) {
				String sql = "SELECT datetime, type, amount, source_account, destination_account, description "
						+ "FROM transaction " + "WHERE source_account = ? OR destination_account = ? "
						+ "ORDER BY datetime DESC LIMIT 5";
				try (PreparedStatement ps = connection.prepareStatement(sql)) {
					ps.setInt(1, accountNumber);
					ps.setInt(2, accountNumber);
					try (ResultSet rs = ps.executeQuery()) {
						boolean found = false;
						System.out.println(
								"\nDATE & TIME         | TYPE     | AMOUNT   | FROM       | TO         | DESC");
						System.out.println(
								"-------------------------------------------------------------------------------");
						while (rs.next()) {
							found = true;
							String dt = rs.getTimestamp("datetime").toString();
							String type = rs.getString("type");
							double amount = rs.getDouble("amount");
							String src = rs.getString("source_account");
							String dst = rs.getString("destination_account");
							String desc = rs.getString("description");
							System.out.printf("%-19s | %-8s | %8.2f | %-10s | %-10s | %s\n", dt, type, amount, src, dst,
									desc);
						}
						if (!found) {
							System.out.println("No transactions found.");
						}
					}
				} catch (Exception e) {
					System.out.println("Error querying transactions: " + e.getMessage());
				}
			} else if (choice == 2) {
				String sql = "SELECT datetime, type, amount, source_account, destination_account, description "
						+ "FROM transaction " + "WHERE (source_account = ? OR destination_account = ?) "
						+ "AND datetime >= NOW() - INTERVAL 30 DAY " + "ORDER BY datetime DESC";
				try (PreparedStatement ps = connection.prepareStatement(sql)) {
					ps.setInt(1, accountNumber);
					ps.setInt(2, accountNumber);
					try (ResultSet rs = ps.executeQuery()) {
						boolean found = false;
						System.out.println(
								"\nDATE & TIME         | TYPE     | AMOUNT   | FROM       | TO         | DESC");
						System.out.println(
								"-------------------------------------------------------------------------------");
						while (rs.next()) {
							found = true;
							String dt = rs.getTimestamp("datetime").toString();
							String type = rs.getString("type");
							double amount = rs.getDouble("amount");
							String src = rs.getString("source_account");
							String dst = rs.getString("destination_account");
							String desc = rs.getString("description");
							System.out.printf("%-19s | %-8s | %8.2f | %-10s | %-10s | %s\n", dt, type, amount, src, dst,
									desc);
						}
						if (!found) {
							System.out.println("No transactions in last 30 days.");
						}
					}
				} catch (Exception e) {
					System.out.println("Error querying transactions: " + e.getMessage());
				}
			} else if (choice == 3) {
				return;
			} else {
				System.out.println("Invalid choice! Enter 1-3.");
			}
		}
	}

	// Display transaction history by user ID
	public void viewTransactionsForUserId(Scanner scanner, String userId) {
		while (true) {
			System.out.println("\nView Transactions Menu");
			System.out.println("1. View latest 5 transactions");
			System.out.println("2. View last 30 days transactions");
			System.out.println("3. Back");
			System.out.print("Input: ");
			String input = scanner.nextLine().trim();
			int choice;
			try {
				choice = Integer.parseInt(input);
			} catch (NumberFormatException e) {
				System.out.println("Please enter 1, 2 or 3.");
				continue;
			}

			if (choice == 1) {
				String sql = "SELECT datetime, type, amount, source_userid, destination_userid, description "
						+ "FROM transaction " + "WHERE source_userid = ? OR destination_userid = ? "
						+ "ORDER BY datetime DESC LIMIT 5";
				try (PreparedStatement ps = connection.prepareStatement(sql)) {
					ps.setString(1, userId);
					ps.setString(2, userId);
					try (ResultSet rs = ps.executeQuery()) {
						boolean found = false;
						System.out.println(
								"\nDATE & TIME         | TYPE     | AMOUNT   | FROM                | TO                  | DESC");
						System.out.println(
								"----------------------------------------------------------------------------------------------------");
						while (rs.next()) {
							found = true;
							String dt = rs.getTimestamp("datetime").toString();
							String type = rs.getString("type");
							double amount = rs.getDouble("amount");
							String src = rs.getString("source_userid");
							String dst = rs.getString("destination_userid");
							String desc = rs.getString("description");
							System.out.printf("%-19s | %-8s | %8.2f | %-19s | %-19s | %s\n", dt, type, amount, src, dst,
									desc);
						}
						if (!found) {
							System.out.println("No transactions found.");
						}
					}
				} catch (Exception e) {
					System.out.println("Error querying transactions: " + e.getMessage());
				}
			} else if (choice == 2) {
				String sql = "SELECT datetime, type, amount, source_userid, destination_userid, description "
						+ "FROM transaction " + "WHERE (source_userid = ? OR destination_userid = ?) "
						+ "AND datetime >= NOW() - INTERVAL 30 DAY " + "ORDER BY datetime DESC";
				try (PreparedStatement ps = connection.prepareStatement(sql)) {
					ps.setString(1, userId);
					ps.setString(2, userId);
					try (ResultSet rs = ps.executeQuery()) {
						boolean found = false;
						System.out.println(
								"\nDATE & TIME         | TYPE     | AMOUNT   | FROM                | TO                  | DESC");
						System.out.println(
								"----------------------------------------------------------------------------------------------------");
						while (rs.next()) {
							found = true;
							String dt = rs.getTimestamp("datetime").toString();
							String type = rs.getString("type");
							double amount = rs.getDouble("amount");
							String src = rs.getString("source_userid");
							String dst = rs.getString("destination_userid");
							String desc = rs.getString("description");
							System.out.printf("%-19s | %-8s | %8.2f | %-19s | %-19s | %s\n", dt, type, amount, src, dst,
									desc);
						}
						if (!found) {
							System.out.println("No transactions in last 30 days.");
						}
					}
				} catch (Exception e) {
					System.out.println("Error querying transactions: " + e.getMessage());
				}
			} else if (choice == 3) {
				return;
			} else {
				System.out.println("Invalid choice! Enter 1-3.");
			}
		}
	}

	// Find account number by user ID
	public void findAccountNumberByUserId(Scanner scanner) {
		String regex = "^[a-z]+\\.[a-z]+@\\d{4}$";
		while (true) {
			System.out.print("Enter User ID (name.surname@xxxx): ");
			String userId = scanner.nextLine().trim();
			if (!userId.matches(regex)) {
				System.out.println("Invalid format! Please enter as name.surname@xxxx.");
				continue;
			}
			String sql = "SELECT accountnumber FROM account WHERE userid = ?";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, userId);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						int accNum = rs.getInt("accountnumber");
						System.out.println(
								"Account number for user ID '" + userId + "' is: " + String.format("%04d", accNum));
					} else {
						System.out.println("No account found for this User ID.");
					}
					return;
				}
			} catch (Exception e) {
				System.out.println("Error: " + e.getMessage());
				return;
			}
		}
	}

	// Find user ID by account number
	public void findUserIdByAccountNumber(Scanner scanner) {
		while (true) {
			System.out.print("Enter the 4-digit account number: ");
			String input = scanner.nextLine().trim();
			int accountNumber;
			try {
				accountNumber = Integer.parseInt(input);
			} catch (NumberFormatException e) {
				System.out.println("Invalid account number! Please enter 4 digits.");
				continue;
			}
			String sql = "SELECT userid FROM account WHERE accountnumber = ?";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setInt(1, accountNumber);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						String userId = rs.getString("userid");
						System.out.println("User ID for account number '" + String.format("%04d", accountNumber)
								+ "' is: " + userId);
					} else {
						System.out.println("No user found for this account number.");
					}
					return;
				}
			} catch (Exception e) {
				System.out.println("Error: " + e.getMessage());
				return;
			}
		}
	}

	public void showProfileMenuByAccountNumber(Scanner scanner, int accountNumber) throws SQLException {
		while (true) {
			System.out.println("\n--- Profile Menu ---");
			System.out.println("1. View Account Details");
			System.out.println("2. Change PIN or Password");
			System.out.println("3. Edit name");
			System.out.println("4. Back");
			System.out.print("Select option: ");
			String opt = scanner.nextLine().trim();

			if (opt.equals("1")) {
				String sql = "SELECT accountnumber, name, surname, balance, userid, transaction_pin FROM account WHERE accountnumber = ?";
				try (PreparedStatement ps = connection.prepareStatement(sql)) {
					ps.setInt(1, accountNumber);
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							System.out.println("\n--- Account Details ---");
							System.out
									.println("Account Number   : " + String.format("%04d", rs.getInt("accountnumber")));
							System.out.println("User ID          : " + rs.getString("userid"));
							System.out.println(
									"Name             : " + rs.getString("name") + " " + rs.getString("surname"));
							System.out.println("Balance          : " + rs.getDouble("balance"));
						} else {
							System.out.println("Account info not found!");
						}
					}
				}
			} else if (opt.equals("2")) {
				changePinOrPasswordByAccountnumber(scanner, accountNumber);
			} else if (opt.equals("3")) {
				editNameByAccountNumber(scanner, accountNumber);

			}

			else if (opt.equals("4")) {
				return;
			} else {
				System.out.println("Invalid option. Please enter 1-3.");
			}
		}
	}

	public void showProfileMenuByUserId(Scanner scanner, String userId) throws SQLException {
		while (true) {
			System.out.println("\n--- Profile Menu ---");
			System.out.println("1. View Account Details");
			System.out.println("2. Change PIN or Password");
			System.out.println("3. Edit name ");
			System.out.println("4. Back");
			System.out.print("Select option: ");
			String opt = scanner.nextLine().trim();

			if (opt.equals("1")) {
				String sql = "SELECT accountnumber, name, surname, balance, userid, transaction_pin FROM account WHERE userid = ?";
				try (PreparedStatement ps = connection.prepareStatement(sql)) {
					ps.setString(1, userId);
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							System.out.println("\n--- Account Details ---");
							System.out
									.println("Account Number   : " + String.format("%04d", rs.getInt("accountnumber")));
							System.out.println("User ID          : " + rs.getString("userid"));
							System.out.println(
									"Name             : " + rs.getString("name") + " " + rs.getString("surname"));
							System.out.println("Balance          : " + rs.getDouble("balance"));
						} else {
							System.out.println("Account info not found!");
						}
					}
				}
			} else if (opt.equals("2")) {
				changePinOrPasswordByUserId(scanner, userId);
			} else if (opt.equals("3")) {
				editNameByUserId(scanner, userId);
			}

			else if (opt.equals("4")) {
				return;
			} else {
				System.out.println("Invalid option. Please enter 1-3.");
			}
		}
	}

	public void changePinOrPasswordByUserId(Scanner scanner, String userId) throws SQLException {
		while (true) {
			System.out.println("\nChange:");
			System.out.println("1. Password");
			System.out.println("2. Transaction PIN");
			System.out.println("3. Back");
			System.out.print("Choose: ");
			String ch = scanner.nextLine().trim();

			if (ch.equals("1")) {
				// Verify old password
				String sql = "SELECT password FROM account WHERE userid = ?";
				try (PreparedStatement ps = connection.prepareStatement(sql)) {
					ps.setString(1, userId);
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							String oldPass = rs.getString("password");
							System.out.print("Enter current password: ");
							String enteredOld = scanner.nextLine();
							if (!enteredOld.equals(oldPass)) {
								System.out.println("Incorrect password.");
								continue;
							}
						}
					}
				}
				// Set new password
				String newPassword;
				while (true) {
					System.out.print("Enter new password: ");
					newPassword = scanner.nextLine();
					if (!newPassword.matches("^[^.,]+$") || newPassword.length() < 6) {
						System.out.println("Password must be at least 6 chars, no '.' or ','.");
						continue;
					}
					System.out.print("Confirm password: ");
					String cpass = scanner.nextLine();
					if (!newPassword.equals(cpass)) {
						System.out.println("Passwords do not match.");
						continue;
					}
					break;
				}
				String updateSql = "UPDATE account SET password = ? WHERE userid = ?";
				try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
					ps.setString(1, newPassword);
					ps.setString(2, userId);
					ps.executeUpdate();
					System.out.println("Password changed successfully!");
				}
			} else if (ch.equals("2")) {
				// Verify old PIN
				String sql = "SELECT transaction_pin FROM account WHERE userid = ?";
				try (PreparedStatement ps = connection.prepareStatement(sql)) {
					ps.setString(1, userId);
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							String oldPin = rs.getString("transaction_pin");
							System.out.print("Enter current PIN: ");
							String enteredOld = scanner.nextLine().trim();
							if (!enteredOld.equals(oldPin)) {
								System.out.println("Incorrect PIN.");
								continue;
							}
						}
					}
				}
				// Set new PIN
				String newPin;
				while (true) {
					System.out.print("Enter new 4-digit PIN: ");
					newPin = scanner.nextLine().trim();
					if (!newPin.matches("^\\d{4}$")) {
						System.out.println("PIN must be exactly 4 numeric digits.");
						continue;
					}
					System.out.print("Confirm PIN: ");
					String cpin = scanner.nextLine().trim();
					if (!newPin.equals(cpin)) {
						System.out.println("PINs do not match.");
						continue;
					}
					break;
				}
				String updateSql = "UPDATE account SET transaction_pin = ? WHERE userid = ?";
				try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
					ps.setString(1, newPin);
					ps.setString(2, userId);
					ps.executeUpdate();
					System.out.println("PIN changed successfully!");
				}
			} else if (ch.equals("3")) {
				return;
			} else {
				System.out.println("Invalid choice. Enter 1-3.");
			}
		}
	}

	public void changePinOrPasswordByAccountnumber(Scanner scanner, int accountNumber) throws SQLException {
		while (true) {
			System.out.println("\nChange:");
			System.out.println("1. Password");
			System.out.println("2. Transaction PIN");
			System.out.println("3. Back");
			System.out.print("Choose: ");
			String ch = scanner.nextLine().trim();

			if (ch.equals("1")) {
				// Verify old password
				String sql = "SELECT password FROM account WHERE accountnumber = ?";
				try (PreparedStatement ps = connection.prepareStatement(sql)) {
					ps.setInt(1, accountNumber);
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							String oldPass = rs.getString("password");
							System.out.print("Enter current password: ");
							String enteredOld = scanner.nextLine();
							if (!enteredOld.equals(oldPass)) {
								System.out.println("Incorrect password.");
								continue;
							}
						} else {
							System.out.println("Account not found.");
							return;
						}
					}
				}
				// Set new password
				String newPassword;
				while (true) {
					System.out.print("Enter new password: ");
					newPassword = scanner.nextLine();
					if (!newPassword.matches("^[^.,]+$") || newPassword.length() < 6) {
						System.out.println("Password must be at least 6 characters and cannot contain '.' or ','.");
						continue;
					}
					System.out.print("Confirm password: ");
					String cpass = scanner.nextLine();
					if (!newPassword.equals(cpass)) {
						System.out.println("Passwords do not match.");
						continue;
					}
					break;
				}
				String updateSql = "UPDATE account SET password = ? WHERE accountnumber = ?";
				try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
					ps.setString(1, newPassword);
					ps.setInt(2, accountNumber);
					int updated = ps.executeUpdate();
					if (updated == 1) {
						System.out.println("Password changed successfully!");
					} else {
						System.out.println("Failed to change password. Please try again.");
					}
				}
			} else if (ch.equals("2")) {
				// Verify old PIN
				String sql = "SELECT transaction_pin FROM account WHERE accountnumber = ?";
				try (PreparedStatement ps = connection.prepareStatement(sql)) {
					ps.setInt(1, accountNumber);
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							String oldPin = rs.getString("transaction_pin");
							System.out.print("Enter current PIN: ");
							String enteredOld = scanner.nextLine().trim();
							if (!enteredOld.equals(oldPin)) {
								System.out.println("Incorrect PIN.");
								continue; // return to change menu
							}
						} else {
							System.out.println("Account not found.");
							return; // exit method
						}
					}
				}
				// Set new PIN
				String newPin;
				while (true) {
					System.out.print("Enter new 4-digit PIN: ");
					newPin = scanner.nextLine().trim();
					if (!newPin.matches("^\\d{4}$")) {
						System.out.println("PIN must be exactly 4 numeric digits.");
						continue;
					}
					System.out.print("Confirm PIN: ");
					String cpin = scanner.nextLine().trim();
					if (!newPin.equals(cpin)) {
						System.out.println("PINs do not match.");
						continue;
					}
					break;
				}
				String updateSql = "UPDATE account SET transaction_pin = ? WHERE accountnumber = ?";
				try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
					ps.setString(1, newPin);
					ps.setInt(2, accountNumber);
					int updated = ps.executeUpdate();
					if (updated == 1) {
						System.out.println("PIN changed successfully!");
					} else {
						System.out.println("Failed to change PIN. Please try again.");
					}
				}
			} else if (ch.equals("3")) {
				return;
			} else {
				System.out.println("Invalid choice. Enter 1-3.");
			}
		}
	}

	public void editNameByUserId(Scanner scanner, String userId) throws SQLException {
		// Ask password to validate
		System.out.print("Enter your current password to proceed: ");
		String passwordInput = scanner.nextLine();
		String sqlCheck = "SELECT password FROM account WHERE userid=?";
		try (PreparedStatement ps = connection.prepareStatement(sqlCheck)) {
			ps.setString(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next() || !rs.getString("password").equals(passwordInput)) {
					System.out.println("Incorrect password. Cannot update name.");
					return;
				}
			}
		}
		// Now allow update
		String newFirst = "", newMiddle = "", newLast = "";
		while (true) {
			System.out.print("Enter new First Name: ");
			newFirst = scanner.nextLine().trim();
			if (!newFirst.matches("[A-Za-z]+")) {
				System.out.println("Invalid! Only letters allowed.");
				continue;
			}
			System.out.print("Enter new Middle Name (leave blank if none): ");
			newMiddle = scanner.nextLine().trim();
			if (!newMiddle.isEmpty() && !newMiddle.matches("[A-Za-z]+")) {
				System.out.println("Invalid! Only letters allowed.");
				continue;
			}
			System.out.print("Enter new Last Name: ");
			newLast = scanner.nextLine().trim();
			if (!newLast.matches("[A-Za-z]+")) {
				System.out.println("Invalid! Only letters allowed.");
				continue;
			}
			break;
		}
		String sqlUpdate = "UPDATE account SET name=?, middle_name=?, surname=? WHERE userid=?";
		try (PreparedStatement ps = connection.prepareStatement(sqlUpdate)) {
			ps.setString(1, newFirst);
			ps.setString(2, newMiddle);
			ps.setString(3, newLast);
			ps.setString(4, userId);
			ps.executeUpdate();
			System.out.println("Name updated successfully!");
		}
	}

	public void editNameByAccountNumber(Scanner scanner, int accountNumber) throws SQLException {
		// Ask password to validate
		System.out.print("Enter your current password to proceed: ");
		String passwordInput = scanner.nextLine();
		String sqlCheck = "SELECT password FROM account WHERE accountnumber=?";
		try (PreparedStatement ps = connection.prepareStatement(sqlCheck)) {
			ps.setInt(1, accountNumber);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next() || !rs.getString("password").equals(passwordInput)) {
					System.out.println("Incorrect password. Cannot update name.");
					return;
				}
			}
		}
		// Now allow update
		String newFirst = "", newMiddle = "", newLast = "";
		while (true) {
			System.out.print("Enter new First Name: ");
			newFirst = scanner.nextLine().trim();
			if (!newFirst.matches("[A-Za-z]+")) {
				System.out.println("Invalid! Only letters allowed.");
				continue;
			}
			System.out.print("Enter new Middle Name (leave blank if none): ");
			newMiddle = scanner.nextLine().trim();
			if (!newMiddle.isEmpty() && !newMiddle.matches("[A-Za-z]+")) {
				System.out.println("Invalid! Only letters allowed.");
				continue;
			}
			System.out.print("Enter new Last Name: ");
			newLast = scanner.nextLine().trim();
			if (!newLast.matches("[A-Za-z]+")) {
				System.out.println("Invalid! Only letters allowed.");
				continue;
			}
			break;
		}
		String sqlUpdate = "UPDATE account SET name=?, middle_name=?, surname=? WHERE accountnumber=?";
		try (PreparedStatement ps = connection.prepareStatement(sqlUpdate)) {
			ps.setString(1, newFirst);
			ps.setString(2, newMiddle);
			ps.setString(3, newLast);
			ps.setInt(4, accountNumber);
			ps.executeUpdate();
			System.out.println("Name updated successfully!");
		}
	}

}
