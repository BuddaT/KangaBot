# KangaBot
Remote server administration IRC bot for Wurm Unlimited servers.

# Required Libraries
- pircbotx-2.0.1
- guava-18.0
- slf4j-api-1.7.13
- slf4j-simple-1.7.13
- commons-codec-1.10
- commons-lang3-3.4

# Additional Classes Required
- com.wurmonline.server.players.BannedIp
- com.wurmonline.server.webinterface.WebInterface
- com.wurmonline.shared.exceptions.WurmException
- com.wurmonline.shared.exceptions.WurmServerException

# Usage
Package the project as a runnable jar, with the required classes and libraries in jar, or specified in the classpath at runtime.
Edit config.ini to fit your desired usage.
Run the jar, bot will connect to channels defined in config.ini where you may give it commands.

# Config Options
- "BOTNAME=String": Sets the name of the bot in IRC
- "IRCSERVER=String": Sets the IRC server the bot will connect to
- "CHANNEL=String[,String]": Adds a channel for the bot to join, optional password as a second string after comma delimiter
- "CHECKCHANNELS=Boolean": Flag whether to only accept commands from the given channels
- "USER=String": Adds a verified user and flags the bot to only accept commands from verified users
- "HOSTMASK=String": Adds a verified user hostmask and flags the bot to only accept commands from verified hostmasks
- "CONNECTIONSTRING=String": URL of the WebInterface of the WU Server
- "WEBINTERFACEPASSWORD=String": Password for connecting to the WebInterface of the WU Server

# IRC Commands
- "!connect [-f]": Bot attempts to connect to the WU Server. Optional "-f" flag to force a disconnect and reconnect attempts
- "!disconnect": Bot disconnects from the WU Server
- "!announce": Send a broadcast message to the WU Server
- "!am <user> <iron_amnt> <comment>" / "!addmoney <user> <iron_amnt> <comment>": Adds the specified iron amount to the user's bank on the connected WU server
- "!rm <user> <iron_amnt>" / "!removemoney <user> <iron_amnt>": Removed the specified iron amount from the user's bank on the connected WU server
- "!shutdown <min> <message>": Starts the shutdown process for the specified minutes, with the specified message