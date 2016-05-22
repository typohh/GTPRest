package typo.gtp;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class Mediator {
	
	private GTP mGTP;
	private Rest mRest;
	
	boolean cleanUp=false;
	Move oldMove=null;
	Move oldOldMove=null;

	public boolean matchDead() throws IOException {
		Set<Move> deadGTP = mGTP.getDeadList();
		Set<Move> deadRest = mRest.getDeadList();
		if( deadRest.size() == deadGTP.size() ) {
			for( Move m : deadRest ) {
				if( !deadGTP.contains( m )) {
					return false;
				}
			}
		}
		return true;
	}

	public void run() throws IOException , InterruptedException {
		cleanUp=false;
		oldMove=null;
		oldOldMove=null;
		
		List<Move> moves=mRest.startGame();
		mGTP.start( mRest.getByomiTime() - 2000 );
		for( Move move : moves ) {
			mGTP.sendMove( move );
		}
		while( true ) {
			Move move;
			if( mRest.isBlacksTurn() == mRest.isBotBlack() ) {
				if( ( oldMove == Move.sPass || oldMove == Move.sAccept ) && oldOldMove == Move.sPass ) {
					if( matchDead() ) {
						move=Move.sAccept;
					} else {
						move=Move.sReject;
					}
					mGTP.sendMove( move );
					mRest.sendMove( move );
				} else {
					move=mGTP.getMove( cleanUp );
					mRest.sendMove( move );
				}
			} else {
				move=mRest.getMove();
				mGTP.sendMove( move );
			}
			if( move == Move.sReject  ) {
				cleanUp=true;
			}
			if( move == Move.sPass && oldMove == Move.sPass && cleanUp ) {
				return;
			}
			if( move == Move.sAccept && oldMove == Move.sAccept ) {
				return;
			}
			if( move == Move.sResign || move == Move.sTimeLoss ) {
				return;
			}
			oldOldMove=oldMove;
			oldMove=move;
		}
	}

	public Mediator( GTP pGTP , Rest pRest ) {
		mRest=pRest;
		mGTP=pGTP;
	}
	
}
