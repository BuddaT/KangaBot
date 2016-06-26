package net.buddat.kangabot;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class BotConfig {

	private String botName;
	
	private String ircServer;
	
	private ArrayList<Channel> channelList = new ArrayList<Channel>();
	
	public BotConfig(String configFile) {
		try {
			BufferedReader configReader = new BufferedReader(new FileReader(configFile));
			String line;
			while ((line = configReader.readLine()) != null) {
				processConfig(line);
			}
			configReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("FATAL ERROR: CONFIG FILE \"" + configFile + "\" NOT FOUND");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("FATAL ERROR: CONFIG FILE \"" + configFile + "\" ERROR READING");
			e.printStackTrace();
		}
	}
	
	public String getName() {
		return botName;
	}
	
	public String getServer() {
		return ircServer;
	}
	
	public ArrayList<Channel> getChannels() {
		return channelList;
	}
	
	private void processConfig(String line) {
		String[] splits = line.split("=");
		if (splits.length < 2)
			return;
		
		if (splits[0].equalsIgnoreCase("BOTNAME")) {
			botName = splits[1];
			return;
		}
		
		if (splits[0].equalsIgnoreCase("IRCSERVER")) {
			ircServer = splits[1];
			return;
		}
		
		if (splits[0].equalsIgnoreCase("CHANNEL")) {
			String[] chanPass = splits[1].split(",");
			if (chanPass.length < 2)
				channelList.add(new Channel(chanPass[0]));
			else
				channelList.add(new Channel(chanPass[0], chanPass[1]));
			
			KangaBot.verifyChannel(chanPass[0]);
			return;
		}
		
		if (splits[0].equalsIgnoreCase("CHECKCHANNELS")) {
			boolean check = false;
			try {
				check = Boolean.parseBoolean(splits[1]);
			} catch (Exception e) {
				return;
			}
			
			KangaBot.setCheckChannel(check);		
			return;
		}
		
		if (splits[0].equalsIgnoreCase("USER")) {
			KangaBot.verifyName(splits[1]);
			return;
		}
		
		if (splits[0].equalsIgnoreCase("HOSTMASK")) {
			KangaBot.verifyHostmask(splits[1]);
			return;
		}
		
		if (splits[0].equalsIgnoreCase("CONNECTIONSTRING")) {
			KangaBot.setConnectionString(splits[1]);
			return;
		}
		
		if (splits[0].equalsIgnoreCase("WEBINTERFACEPASSWORD")) {
			KangaBot.setServerWIPassword(splits[1]);
			return;
		}
	}
	
	class Channel {
		String channelName;
		String channelPassword;
		
		Channel(String name, String password) {
			channelName = name;
			channelPassword = password;
		}
		
		Channel(String name) {
			this(name, null);
		}
	}
}
