package typo.gtp;

import java.io.IOException;

public class PachiGTP extends GTP {

	private Process mProcess = null;

	public void shutDown() throws IOException {
		if( mProcess != null ) {
			if( mProcess.isAlive() ) {
				try {
					readFromGTP( "quit" );
				} catch( Error pE ) {
				}
			}
			try {
				Thread.sleep( 50 );
			} catch( InterruptedException pE ) {
				throw new Error( pE );
			}
			if( mProcess.isAlive() ) {
				mProcess.destroy();
				try {
					Thread.sleep( 50 );
				} catch( InterruptedException pE ) {
					throw new Error( pE );
				}
				if( mProcess.isAlive() ) {
					mProcess.destroyForcibly();
					try {
						Thread.sleep( 50 );
					} catch( InterruptedException pE ) {
						throw new Error( pE );
					}
				}
			}
			mProcess = null;
		}
	}

	@Override
	public void start( long pTime ) throws IOException {
		shutDown();
		mProcess = Runtime.getRuntime().exec( "pachi-11.00-win32\\pachi.exe -f book.dat -r Chinese -t " + ( pTime / 1000 ) + " -d 0 threads=10,maximize_score" );
		setStreams( mProcess.getInputStream() , mProcess.getOutputStream() );
		readFromGTP( "boardsize 19" );
		readFromGTP( "komi 7.5" );
	}

	public static void main( String[] pArg ) throws IOException , InterruptedException {
		PachiGTP gtp = new PachiGTP();
		Rest rest=new Rest( 0 , true , true );
		Mediator mMediator=new Mediator( gtp , rest );
		while( true ) {
			try {
				mMediator.run();
			} catch( IOException pE ) {
				pE.printStackTrace();
			} catch( Error pE ) {
				pE.printStackTrace();
			}
		}
	}

}
