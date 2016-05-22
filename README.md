# GTPRest
GTP to RESTful services for http://goratingserver.appspot.com/

This provides simple java code for connecting a GTP (Go Text Protocol) interface to the RESTful services of GRS (http://goratingserver.appspot.com/).

The server uses Chinese rules, there is no negotiation for scoring (see scoring protocol below) and clock keeps running during scoring, no disconnect protection and no undo. There are no handicaps and players are paired with similar strength players to the extent possible.

- Move.java allows translation between GTP style coordinates and json encoding used by GRS. 
- GTP.java class is extended to customize it to given GTP implementation. There is a sample implementation for command line Pachi GTP in PachiGTP.java and the main function to run it. With GTP implementations supporting kgs-genmove_cleanup all that is required is providing the input and output streams of the bots get interface to the GTP class.
- Rest.java provides an interface to the restful services. 
- Mediator.java connects the Rest class to the custom GTP implementation.

## The rest interface required to operate a bot is very simple. 
 - retrieve user information, this translates userid to a name, or alternatively registers a new account if 0 is given as user id.
 - poll for game, retries game id for a game that is still in progress by the player, or alternatively tries to pair with the time settings requested, this method timeouts in 20 seconds with a 408 error code if no suitable pairing is found.
 - retrieve game information, provides game information for the game which id was provided. the relevant information is deducing which color user is as well as existing moves already played if this is a game already in progress.
- poll for game update, gets the move corresponding to the provided move number and game id, if no such move has became available within 20 seconds this method timeouts with a 408 error code.
- play given move, duh.
- retrieve dead list, gets a list of stones that server considers dead for the given game.

## Brief explanation of the scoring protocol used.
After two consecive passes, active player should fetch dead list from server, and compare it to its own dead list. If bots oppion on dead stones matches servers oppinion bot should send a move named "Accept" to the server, otherwise send a move named "Reject". If player sends an "Accept", the other player should then repeat this process (active player, when last move is Accept and preceding move is a Pass). If both players accepted, the game is scored according to the dead list provided by the server. If either player rejected, play should continue with cleanup, capturing all dead stones, next time both players pass the game is scored by the server as if all stones on the board are alive.
