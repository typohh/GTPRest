package typo.gtp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

public class Rest {

	private String mServerUrl = "http://goratingserver.appspot.com/rankingserver/restful";
	private boolean mBlitz;
	private boolean mFast;
	
	private long mGameId;
	private String mBotName;
	private boolean mBotIsBlack;
	private long mUserId;
	private int mMoveNumber=0;
	private long mByomiTime;
	
	private JSONObject pollJson( String pUrl ) throws IOException {
		System.out.println( "requested " + pUrl );
		while( true ) {
			URL url = new URL( pUrl );
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod( "GET" );
			conn.setRequestProperty( "Accept" , "application/json" );
			if( conn.getResponseCode() == 408 ) {
				System.out.println( "retrying.." );
				continue;
			}
			if( conn.getResponseCode() != 200 ) {
				throw new Error( pUrl + " : " + conn.getResponseCode() );
			}
			BufferedReader br = new BufferedReader( new InputStreamReader( (conn.getInputStream()) ) );
			String json = br.readLine();
			conn.disconnect();
			System.out.println( "recieved " + json );
			return new JSONObject( json );
		}
	}

	protected void pollNewGame() throws IOException {
		JSONObject json = pollJson( mServerUrl + "?c=png&u=" + mUserId + "&b=" + mBlitz + "&f=" + mFast );
		mGameId = json.getLong( "gameId" );
	}

	protected List<Move> getGameData() throws IOException {
		JSONObject json = pollJson( mServerUrl + "?c=ggd&g=" + mGameId );
		mBotIsBlack = json.getJSONObject( "black" ).getString( "name" ).equals( mBotName );
		mByomiTime = json.getLong( "timePerPeriod" );
		JSONArray moves = json.getJSONArray( "moves" );
		Vector<Move> movesOut=new Vector<>();
		for( int i = 0 ; i < moves.length() ; ++i ) {
			String s = moves.getString( i );
			movesOut.addElement( Move.parseJson( s ) );
		}
		mMoveNumber=movesOut.size();
		return movesOut;
	}

	protected void getUserData() throws IOException {
		JSONObject json = pollJson( mServerUrl + "?c=gi&u=" + mUserId );
		if( mUserId != json.getLong( "id" ) ) {
			System.err.println( "new userid is : " + json.getLong( "id" ) );
		}
		mUserId = json.getLong( "id" );
		mBotName = json.getString( "name" );
	}

	public boolean isBotBlack() {
		return mBotIsBlack;
	}
	
	public long getByomiTime() {
		return mByomiTime;
	}
	
	public Set<Move> getDeadList() throws IOException {
		JSONObject json = pollJson( mServerUrl + "?c=gdl&g=" + mGameId );
		JSONArray moves = json.getJSONArray( "moves" );
		HashSet<Move> movesOut=new HashSet<>();
		for( int i = 0 ; i < moves.length() ; ++i ) {
			String s = moves.getString( i );
			movesOut.add( Move.parseJson( s ) );
		}
		return movesOut;
	}
	
	public Move getMove() throws IOException {
		JSONObject json = pollJson( mServerUrl + "?c=pgu&g=" + mGameId + "&n=" + mMoveNumber );
		++mMoveNumber;
		return Move.parseJson( json.getString( "move" ) );
	}

	public void sendMove( Move pMove ) throws IOException {
		JSONObject json = pollJson( mServerUrl + "?c=sm&u=" + mUserId + "&n=" + mMoveNumber + "&m=" + pMove.toJson() );
		++mMoveNumber;
		if( Move.parseJson( json.getString( "move" ) ) == Move.sTimeLoss ) {
			throw new Error( "timeloss, wtf?!?!?" );
		}
	}

	public List<Move> startGame() throws IOException {
		getUserData();
		pollNewGame();
		return getGameData();
	}

	public Rest( long pUserId , boolean pBlitz , boolean pFast ) {
		mUserId=pUserId;
		mFast=pFast;
		mBlitz=pBlitz;
	}

	public boolean isBlacksTurn() {
		return mMoveNumber % 2 == 0;
	}
	
}
