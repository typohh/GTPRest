package typo.gtp;

import java.io.IOException;

public class CommandLineGTP extends GTP {
	
	@Override
	public void start( long pTimeInMs ) throws IOException {
		if( mProcess != null && mProcess.isAlive() ) {
			readFromGTP( "quit" );
			try {
				Thread.sleep( 50 );
			} catch( InterruptedException pE ) {
				throw new Error( pE );
			}
			if( mProcess.isAlive() ) {
				mProcess.destroy();
			}
			try {
				Thread.sleep( 50 );
			} catch( InterruptedException pE ) {
				throw new Error( pE );
			}
			if( mProcess.isAlive() ) {
				mProcess.destroyForcibly();
			}
			try {
				Thread.sleep( 50 );
			} catch( InterruptedException pE ) {
				throw new Error( pE );
			}
		}
		mProcess = Runtime.getRuntime().exec( mLaunchGTP );
		setStreams( mProcess.getInputStream() , mProcess.getOutputStream() );
		mBlacksTurn=true;
		try {
			Thread.sleep( 100 );
		} catch( InterruptedException pE ) {
			throw new Error( pE );
		}
		for( String command : mInitializeGTP ) {
			readFromGTP( command );
		}
		int seconds=(int) (( pTimeInMs ) / 1000);
		readFromGTP( "time_settings 0 " + seconds + " 1" );
	}

	private Process mProcess=null;
	
	private String mLaunchGTP;
	private String[] mInitializeGTP;
	
	public CommandLineGTP( String pLaunchGTP , String[] pInitializeGTP ) {
		mLaunchGTP=pLaunchGTP;
		mInitializeGTP=pInitializeGTP;
	}
	
}
