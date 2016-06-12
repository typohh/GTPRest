package typo.gtp;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import typo.gtp.ChannelAPI.ChannelException;

public class WebSocketEngine {

	String getTime() {
		final SimpleDateFormat sdfDate = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
		return sdfDate.format( new Date() );
	}

	private boolean mBlitz;
	private boolean mFast;
	private long mGameId;
	private String mBotName;
	private boolean mBotIsBlack;
	private long mLag;
	private long mUserId;
	private long mByomiTime;
	private Set<Move> mDead = new HashSet<>();
	private Vector<Move> mMoves = new Vector<>();
	private long mUnique;

	private boolean isCleanUp() {
		return mMoves.contains( Move.sReject );
	}

	private boolean isScoring() {
		return mMoves.size() > 1 && (mMoves.lastElement() == Move.sAccept || mMoves.lastElement() == Move.sPass) && mMoves.get( mMoves.size() - 2 ) == Move.sPass;
	}

	public boolean isOver() {
		if( mMoves.isEmpty() ) {
			return false;
		}
		Move moveB = mMoves.get( mMoves.size() - 1 );
		if( moveB == Move.sResign ) {
			return true;
		}
		if( moveB == Move.sTimeLoss ) {
			return true;
		}
		if( mMoves.size() > 1 ) {
			Move moveA = mMoves.get( mMoves.size() - 2 );
			if( moveA == Move.sAccept && moveB == Move.sAccept ) {
				return true;
			}
			if( moveA == Move.sPass && moveB == Move.sPass && isCleanUp() ) {
				return true;
			}
		}
		return false;
	}

	private long mPreviousGameId;

	private void processMessage( String pMessage ) throws IOException {
		JSONObject jo = new JSONObject( pMessage );
		long messageId = Long.parseLong( jo.getString( "messageId" ) );
		if( jo.getString( "type" ).equals( "gameupdate" ) && jo.getLong( "gameId" ) != mGameId && jo.getLong( "gameId" ) != mPreviousGameId ) {
			System.out.println( getTime() + " received move from unknown game, wont ack." );
			return; // message from the future, lets not ack this quite yet..
		}
		if( jo.getString( "type" ).equals( "gameupdate" ) && jo.getLong( "gameId" ) == mGameId && jo.getLong( "moveNumber" ) > mMoves.size() ) {
			System.out.println( getTime() + " received move from the future, wont ack." );
			return; // message from the future, lets not ack this quite yet, alternatively when this happens could create a new channel..
		}
		if( jo.getString( "type" ).equals( "gameData" ) && mBotName == null ) {
			System.out.println( getTime() + " received gamedata without a userdata first, wont ack." );
			return; // lets wait for userdata messsage first..
		}
		Rest.ack( mUserId , messageId );
		if( jo.getString( "type" ).equals( "userdata" ) ) {
			if( mGameId != 0 ) {
				System.out.println( getTime() + " http://goratingserver.appspot.com/sgf/" + mGameId + ".sgf" );
			}
			mBotName = jo.getString( "name" );
			System.out.println( getTime() + " rating is " + jo.getInt( "ratingMean" ) + "±" + jo.getInt( "ratingSD" ) + " after " + jo.getInt( "numberOfGames" ) + " games." );
			Rest.registerForPairing( mUserId , mBlitz , mFast );
			return;
		}
		if( jo.getString( "type" ).equals( "ping" ) ) {
			return;
		}
		if( jo.getString( "type" ).equals( "disconnect" ) ) {
			if( mUnique != Long.parseLong( jo.getString( "active" ) ) ) {
				System.out.println( getTime() + " this account has been logged into from elsewhere." );
			}
			return;
		}
		if( jo.getString( "type" ).equals( "deadlist" ) ) {
			JSONArray ja = jo.getJSONArray( "dead" );
			mDead = new HashSet<>();
			for( int i = 0 ; i < ja.length() ; ++i ) {
				mDead.add( Move.parseJson( ja.getString( i ) ) );
			}
		}
		if( jo.getString( "type" ).equals( "gamedata" ) ) {
			mDead = null;
			if( jo.getBoolean( "hasDead" ) ) {
				JSONArray jad = jo.getJSONArray( "dead" );
				mDead = new HashSet<>();
				for( int i = 0 ; i < jad.length() ; ++i ) {
					mDead.add( Move.parseJson( jad.getString( i ) ) );
				}
			}
			mBotIsBlack = jo.getString( "blackName" ).equals( mBotName );
			if( mBotIsBlack ) {
				System.out.println( getTime() + " playing against " + jo.getString( "whiteName" ) + " (" + jo.getInt( "whiteRatingMean" ) + "±" + jo.getInt( "whiteRatingSD" ) + ")" );
			} else {
				System.out.println( getTime() + " playing against " + jo.getString( "blackName" ) + " (" + jo.getInt( "blackRatingMean" ) + "±" + jo.getInt( "blackRatingSD" ) + ")" );
			}
			long gameId = Long.parseLong( jo.getString( "gameId" ) );
			if( gameId != mGameId ) {
				mPreviousGameId = mGameId;
			}
			mGameId = gameId;
			mByomiTime = jo.getInt( "timePerPeriod" );
			System.out.println( getTime() + " giving bot " + (mByomiTime - mLag) / 1000 + " seconds per move." );
			mGTP.start( mByomiTime - mLag );
			mMoves.clear();
			JSONArray jam = jo.getJSONArray( "moves" );
			for( int i = 0 ; i < jam.length() ; ++i ) {
				Move move = Move.parseJson( jam.getString( i ) );
				mMoves.add( move );
				mGTP.sendMove( move );
			}
		}
		if( jo.getString( "type" ).equals( "gameupdate" ) ) {
			int moveNumber = jo.getInt( "moveNumber" );
			Move move = Move.parseJson( jo.getString( "move" ) );
			// System.out.println( "receiving move " + moveNumber + "=" + move );
			if( moveNumber == mMoves.size() - 1 && (move.equals( mMoves.lastElement() ) || move.equals( Move.sTimeLoss )) ) { // confirming own move..
				if( move.equals( Move.sTimeLoss ) ) {
					System.out.println( getTime() + " server reported that we lost on time!" );
				}
				return;
			}
			if( moveNumber != mMoves.size() ) {
				System.out.println( getTime() + " discarding move " + moveNumber + "=" + move + " because expecting " + mMoves.size() );
				return;
			}
			mMoves.add( move );
			try {
				mGTP.sendMove( move );
			} catch( Error pE ) {
				System.err.println( mMoves );
				throw pE;
			}
		}
		processUpdate();
	}

