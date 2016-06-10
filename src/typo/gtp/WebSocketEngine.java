package typo.gtp;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import typo.gtp.ChannelAPI.ChannelException;

public class WebSocketEngine {

	private boolean mBlitz;
	private boolean mFast;
	private long mGameId;
	private String mBotName;
	private boolean mBotIsBlack;
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

	private void processMessage( String pMessage ) throws IOException {
//		System.out.println( "received : \n" + pMessage );
		JSONObject jo = new JSONObject( pMessage );
		long messageId=Long.parseLong( jo.getString( "messageId" ) );
		Rest.ack( mUserId , messageId );
		if( jo.getString( "type" ).equals( "userdata" ) ) {
			if( mGameId != 0 ) {
				System.out.println( "http://goratingserver.appspot.com/sgf/" + mGameId + ".sgf" );
			}
			mBotName = jo.getString( "name" );
			System.out.println( "rating is " + jo.getInt( "ratingMean" ) + "±" + jo.getInt( "ratingSD" ) + " after " + jo.getInt( "numberOfGames" ) + " games." );
			Rest.registerForPairing( mUserId , mBlitz , mFast );
			return;
		}
		if( jo.getString( "type" ).equals( "ping" ) ) {
			return;
		}
		if( jo.getString( "type" ).equals( "disconnect" ) ) {
			if( mUnique != Long.parseLong( jo.getString( "active" ))) {
				System.out.println( "this account has been logged into from elsewhere." );
			}
			return; // TODO : should check that active matches our unique..
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
			mMoves.clear();
			if( jo.getBoolean( "hasDead" ) ) {
				JSONArray jad = jo.getJSONArray( "dead" );
				mDead = new HashSet<>();
				for( int i = 0 ; i < jad.length() ; ++i ) {
					mDead.add( Move.parseJson( jad.getString( i ) ) );
				}
			}
			JSONArray jam = jo.getJSONArray( "moves" );
			mBotIsBlack = jo.getString( "blackName" ).equals( mBotName );
			if( mBotIsBlack ) {
				System.out.println( "playing against " + jo.getString( "whiteName" ) + " (" + jo.getInt( "whiteRatingMean" ) + "±" + jo.getInt( "whiteRatingSD" ) + ")" );
			} else {
				System.out.println( "playing against " + jo.getString( "blackName" ) + " (" + jo.getInt( "blackRatingMean" ) + "±" + jo.getInt( "blackRatingSD" ) + ")" );
			}
			mGameId = Long.parseLong( jo.getString( "gameId" ) );
			mByomiTime = jo.getInt( "timePerPeriod" );
			mGTP.start( mByomiTime );
			for( int i = 0 ; i < jam.length() ; ++i ) {
				Move move = Move.parseJson( jam.getString( i ) );
				mMoves.add( move );
				mGTP.sendMove( move );
			}
		}
		if( jo.getString( "type" ).equals( "gameupdate" ) ) {
			int moveNumber = jo.getInt( "moveNumber" );
			Move move = Move.parseJson( jo.getString( "move" ) );
//			System.out.println( "receiving move " + moveNumber + "=" + move );
			if( moveNumber == mMoves.size() - 1 && move.equals( mMoves.lastElement() ) ) { // confirming own move..
				return;
			}
			if( moveNumber < mMoves.size() ) {
				System.out.println( "discarding move number " + moveNumber + " because expected " + mMoves.size() );
				return;
			}
			if( moveNumber != mMoves.size() ) {
				System.out.println( "received future move, resyncing.." );
				Rest.resync( mUserId );
				return;
			}
			mGTP.sendMove( move );
			mMoves.add( move );
		}
		processUpdate();
	}

	private void processUpdate() throws IOException {
		if( isOver() ) {
			return; // lets just wait for the new user info..
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
			} else {
				mMoves.add( mGTP.getMove( isCleanUp() ) );
			}
//			System.out.println( "sending move " + ( mMoves.size() - 1 ) + "=" + mMoves.lastElement() + " at " + ( ( System.currentTimeMillis() / 1000 ) % 1000 )  );
			Rest.sendMove( mUserId , mMoves.lastElement() , mMoves.size() - 1 );
		}
	}

	private GTP mGTP;

	public WebSocketEngine( GTP pGTP , long pUserId , boolean pBlitz , boolean pFast ) {
		mGTP = pGTP;
		mUserId = pUserId;
		mFast = pFast;
		mBlitz = pBlitz;
	}

	public static String sUrl = "http://goratingserver.appspot.com";

	public void start() throws IOException , ChannelException {
		if( mUserId == 0 ) {
			mUserId=Rest.createUserId();
			System.out.println( "created new account with id " + mUserId );
		}

		ChannelAPI ca=new ChannelAPI( sUrl ) {

			@Override
			public void onOpen() {
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
				System.out.println( "error " + pErrorCode + " : " + pDescription );
			}

			@Override
			public void onClose() {
			}
			
			@Override
			protected String createChannel() throws IOException {
				mUnique=new Random().nextLong();
				String channelSecret = Rest.getChannelSecret( mUserId , mUnique );
				return channelSecret;
			}
			
		};
		ca.open();
	
	}
}
