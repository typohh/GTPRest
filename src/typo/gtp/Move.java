package typo.gtp;

import java.io.Serializable;

public class Move implements Serializable {

	private Move( int pX , int pY ) {
		mX=pX;
		mY=pY;
	}
	
	private int mX;
	private int mY;
	
	@Override
	public String toString() {
		return toJson();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + mX;
		result = prime * result + mY;
		return result;
	}

	@Override
	public boolean equals( Object obj ) {
		if( this == obj )
			return true;
		if( obj == null )
			return false;
		if( getClass() != obj.getClass() )
			return false;
		Move other = (Move) obj;
		if( mX != other.mX )
			return false;
		if( mY != other.mY )
			return false;
		return true;
	}

	public static final Move sPass=new Move( -1 , -1 );
	public static final Move sResign=new Move( -2 , -1 );
	public static final Move sTimeLoss=new Move( -3 , -1 );
	public static final Move sAccept=new Move( -4 , -1 );
	public static final Move sReject=new Move( -5 , -1 );
	
	public String toJson() {
		if( mY == -1 ) {
			if( mX == -1 ) {
				return "Pass";
			} else if( mX == -2 ) {
				return "Resign";
			} else if( mX == -3 ) {
				return "TimeLoss"; 
			} else if( mX == -4 ) {
				return "Accept";
			} else if( mX == -5 ) {
				return "Reject";
			}
		}
		return (char)( 'a'+ mX ) + "" + (char)( 'a' + mY );
	}
	
	public String toGtp() {
		if( mY == -1 ) {
			return "Pass";
		}
		char x = (char) ('A' + mX);
		if( x >= 'I' ) {
			++x;
		}
		return x + "" + (int)(1 + mY);
		
	}
	
	public static Move parseJson( String pMove ) {
		if( pMove.equals( "Pass" )) {
			return sPass;
		}
		if( pMove.equals( "Resign" )) {
			return sResign;
		}
		if( pMove.equals( "TimeLoss" )) {
			return sTimeLoss;
		}
		if( pMove.equals( "Accept" )) {
			return sAccept;
		}
		if( pMove.equals( "Reject" )) {
			return sReject;
		}
		if( pMove.length() != 2 ) {
			throw new Error( "unidentified : " + pMove );
		}
		return new Move( pMove.charAt( 0 ) - 'a' , pMove.charAt( 1 ) - 'a' );
	}
	
	public static Move parseGTP( String pMove ) {
		if( pMove.equalsIgnoreCase( "Pass" )) {
			return sPass;
		}
		if( pMove.equalsIgnoreCase( "Resign" )) {
			return sResign;
		}
		int x = Character.toLowerCase( pMove.charAt( 0 ) ) - 'a';
		int y = Integer.parseInt( pMove.substring( 1 ) ) - 1;
		if( x >= ('j' - 'a') ) {
			--x;
		}
		return new Move( x , y );
	}
	
}
