package net.buddat.kangabot;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;

import com.google.common.base.Joiner;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import com.wurmonline.server.webinterface.WebInterface;

import net.buddat.kangabot.BotConfig.Channel;

public class KangaBot extends ListenerAdapter {
	
	private static final String CONFIG_FILE = "./config.ini";
	
	private static String connectionString = "//localhost:7220/wuinterface";
	private static String webInterfacePassword = "password";
	
	private static final ArrayList<String> VERIFIED_CHANNELS = new ArrayList<String>();
	private static final ArrayList<String> VERIFIED_NAMES = new ArrayList<String>();
	private static final ArrayList<String> VERIFIED_HOSTMASKS = new ArrayList<String>();

	private static final int FREEDOM = 4;

	private static boolean checkNames = false;
	private static boolean checkChannel = false;
	private static boolean checkHostmask = false;

	public static void setConnectionString(String newConnString) {
		KangaBot.connectionString = newConnString;
	}
	
	public static void setServerWIPassword(String password) {
		KangaBot.webInterfacePassword = password;
	}
	
	public static void setCheckChannel(boolean checkChannel) {
		KangaBot.checkChannel = checkChannel;
	}
	
	public static void verifyChannel(String channel) {
		VERIFIED_CHANNELS.add(channel);
	}
	
	public static void verifyName(String name) {
		checkNames = true;
		VERIFIED_NAMES.add(name);
	}
	
	public static void verifyHostmask(String hostmask) {
		checkHostmask = true;
		VERIFIED_HOSTMASKS.add(hostmask);
	}
	
	private WebInterface wiConnection;

