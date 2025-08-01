package com.testlab.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

import com.testlab.model.Account;
import com.testlab.model.Authentication;
import com.testlab.model.BankOperations;

public class BankApp {
	private static void showUserIdSessionMenu(Connection connection, Scanner scanner, String userId) throws Exception {
		BankOperations bankOps = new BankOperations(connection);
		boolean active = true;
		while (active) {
			System.out.println("\n--- LENA Bank - UserID Session Menu ---");
			System.out.println("Logged in as: " + userId);
			System.out.println("1. Check Balance");
			System.out.println("2. Transfer Money");
			System.out.println("3. Deposit Money");
			System.out.println("4. Withdraw Money");
			System.out.println("5. View Transaction Statement");
			System.out.println("6. View Profile");
			System.out.println("7. Logout");
			System.out.print("Select option: ");

			String optStr = scanner.nextLine().trim();
			int choice;
			try {
				choice = Integer.parseInt(optStr);
			} catch (Exception e) {
				System.out.println("Invalid option.");
				continue;
			}

			switch (choice) {
			case 1:
				bankOps.checkBalanceonUserid(userId);
				break;
			case 2:
				bankOps.transferMoneyUsingUserID(scanner, userId);
				break;
			case 3:
				bankOps.depositMoneyUsingUserID(scanner, userId);
				break;
			case 4:
				bankOps.withdrawMoneyUsingUserID(scanner, userId);
				break;
			case 5:
				bankOps.viewTransactionsForUserId(scanner, userId);
				break;
			case 6:
				bankOps.showProfileMenuByUserId(scanner, userId);
				break;
			case 7:
				System.out.println("Logging out...");
				active = false;
				break;
			default:
				System.out.println("Invalid option. Please enter 1-7.");
			}
		}
	}

	// SESSION MENU—ACCOUNTNUMBER
	private static void showAccountNumberSessionMenu(Connection connection, Scanner scanner, int accountNumber)
			throws Exception {
		BankOperations bankOps = new BankOperations(connection);
		boolean active = true;
		while (active) {
			System.out.println("\n--- LENA Bank - AccountNumber Session Menu ---");
			System.out.println("Logged in as: " + String.format("%04d", accountNumber));
			System.out.println("1. Check Balance");
			System.out.println("2. Transfer Money");
			System.out.println("3. Deposit Money");
			System.out.println("4. Withdraw Money");
			System.out.println("5. View Transaction Statement");
			System.out.println("6. View Profile");
			System.out.println("7. Logout");
			System.out.print("Select option: ");

			String optStr = scanner.nextLine().trim();
			int choice;
			try {
				choice = Integer.parseInt(optStr);
			} catch (Exception e) {
				System.out.println("Invalid option.");
				continue;
			}

			switch (choice) {
			case 1:
				bankOps.checkBalanceonAccountnumber(accountNumber);
				break;
			case 2:
				bankOps.transferMoneyUsingAccountNumber(scanner, accountNumber);
				break;
			case 3:
				bankOps.depositMoneyUsingAccountNumber(scanner, accountNumber);
				break;
			case 4:
				bankOps.withdrawMoneyUsingAccountNumber(scanner, accountNumber);
				break;
			case 5:
				bankOps.viewTransactionsForAccountNumber(scanner, accountNumber);
				break;
			case 6:
				bankOps.showProfileMenuByAccountNumber(scanner, accountNumber);
				break;
			case 7:
				System.out.println("Logging out...");
				active = false;
				break;
			default:
				System.out.println("Invalid option. Please enter 1-7.");
			}
		}
	}