	private void processUpdate() throws IOException {
		if( isOver() ) {
			return;
		}
		if( (mMoves.size() % 2 == 0) == mBotIsBlack ) {
			if( isScoring() ) {
				if( mDead == null ) {
					return; // have to wait for server to send dead list..
				}
				if( mGTP.getDeadList().equals( mDead ) ) {
					mMoves.add( Move.sAccept );
				} else {
					mMoves.add( Move.sReject );
				}
				mGTP.sendMove( Move.sPass );
			} else {
				mMoves.add( mGTP.getMove( isCleanUp() ) );
			}
			// System.out.println( "sending move " + (mMoves.size() - 1) + "=" + mMoves.lastElement() + " at " + ((System.currentTimeMillis() / 1000) % 1000) );
			Rest.sendMove( mUserId , mMoves.lastElement() , mMoves.size() - 1 );
		}
	}

	private GTP mGTP;

	public WebSocketEngine( GTP pGTP , long pUserId , boolean pBlitz , boolean pFast , double pLagInSeconds ) {
		mGTP = pGTP;
		mUserId = pUserId;
		mFast = pFast;
		mBlitz = pBlitz;
		mLag = (int) (pLagInSeconds * 1000);
	}

	public static String sUrl = "http://goratingserver.appspot.com";

	public void start() throws IOException , ChannelException {
		if( mUserId == 0 ) {
			mUserId = Rest.createUserId();
			System.out.println( getTime() + " created new account with id " + mUserId );
		}
		ChannelAPI ca = new ChannelAPI( sUrl ) {

			@Override
			public void onOpen() {
				try {
					if( mBotName == null ) {
						Rest.initialize( mUserId );
					}
				} catch( IOException pE ) {
					throw new Error( pE );
				}
			}

			@Override
			public void onMessage( String pMessage ) {
				try {
					processMessage( pMessage );
				} catch( IOException pE ) {
					throw new Error( pE );
				}
			}

			@Override
			public void onError( Integer pErrorCode , String pDescription ) {
				System.out.println( getTime() + " error " + pErrorCode + " : " + pDescription );
			}

			@Override
			public void onClose() {}

			@Override
			protected String createChannel() throws IOException {
				if( mUnique != 0 ) {
					this.close();
				}
				mUnique = new Random().nextLong();
				String channelSecret = Rest.getChannelSecret( mUserId , mUnique );
				System.out.println( getTime() + " new connection to server." );
				return channelSecret;
			}
		};
		ca.open();
	}
}
