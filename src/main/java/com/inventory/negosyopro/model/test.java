package com.inventory.negosyopro.model;
import java.util.Scanner;

public class test {

    //keyword parts = MINE
    //draft expected layout of backend
    //tasks identified, fill necessary logic to some parts

    // ===========================================
    // MAIN OUPUT
    // ===========================================

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        clearScreen();

        // Account creation
        System.out.println("Login/sign up");
        System.out.print("Enter to continue: ");
        String next = sc.nextLine();

        clearScreen();

        // Company Creation
        System.out.println("Company Creation");
        System.out.print("Enter to continue: ");
        next = sc.nextLine();

        do{
        clearScreen();

        // Menu
        System.out.println("= = = = = Menu = = = = =");
        System.out.println("[1] Dashboard");
        System.out.println("[2] Products");
        System.out.println("[3] Sales/POS");
        System.out.println("[4] Suppliers");
        System.out.println("[5] Reports");
        System.out.println("[6] Settings");
        System.out.println("[7] My account");
        System.out.println("[8] Logout");
        System.out.println("= = = = = = = = = = = = =");  
        System.out.print("Choose: ");
        int choose = sc.nextInt();
        
        sc.nextLine();
        clearScreen();

        switch(choose){
            case 1: Dashboard(next, sc);
                break;
            case 2: Products(next, sc);
                break;
            case 3: Sales_POS(next, sc);
                break;
            case 4: Suppliers(next, sc);
                break;
            case 5: Reports(next, sc);
                break;
            case 6: Settings(next, sc);
                break;
            case 7: MyAccount(next, sc);
                break;
            case 8: Logout(next, sc);
                break;
        }
    }while(true);
}

    // ===========================================
    // Functions
    // ===========================================

    // Dashboard = = = = = = = =
    public static void Dashboard(String next, Scanner sc){
        System.out.println("= = = = = Dashboard = = = = =");
        System.out.println("*Sales & inventory overview*");
        System.out.println("Ai Summary"); // ai summary (MINE)
        System.out.println("= = = = = = = = = = = = = = =");
        System.out.print("Enter to back:");
        next = sc.nextLine();
    }

    // Products = = = = = = = =
    public static void Products(String next, Scanner sc){
        System.out.println("= = = = = Products = = = = =");
        System.out.println("*List of products*");
        System.out.println("All, Low, Out, Expiring tabs");
        System.out.println("= = = = = = = = = = = = = = =");
        System.out.print("Enter to back:");
        next = sc.nextLine();
    }

    // Sales/POS = = = = = = = =
    public static void Sales_POS(String next, Scanner sc){
        System.out.println("= = = = = Sales/POS = = = = =");
        System.out.println("[X] New sale");
        System.out.println("[2] Checkout Details"); // MINE
        System.out.println("[X] Transactions");
        System.out.println("= = = = = = = = = = = = = = =");
        System.out.print("Enter to back:");
        next = sc.nextLine();
        Checkout_Details(next, sc);
    }

    public static void Checkout_Details(String next, Scanner sc){
        System.out.println("= = = = = Checkout Details = = = = =");
        System.out.println("*list of ordered procuts*");
        System.out.println("*subtotal, discount, total*");
        System.out.println("Customer name: __");
        System.out.println("Contact: __");
        System.out.println("Payment method: __");
        System.out.println("Amount paid: __");
        System.out.println("*Submit button*");
        System.out.println("*full receipt*");
        System.out.println("= = = = = = = = = = = = = = =");
        System.out.print("Enter to back:");
        next = sc.nextLine();
    }

    // Suppliers = = = = = = = =
    public static void Suppliers(String next, Scanner sc){
        System.out.println("= = = = = Products = = = = =");
        System.out.println("[X] Suppliers");
        System.out.println("[X] Purchase Orders");
        System.out.println("[3] Restock suggestions"); //MINE
        System.out.println("= = = = = = = = = = = = = = =");
        System.out.print("Enter to next:");
        next = sc.nextLine();
        Restock_suggestions(next, sc);
    }

    public static void Restock_suggestions(String next, Scanner sc){
        System.out.println("= = = = = Restock = = = = =");
        System.out.println("*lists low stock items*");
        System.out.println("*request puchase order*");
        System.out.println("= = = = = = = = = = = = = = =");
        System.out.print("Enter to back:");
        next = sc.nextLine();
    }

    // Reports = = = = = = = =
    public static void Reports(String next, Scanner sc){
        System.out.println("= = = = = Reports = = = = =");
        System.out.println("*overview list*");
        System.out.println("Ai Summary"); //MINE
        System.out.println("= = = = = = = = = = = = = = =");
        System.out.print("Enter to back:");
        next = sc.nextLine();
    }

    // Settings = = = = = = = =
    public static void Settings(String next, Scanner sc){
        System.out.println("= = = = = Settings = = = = =");//MINE
        System.out.println("[1] Business info");
        System.out.println("[2] Tax and currency");
        System.out.println("[3] Payment methods");
        System.out.println("[4] Discounts");
        System.out.println("[5] Inventory ");
        System.out.println("[X] Users");  // NOT
        System.out.println("= = = = = = = = = = = = = = =");
        System.out.print("Enter to next:");
        next = sc.nextLine();
        Settings_Tabs(next, sc);
    }

    public static void Settings_Tabs(String next, Scanner sc){
        System.out.println("= = = = = Business Info = = = = =");
        System.out.println("Business name, address, contact");
        System.out.println("= = = = = = = = = = = = = = =");

        System.out.println("= = = = = Tax and currency = = = = =");
        System.out.println("vat rate, currency");
        System.out.println("*radio* VAT, prices oinclude VAT");
        System.out.println("= = = = = = = = = = = = = = =");

        System.out.println("= = = = = Discounts = = = = =");
        System.out.println("*radio* senior, spromo");
        System.out.println("= = = = = = = = = = = = = = =");

        System.out.println("= = = = = Inventory = = = = =");
        System.out.println("*radio* use expiry dates, use purchase oders");
        System.out.println("ordering cost, holding cost, expiry alert");
        System.out.println("= = = = = = = = = = = = = = =");
        System.out.print("Enter to back:");
        next = sc.nextLine();
    }

    // My Account = = = = = = = =
    public static void MyAccount(String next, Scanner sc){
        System.out.println("= = = = = My Account = = = = =");
        System.out.println("Name & Password");
        System.out.println("= = = = = = = = = = = = = = =");
        System.out.print("Enter to back:");
        next = sc.nextLine();
    }

    // Logout = = = = = = = =
    public static void Logout(String next, Scanner sc){ // MINE
        System.out.println("= = = = = Logout = = = = =");
        System.out.println("*Directs back to login*");
        System.out.println("= = = = = = = = = = = = = = =");
        System.out.print("Enter to back:");
        next = sc.nextLine();
    }

    // ===========================================
    // Other tools
    // =========================================
    public static void clearScreen() { //Clears screen for better readability
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }


}