	private static void forgotPasswordMenu(Scanner scanner, Connection connection) {
	    System.out.println("Forgot Password:");
	    System.out.println("1. By User ID");
	    System.out.println("2. By Account Number");
	    System.out.println("3. Back");
	    System.out.print("Select option: ");
	    String choiceStr = scanner.nextLine().trim();
	    int choice;
	    try {
	        choice = Integer.parseInt(choiceStr);
	    } catch (NumberFormatException e) {
	        System.out.println("Invalid input!");
	        return;
	    }
	    if (choice == 1) {
	        // By User ID, keep asking until valid or return
	        String regex = "^[a-z]+\\.[a-z]+@\\d{4}$";
	        String userId;
	        while (true) {
	            System.out.print("Enter User ID: ");
	            userId = scanner.nextLine().trim();
	            if (!userId.matches(regex)) {
	                System.out.println("Please enter User ID in correct format: name.surname@xxxx (e.g., john.doe@1234)");
	                System.out.println("Enter 0 to go back to previous menu or any other key to try again.");
	                String again = scanner.nextLine();
	                if (again.equals("0")) return;
	                continue;
	            }
	            break;
	        }

	        System.out.print("Enter your First Name: ");
	        String firstName = scanner.nextLine().trim();
	        System.out.print("Enter your Last Name: ");
	        String lastName = scanner.nextLine().trim();

	        String sql = "SELECT name, surname FROM account WHERE userid = ?";
	        try (PreparedStatement ps = connection.prepareStatement(sql)) {
	            ps.setString(1, userId);
	            try (ResultSet rs = ps.executeQuery()) {
	                if (rs.next()) {
	                    String dbFirst = rs.getString("name");
	                    String dbLast = rs.getString("surname");
	                    if (dbFirst.equalsIgnoreCase(firstName) && dbLast.equalsIgnoreCase(lastName)) {
	                        String newPass = null;
	                        while (true) {
	                            System.out.print("Enter new password: ");
	                            newPass = scanner.nextLine();
	                            if (!newPass.matches("^[^.,]+$") || newPass.length() < 6) {
	                                System.out.println("Password must be at least 6 characters, no '.' or ','");
	                                continue;
	                            }
	                            System.out.print("Confirm new password: ");
	                            String cpass = scanner.nextLine();
	                            if (!newPass.equals(cpass)) {
	                                System.out.println("Passwords do not match.");
	                                continue;
	                            }
	                            break;
	                        }
	                        String updateSql = "UPDATE account SET password=? WHERE userid=?";
	                        try (PreparedStatement ups = connection.prepareStatement(updateSql)) {
	                            ups.setString(1, newPass);
	                            ups.setString(2, userId);
	                            ups.executeUpdate();
	                        }
	                        System.out.println("Password has been reset! You can now login with your new password.");
	                    } else {
	                        System.out.println("Verification failed. Cannot reset password.");
	                    }
	                } else {
	                    System.out.println("User ID not found.");
	                }
	            }
	        } catch (SQLException e) {
	            System.out.println("Database error: " + e.getMessage());
	        }
	    } else if (choice == 2) {
	        // By Account Number (original logic unchanged)
	        System.out.print("Enter your Account Number: ");
	        int accNum;
	        try {
	            accNum = Integer.parseInt(scanner.nextLine().trim());
	        } catch (NumberFormatException e) {
	            System.out.println("Invalid account number!");
	            return;
	        }
	        System.out.print("Enter your First Name: ");
	        String firstName = scanner.nextLine().trim();
	        System.out.print("Enter your Last Name: ");
	        String lastName = scanner.nextLine().trim();
	        String sql = "SELECT name, surname FROM account WHERE accountnumber = ?";
	        try (PreparedStatement ps = connection.prepareStatement(sql)) {
	            ps.setInt(1, accNum);
	            try (ResultSet rs = ps.executeQuery()) {
	                if (rs.next()) {
	                    String dbFirst = rs.getString("name");
	                    String dbLast = rs.getString("surname");
	                    if (dbFirst.equalsIgnoreCase(firstName) && dbLast.equalsIgnoreCase(lastName)) {
	                        String newPass = null;
	                        while (true) {
	                            System.out.print("Enter new password: ");
	                            newPass = scanner.nextLine();
	                            if (!newPass.matches("^[^.,]+$") || newPass.length() < 6) {
	                                System.out.println("Password must be at least 6 characters, no '.' or ','");
	                                continue;
	                            }
	                            System.out.print("Confirm new password: ");
	                            String cpass = scanner.nextLine();
	                            if (!newPass.equals(cpass)) {
	                                System.out.println("Passwords do not match.");
	                                continue;
	                            }
	                            break;
	                        }
	                        String updateSql = "UPDATE account SET password=? WHERE accountnumber=?";
	                        try (PreparedStatement ups = connection.prepareStatement(updateSql)) {
	                            ups.setString(1, newPass);
	                            ups.setInt(2, accNum);
	                            ups.executeUpdate();
	                        }
	                        System.out.println("Password has been reset! You can now login with your new password.");
	                    } else {
	                        System.out.println("Verification failed. Cannot reset password.");
	                    }
	                } else {
	                    System.out.println("Account number not found.");
	                }
	            }
	        } catch (SQLException e) {
	            System.out.println("Database error: " + e.getMessage());
	        }
	    } else if (choice == 3) {
	        return;
	    } else {
	        System.out.println("Invalid option!");
	    }
	}


