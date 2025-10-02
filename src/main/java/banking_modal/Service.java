package banking_modal;

import java.util.Scanner;
import database_modal.TableConnector;

public class Service
{
    private static Scanner scanner = new Scanner(System.in);
    private static Authorization auth = new Authorization();
    private static Account account = new Account();

    public static void main(String[] args)
    {
        TableConnector conn = TableConnector.getInstance();

        try
        {
            while (true)
            {
                System.out.println("==================================");
                System.out.println("        Welcome to MyBank         ");
                System.out.println("==================================");
                System.out.println("1. Login");
                System.out.println("2. Exit");
                System.out.print("Select an option: ");
                int choice = Integer.parseInt(scanner.nextLine());

                if (choice == 1)
                {
                    if (loginFlow())
                    {
                        accountMenu();
                    }
                }
                else if (choice == 2)
                {
                    System.out.println("Thank you for visiting MyBank!");
                    break;
                }
                else
                {
                    System.out.println("Invalid choice! Try again.");
                }
            }
        }
        finally
        {
            conn.close();
        }
    }

    private static boolean loginFlow()
    {
        System.out.println();
        System.out.println("==================================");
        System.out.println("           LOGIN MENU             ");
        System.out.println("==================================");
        System.out.print("Enter Account ID: ");
        String accountId = scanner.nextLine();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        String session = auth.LoginAccount(accountId, password);
        if (session.equals("0"))
        {
            System.out.println("Login failed! Invalid credentials.");
            return false;
        }

        SessionHolder.sessionId = session;
        SessionHolder.customerId = auth.getCustomerId(session);

        System.out.println("Login successful! Customer ID: " + SessionHolder.customerId);
        return true;
    }

    private static void accountMenu()
    {
        while (true)
        {
            System.out.println("==================================");
            System.out.println("        Account Menu              ");
            System.out.println("==================================");
            System.out.println();
            System.out.println("1. Create New Account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Logout");
            System.out.print("Select an option: ");
            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice)
            {
                case 1:
                    createAccountFlow();
                    break;
                case 2:
                    depositFlow();
                    break;
                case 3:
                    withdrawFlow();
                    break;
                case 4:
                    transferFlow();
                    break;
                case 5:
                    auth.Logout(SessionHolder.sessionId);
                    System.out.println("Logged out successfully.");
                    return;
                default:
                    System.out.println("Invalid choice! Try again.");
            }
        }
    }

    private static void createAccountFlow()
    {
        BuildAccountDetails builder = new BuildAccountDetails(SessionHolder.customerId);
        account.CreateAccount(builder, "Primary");
        System.out.println("Primary account created successfully.");
    }

    private static void depositFlow()
    {
        System.out.print("Enter amount to deposit: ");
        double amount = Double.parseDouble(scanner.nextLine());
        boolean success = account.Deposit(SessionHolder.sessionId, amount);
        if (success)
            System.out.println("Deposit successful! Amount: " + amount);
        else
            System.out.println("Deposit failed!");
    }

    private static void withdrawFlow()
    {
        System.out.print("Enter amount to withdraw: ");
        double amount = Double.parseDouble(scanner.nextLine());
        boolean success = account.Withdraw(SessionHolder.sessionId, amount);
        if (success)
            System.out.println("Withdrawal successful! Amount: " + amount);
        else
            System.out.println("Insufficient balance or error!");
    }

    private static void transferFlow()
    {
        System.out.print("Enter target account number: ");
        long targetAccount = Long.parseLong(scanner.nextLine());
        System.out.print("Enter amount to transfer: ");
        double amount = Double.parseDouble(scanner.nextLine());

        boolean success = account.TransferMoney(SessionHolder.sessionId, null, targetAccount, amount);
        if (success)
            System.out.println("Transfer successful! Amount: " + amount);
        else
            System.out.println("Transfer failed! Check balance or account number.");
    }

    private static class SessionHolder
    {
        static String sessionId;
        static int customerId;
    }
}