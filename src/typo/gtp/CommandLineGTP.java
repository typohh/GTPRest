package typo.gtp;

import java.io.IOException;

public class CommandLineGTP extends GTP {

	private static final int sLagMargin=3000;
	
	@Override
	public void start( long pTimeInMs ) throws IOException {
		if( mProcess != null && mProcess.isAlive() ) {
			mProcess.destroyForcibly();
		}
		mProcess = Runtime.getRuntime().exec( mLaunchGTP );
		setStreams( mProcess.getInputStream() , mProcess.getOutputStream() );
		try {
			Thread.sleep( 100 );
		} catch( InterruptedException pE ) {
			// TODO Auto-generated catch block
			pE.printStackTrace();
		}
		for( String command : mInitializeGTP ) {
			readFromGTP( command );
		}
		int seconds=(int) (( pTimeInMs - sLagMargin ) / 1000);
		System.out.println( "giving bot " + seconds + " seconds per move." );
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
