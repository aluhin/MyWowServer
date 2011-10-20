package com.amg.mywow.common;

import java.io.Serializable;

public class MovementAction implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private Position[] positions;
	
	public MovementAction() {
	}
	
	public MovementAction (Position[] positions) {
		this.positions = positions;
	}
	
	public Position[] getPositions() {
		return positions;
	}
	
	public void setPositions(Position[] positions) {
		this.positions = positions;
	}
	
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder("Coords: ");
		for (Position pos : positions) {
			stringBuilder.append(pos);
			stringBuilder.append(" ");
		}
		return stringBuilder.toString();
	}
}
