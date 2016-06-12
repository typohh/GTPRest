package typo.gtp;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import typo.gtp.ChannelAPI.ChannelException;

public class MimicKGS {

	public static void main( String[] pArg ) throws IOException, ChannelException {
		Properties prop = new Properties();
		String fileName = "properties.ini";
		if( pArg.length > 0 ) {
			fileName = pArg[0];
		}
		FileInputStream input = new FileInputStream( fileName );
		prop.load( input );
		input.close();

		String engine=prop.getProperty( "engine" );
		long id=Long.parseLong( prop.getProperty( "id" , "0" ).trim() );
		boolean blitz=Boolean.parseBoolean( prop.getProperty( "blitz" , "true" ).trim() );
		boolean fast=Boolean.parseBoolean( prop.getProperty( "fast" , "true" ).trim() );
		double lag=Double.parseDouble( prop.getProperty( "lag" , "3" ).trim() );
		
		CommandLineGTP gtp = new CommandLineGTP( engine , new String[] {
				"boardsize 19", "komi 7.5"
		} );
		WebSocketEngine wse = new WebSocketEngine( gtp , id , blitz , fast , lag );
		wse.start();
	}
	
}
