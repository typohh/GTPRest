# GTPRest
This provides simple java code for connecting a GTP (Go Text Protocol) interface to the the GRS (http://goratingserver.appspot.com/).

## http://goratingserver.appspot.com/
The server uses Chinese rules, there is no negotiation for scoring (see scoring protocol below) and clock keeps running during scoring, no disconnect protection and no undo. There are no handicaps and players are paired with similar strength players to the extent possible, bots can be paired against each other. Upstream is via restful, and downstream via websockets.

- MimicKGS.jar contains the lib and src folders compiled into one jar. It can be run from command line with java -jar MimicKGS.jar to connect a GTP command line bot to the GRS. It needs the properties.ini file in the same directory for configurations.
- properties.ini is simple configuration file for the bot. It specifies which account to use for the bot, how the bot is started (possible command line options etc), and if the bot plays blits or fast or both.

## Brief explanation of the scoring protocol used.
After two consecive passes, active player should fetch dead list from server, and compare it to its own dead list. If bots oppion on dead stones matches servers oppinion bot should send a move named "Accept" to the server, otherwise send a move named "Reject". If player sends an "Accept", the other player should then repeat this process (active player, when last move is Accept and preceding move is a Pass). If both players accepted, the game is scored according to the dead list provided by the server. If either player rejected, play continues with cleanup, capturing all dead stones, next time both players pass the game is scored by the server as if all stones on the board are alive.