	public KangaBot() {
		BotConfig botConfig = new BotConfig(CONFIG_FILE);
		
		ArrayList<Channel> allChannels = botConfig.getChannels();
		
		Configuration.Builder<PircBotX> configBuilder = new Configuration.Builder<>();
		configBuilder.setName(botConfig.getName());
		configBuilder.setServerHostname(botConfig.getServer());
		configBuilder.addListener(this);
		
		for (Channel c : allChannels) {
			if (c.channelPassword == null)
				configBuilder.addAutoJoinChannel(c.channelName);
			else
				configBuilder.addAutoJoinChannel(c.channelName, c.channelPassword);
		}

		Configuration<PircBotX> ircConfig = configBuilder.buildConfiguration();
		PircBotX ircBot = new PircBotX(ircConfig);

		try {
			ircBot.startBot();
		} catch (IOException | IrcException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onMessage(MessageEvent event) throws KangaBotException {
		if (event == null) {
			System.err.println("Null event found in onMessage");
			throw new KangaBotException("Null event encountered");
		}
		String[] splits = event.getMessage().split(" ");
		
		if (!splits[0].startsWith("!"))
			return;
		
		if (checkChannel && !VERIFIED_CHANNELS.contains(event.getChannel().getName())) {
			event.respond("Invalid channel.");
			return;
		}
		
		if (checkNames && !VERIFIED_NAMES.contains(event.getUser().getNick())) {
			event.respond("Invalid user.");
			return;
		}
		
		if (checkHostmask && !VERIFIED_HOSTMASKS.contains(event.getUser().getHostmask())) {
			event.respond("Invalid hostmask.");
			return;
		}

		final String command = splits[0].toLowerCase();
		try {
			switch (command) {
				case "!connect":
					newConnection(event, splits);
					break;
				case "!disconnect":
					wiConnection = null;
					break;
				case "!announce":
					announce(event, splits);
					break;
				case "!am":
				case "!addmoney":
					addMoney(event, splits);
					break;
				case "!who":
					who(event);
					break;
				case "!rm":
				case "!removemoney":
					removeMoney(event, splits);
					break;
				case "!shutdown":
					shutdownServer(event, splits);
					break;
				case "!uptime":
					uptime(event);
					break;
				default:
					event.respond("Unknown command.");
					break;
			}
		} catch (RuntimeException e) {
			// PircbotX 2.0.1 appears to silently swallow exceptions within
			// onMessage. Should at least report them.
			KangaBotException kbe =
					new KangaBotException("Uncaught exception while invoking " +
					"command " + command, e);
			kbe.printStackTrace();
			throw kbe;
		}
	}

	private void shutdownServer(MessageEvent event, String[] splits) {
		if (wiConnection == null) {
			event.respond("Must be connected to the server first.");
		}
		if (splits.length < 3) {
			event.respond("Usage: !shutdown <mins> <reason>");
			return;
		}
		
		int seconds;
		try {
			seconds = Integer.parseInt(splits[1]);
			seconds *= 60;
		} catch (NumberFormatException nfe) {
			event.respond(splits[1] + " is not a valid number.");
			return;
		}
		
		String reason = "";
		for (int i = 2; i < splits.length; i++)
			reason += splits[i] + " ";
		
		try {
			wiConnection.startShutdown(webInterfacePassword, event.getUser().getNick(), seconds, reason);
			event.respond("Shutdown scheduled for " + splits[1] + " minutes with reason: " + reason);
			event.respond("Terminating connection to server.");
			wiConnection = null;
		} catch (RemoteException e) {
			e.printStackTrace();
			event.respond("Error occured when shutting down: " + e.getMessage());
		}
	}
	
	private void announce(MessageEvent event, String[] splits) {
		if (wiConnection == null) {
			event.respond("Must be connected to the server first.");
		}
		if (splits.length < 2) {
			event.respond("Usage: !announce <msg>");
			return;
		}
		
		String msg = "";
		for (int i = 1; i < splits.length; i++)
			msg += splits[i] + " ";
		
		try {
			wiConnection.broadcastMessage(webInterfacePassword, msg);
			event.respond("Announcement successfully sent.");
		} catch (RemoteException e) {
			e.printStackTrace();
			event.respond("Error occured when sending announcement: " + e.getMessage());
		}
	}
	
	private void addMoney(MessageEvent event, String[] splits) {
		if (wiConnection == null) {
			event.respond("Must be connected to the server first.");
		}
		if (splits.length < 4) {
			event.respond("Usage: !addMoney <name> <ironAmnt> <comment>");
			return;
		}
		
		int amnt;
		try {
			amnt = Integer.parseInt(splits[2]);
		} catch (NumberFormatException nfe) {
			event.respond(splits[2] + " is not a valid number.");
			return;
		}
		
		try {
			Map<String,String> response = wiConnection.doesPlayerExist(webInterfacePassword, splits[1]);
			if (!response.get("ResponseCode").equals("OK")) {
				event.respond("Error: " + response.get("ErrorMessage"));
				return;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
			event.respond("Error occured when checking player: " + e.getMessage());
		}
		
		String comment = "";
		for (int i = 3; i < splits.length; i++)
			comment += splits[i] + " ";
		
		try {
			Map<String, String> response = wiConnection.addMoneyToBank(webInterfacePassword, splits[1], amnt, comment);
			if (response.get("error") != null) {
				event.respond("Error: " + response.get("error"));
				return;
			}
			if (response.get("ok") != null) {
				event.respond("Ok: " + response.get("ok"));
			}
		} catch (RemoteException e) {
			e.printStackTrace();
			event.respond("Error occured when adding money: " + e.getMessage());
		}
	}

	private void removeMoney(MessageEvent event, String[] splits) {
		if (wiConnection == null) {
			event.respond("Must be connected to the server first.");
		}
		if (splits.length < 3) {
			event.respond("Usage: !removeMoney <name> <ironAmnt>");
			return;
		}
		
		int amnt;
		try {
			amnt = Integer.parseInt(splits[2]);
		} catch (NumberFormatException nfe) {
			event.respond(splits[2] + " is not a valid number.");
			return;
		}
		
		try {
			Map<String,String> response = wiConnection.doesPlayerExist(webInterfacePassword, splits[1]);
			if (!response.get("ResponseCode").equals("OK")) {
				event.respond("Error: " + response.get("ErrorMessage"));
				return;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
			event.respond("Error occured when checking player: " + e.getMessage());
		}
		
		try {
			long response = wiConnection.chargeMoney(webInterfacePassword, splits[1], amnt);
			event.respond("Money successfully removed, new balance: " + response);
		} catch (RemoteException e) {
			e.printStackTrace();
			event.respond("Error occured when removing money: " + e.getMessage());
		}
	}

	private void uptime(MessageEvent event) {
		if (activeConnection(event)) {
			try {
				event.respond(wiConnection.getUptime(webInterfacePassword));
			} catch (RemoteException e) {
				e.printStackTrace();
				event.respond("Error occurred while trying to get uptime: " + e.getMessage());
			}
		}
	}

	private void who(MessageEvent event) {
		if (activeConnection(event)) {
			try {
				Map<Long, String> players = wiConnection.getPlayersForKingdom(webInterfacePassword,
						FREEDOM);
				if (players.isEmpty()) {
					event.respond("There are currently no players logged in.");
				} else {
					event.respond(Joiner.on(", ").join(players.values()));
				}
			} catch (RemoteException e) {
				e.printStackTrace();
				event.respond("Couldn't retrieve players: " + e.getMessage());
			}
		}
	}

	private void newConnection(MessageEvent event, String[] splits) {
		if (wiConnection != null && splits.length > 1)
			if (splits[1].equalsIgnoreCase("-f")) {
				wiConnection = null;
				event.respond("Existing connection terminated.");
			}
		
		if (wiConnection == null) {
			connect(event);
		} else {
			event.respond("Connection already exists. Use '!connect -f' to force a new connection.");
		}
	}

	private boolean connect(MessageEvent event) {
		try {
			wiConnection = (WebInterface) Naming.lookup(connectionString);
			event.respond("Connection successful.");
			return true;
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			e.printStackTrace();
			event.respond("Connection unsuccessful. " + e.getMessage());
		}
		return false;
	}

	/**
	 * Checks whether or not there is an active connection. If there is,
	 * returns true. If there is not, attempts to connect and returns true if
	 * the the connection was successful, otherwise false.
	 * @param event Event on which to response
	 * @return True if there is an active connection after calling, otherwise
	 * false.
     */
	private boolean activeConnection(MessageEvent event) {
		if (wiConnection == null) {
			event.respond("No active connection, attempting to connect...");
			return connect(event);
		}
		return true;
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			// -t parameter directs a brief test of the webinterface lookup
			if ("-t".equals(args[0])) {
				new BotConfig(CONFIG_FILE);
				try {
					System.out.println("Attempting connection to " + connectionString);
					WebInterface wiConnection = (WebInterface) Naming.lookup(connectionString);
					if (wiConnection != null) {
						System.out.println("Connection successful.");
					} else {
						System.out.println("Connection unsuccessful.");
					}
				} catch (MalformedURLException | RemoteException | NotBoundException e) {
					e.printStackTrace();
					System.err.println("Connection unsuccessful: " + e.getMessage());
				}
				System.exit(0);
			}
		} else {
			new KangaBot();
		}
	}

}
