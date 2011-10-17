package com.amg.mywow.common;

public class MovementAction {
	
	private int characterId;
	private Position[] movements;
	
	public MovementAction() {
	}
	
	public int getCharacterId() {
		return characterId;
	}
	
	private void setCharacterId(int characterId) {
		this.characterId = characterId;
	}
	
	public Position[] getMovements() {
		return movements;
	}
	
	public void setMovements(Position[] movements) {
		this.movements = movements;
	}
}
