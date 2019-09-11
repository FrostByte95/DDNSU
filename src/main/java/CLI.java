import java.io.IOException;

import java.util.Scanner;

import static com.jrelich.tools.network.gdyndns.GDynDNS.*;

/**
 * CLI Example using GDynDNS
 * @author Joshua Relich
 * @version 1.0
 * @since 09/08/2019
 */

public final class CLI {
	private final static String version = "1.0";
	/**
	 * CLI
	 * Gets required arguments and performs update
	 * @param args Arguments should be given in this order: Username, Password, Hostname, IP(optional), offline(optional)
	 */
	public static void main(String[] args){
		Scanner scanner = new Scanner(System.in);
		String Username = "";
		String Password = "";
		String Hostname = "";
		String IP = "";
		boolean Offline = false;

		printHeader();
		//get args
		if (args.length < 3 && args.length > 0){
			System.out.println("Required arguments not given.");
			System.out.println("Usage: ddu Username, Password, Hostname, IP(optional), offline(optional)");
			System.out.print("");
			String hold = scanner.nextLine();
			System.exit(0);
		}
		if(args.length > 0) {
			for (int i = 0; i < args.length; i++){
				switch(i){
					case 0:
						Username = args[0];
						break;
					case 1:
						Password = args[1];
						break;
					case 2:
						Hostname = args[2];
						break;
					case 3:
						if (args[3].equalsIgnoreCase("offline"))
							Offline = true;
						else
							IP = args[3];
						break;
					case 4:
						if (args[4].equalsIgnoreCase("offline"))
							Offline = true;
						else
							IP = args[4];
						break;
				}
			}
		}

		//get args from System.in if no arguments supplied
		if(args.length == 0){
			System.out.println("Please provide the required information to update google dynamic dns settings");
			System.out.print("Username: ");
			Username = scanner.nextLine();
			System.out.print("Password: ");
			Password = scanner.nextLine();
			System.out.print("Hostname: ");
			Hostname = scanner.nextLine();
			System.out.print("IP (leave blank to get ip from internet): ");
			IP = scanner.nextLine();
			do {
				System.out.print("Offline mode (yes/no): ");
				String om = scanner.nextLine();
				if (om.trim().equalsIgnoreCase("yes")){
					Offline = true;
					break;
				}
				else if (om.trim().equalsIgnoreCase("no")) {
					Offline = false;
					break;
				}
			}while(true);
		}

		//get IP if not supplied
		if (IP.isEmpty()){
			try {
				System.out.println("Getting your public IPv4 address from http://checkip.amazonaws.com");
				IP = getPubIPv4();
				System.out.println("Your public IPv4 is: " + IP);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Could not get your IP! Exiting!");
				System.exit(1);
			}
		}
		if (!Offline)
			System.out.println("Setting " + Hostname +" to " + IP);
		else
			System.out.println("Setting" + Hostname + " offline");

		String response = update(Username, Password, Hostname, IP, Offline);
		responseHandler(response);

		//ask if user wants to keep program running
		boolean loop = false;
		System.out.println("Check for IP change every 24H and update? (yes/no):");
		loop = yesNo(scanner.nextLine());
		while (loop){
			try {
				Thread.sleep(hToMs(24));
				String temp = getPubIPv4();
				if(!IP.equals(temp)){
					IP = temp;
					response = update(Username, Password, Hostname, IP, Offline);
					responseHandler(response);
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}


	}

	public static void printHeader(){
		System.out.println("Dynamic DNS Updater Ver. " + version);
		System.out.println("\u00a92019 Joshua Relich");
		System.out.println("-----------------------------------\n");
	}

	public static boolean yesNo(String ans){
		if (ans.trim().equalsIgnoreCase("yes")){
			return true;
		}
		else if (ans.trim().equalsIgnoreCase("no")) {
			return false;
		}
		return false;
	}

	public static String update(String username, String password, String hostname, String ip, boolean offline){
		//update DNS settings
		String response = "";
		try {
			response = updateGoogleDNS(username, password, hostname, ip, offline);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(response.isEmpty()){
			System.out.println("No response from server");
		} else{
			System.out.println("Server response: " + response);
		}
		return response;
	}

	//convert hours to milliseconds
	public static int hToMs(int hours){
		return hours*60*60*1000;
	}

	//check response
	//end program if bad response
	public static void responseHandler(String response){
		if(response.contains("good")){
			System.out.println("DNS update successful");
		}
		else if (response.contains("nochg")){
			System.out.println("The supplied IP address is already set for this host. You should not attempt another update until your IP address changes.");
		}
		else if (response.contains("nohost")){
			System.out.println("ERROR: Hostname does not exits or does not have Dynamic DNS enabled.");
			System.exit(0);
		}
		else if (response.contains("badauth")){
			System.out.println("ERROR: The username / password combination is not valid for the specified host.");
			System.exit(0);
		}
		else if (response.contains("notfqdn")){
			System.out.println("ERROR: The supplied hostname is not a valid fully-qualified domain name.");
			System.exit(0);
		}
		else if (response.contains("badagent")){
			System.out.println("ERROR: Your Dynamic DNS client is making bad requests. Ensure the user agent is set in the request.");
			System.out.println("If you are seeing this error please report this bug on github");
			System.exit(0);
		}
		else if (response.contains("abuse")){
			System.out.println("ERROR: Dynamic DNS access for the hostname has been blocked due to failure to interpret previous responses correctly.");
			System.exit(0);
		}
		else if (response.contains("911")){
			System.out.println("ERROR: Google is having a bad day. Wait 5 minutes and retry.");
			System.exit(0);
		}
		else if (response.contains("conflict")){
			System.out.println("ERROR: A custom A or AAAA resource record conflicts with the update. Delete the indicated resource record within DNS settings page and try the update again.");
			System.exit(0);
		}
	}
}


