# GTPRest
This provides simple java code for connecting a GTP (Go Text Protocol) interface to the the GRS (http://goratingserver.appspot.com/).

## http://goratingserver.appspot.com/
The server uses Chinese rules, there is no negotiation for scoring (see scoring protocol below) and clock keeps running during scoring, no disconnect protection and no undo. There are no handicaps and players are paired with similar strength players to the extent possible, bots can be paired against each other. Upstream is via restful, and downstream via websockets.

- MimicKGS.jar contains the lib and src folders compiled into one jar. It can be run from command line with java -jar MimicKGS.jar to connect a GTP command line bot to the GRS. It needs the properties.ini file in the same directory for configurations.
- properties.ini is simple configuration file for the bot. It specifies which account to use for the bot, how the bot is started (possible command line options etc), and if the bot plays blits or fast or both.

## Brief explanation of the scoring protocol used.
After two consecive passes the server provides a list of dead stones. The player whose turn it is, should wait for the list of dead stones from the server, and then reply with an accept or reject move, depending on if he agrees with the estimate of dead stones. If he accepts, the other player is posed the same question. If both accept, the game is scored accordingly. After a player rejects the estimate, game continues, next time both players pass the game will be scored as if all stones on the board were alive. As chinese rules are used, no points are lost removing dead stones from the board.
