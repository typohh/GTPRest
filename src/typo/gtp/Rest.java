package typo.gtp;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Rest {

	public static String mServerUrl = "http://goratingserver.appspot.com/rankingserver/restful";

	private static String sendRequest( String pUrl ) throws IOException {
		for( int i=0 ; true ; ++i ) {
			URL url = new URL( pUrl );
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod( "GET" );
			if( conn.getResponseCode() != 200 ) {
				if( conn.getResponseCode() == 400 ) {
					throw new Error( "you are using an old protocol version, please upgrade" );
				}
				try {
					Thread.sleep( (int)Math.pow( 2 , i ) * 100l );
				} catch( InterruptedException pE ) {
					throw new Error( pE );
				}
				System.err.println( pUrl + " : " + conn.getResponseCode() );
				continue;
			}
			InputStream input = conn.getInputStream();
			StringBuffer result=new StringBuffer();
			for( int c=input.read() ; c != -1 ; c=input.read() ) {
				result.append( (char)c );
			}
			return result.toString();
		}
	}

	public static void sendMove( long pUserId , Move pMove , int pMoveNumber ) throws IOException {
		sendRequest( mServerUrl + "?c=sm&u=" + pUserId + "&n=" + pMoveNumber + "&m=" + pMove.toJson() );
	}

	public static void registerForPairing( long pUserId , boolean pBlitz , boolean pFast ) throws IOException {
		sendRequest( mServerUrl + "?c=rfp&u=" + pUserId + "&b=" + pBlitz + "&f=" + pFast );
	}

	public static void ack( long pUserId , long pMessageId ) throws IOException {
		sendRequest( mServerUrl + "?c=a&u=" + pUserId + "&m=" + pMessageId );
	}

	public static void initialize( long pUserId ) throws IOException {
		sendRequest( mServerUrl + "?c=i&u=" + pUserId );
	}

	public static String getChannelSecret( long pUserId , long pUnique ) throws IOException {
		return sendRequest( mServerUrl + "?c=gcs&u=" + pUserId + "&a=" + pUnique + "&v=100" );
	}

	public static long createUserId() throws IOException {
		return Long.parseLong( sendRequest( mServerUrl + "?c=cui" ) );
	}

}