	public static void main(String[] args) {
		String link = "jdbc:mysql://localhost:3306/bank";
		String dbname = "root";
		String dbpass = "Jayesh@2711";

		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			Connection connection = DriverManager.getConnection(link, dbname, dbpass);
			Scanner scanner = new Scanner(System.in);

			while (true) {
				System.out.println("\nWelcome to LENA Bank!");
				System.out.println("1. Add New Account");
				System.out.println("2. Login");
				System.out.println("3. Exit");
				System.out.print("Select option: ");
				String menuInput = scanner.nextLine().trim();

				int option = -1;
				try {
					option = Integer.parseInt(menuInput);
				} catch (NumberFormatException e) {
					System.out.println("Invalid option. Please enter 1-3.");
					continue;
				}

				if (option == 1) {
					Account account = new Account(connection);
					String newUserId = account.createAccount(scanner);
					if (newUserId != null) {
						System.out.println("Logging you in automatically as " + newUserId);
						showUserIdSessionMenu(connection, scanner, newUserId);
					} else {
						System.out.println("Account creation failed. Returning to main menu.");
					}
				} else if (option == 2) {
					while (true) {
						System.out.println("Login by:");
						System.out.println("1. User ID");
						System.out.println("2. Account Number");
						System.out.println("3. Find UserID using Account Number");
						System.out.println("4. Find Account Number using UserID");
						System.out.println("5. Forgot Password");
						System.out.println("6. Back");
						System.out.print("Select option: ");
						String loginTypeStr = scanner.nextLine().trim();
						int loginType = -1;
						try {
							loginType = Integer.parseInt(loginTypeStr);
						} catch (NumberFormatException e) {
							System.out.println("Invalid option. Please enter 1-6.");
							continue;
						}
						Authentication auth = new Authentication(connection);
						BankOperations bankOps = new BankOperations(connection);

						if (loginType == 1) {
							String userId = auth.loginByUserId(scanner);
							if (userId != null) {
								showUserIdSessionMenu(connection, scanner, userId);
							}
							break;
						} else if (loginType == 2) {
							Integer accountNumber = auth.loginByAccountNumber(scanner);
							if (accountNumber != null) {
								showAccountNumberSessionMenu(connection, scanner, accountNumber);
							}
							break;
						} else if (loginType == 3) { 
							bankOps.findUserIdByAccountNumber(scanner);
						} else if (loginType == 4) { 
							bankOps.findAccountNumberByUserId(scanner);
						} else if (loginType == 5) { 
							forgotPasswordMenu(scanner, connection); 
						} else if (loginType == 6) {
							break;
						} else {
							System.out.println("Invalid option. Please enter 1-6.");
						}
					}
				} else if (option == 3) {
					System.out.println("Exiting. Thank you!");
					break;
				} else {
					System.out.println("Invalid option. Please enter 1-3.");
				}
			}

			scanner.close();
			connection.close();

		} catch (Exception e) {
			System.out.println("Error starting application: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
