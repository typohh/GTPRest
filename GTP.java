package typo.gtp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;

public abstract class GTP {

	protected InputStreamReader mReader;
	protected OutputStreamWriter mWriter;
	protected boolean mBlacksTurn = true;

	protected String readFromGTP( String pCommand ) throws IOException {
		mWriter.write( pCommand + "\n" );
		mWriter.flush();
		StringBuffer text = new StringBuffer();
		while( true ) {
			StringBuffer line = new StringBuffer();
			for( int c = mReader.read() ; c != -1 && c != '\n' ; c = mReader.read() ) {
				line.append( (char) c );
			}
			String string = line.toString().trim();
			if( string.length() == 0 ) {
				if( text.charAt( 0 ) == '=' ) {
					return text.substring( 1 , text.length() - 1 ).trim();
				} else {
					throw new Error( pCommand + " -> " + text.toString() );
				}
			}
			text.append( line + "\n" );
		}
	}

	protected String getColor( boolean pBlackNotWhite ) {
		if( pBlackNotWhite ) {
			return "black";
		} else {
			return "white";
		}
	}

	public void sendMove( Move pMove ) throws IOException {
		readFromGTP( "play " + getColor( mBlacksTurn ) + " " + pMove.toGtp() );
		mBlacksTurn = !mBlacksTurn;
	}

	public Move getMove( boolean pCleanUp ) throws IOException {
		String answer;
		if( pCleanUp ) {
			answer = readFromGTP( "kgs-genmove_cleanup " + getColor( mBlacksTurn ) );
		} else {
			answer = readFromGTP( "genmove " + getColor( mBlacksTurn ) );
		}
		mBlacksTurn = !mBlacksTurn;
		return Move.parseGTP( answer );
	}

	public Set<Move> getDeadList() throws IOException {
		HashSet<Move> moves = new HashSet<>();
		String answer = readFromGTP( "final_status_list dead" );
		if( !answer.trim().isEmpty() ) {
			for( String s : answer.split( "[\\n\\s]+" ) ) {
				moves.add( Move.parseGTP( s.trim() ) );
			}
		}
		return moves;
	}

	public abstract void start( long pTimeInMs ) throws IOException;

	protected void setStreams( InputStream pInput , OutputStream pOutput ) {
		mReader = new InputStreamReader( pInput );
		mWriter = new OutputStreamWriter( pOutput );
	}
}
