package com.amg.mywow.server.entities;

public class Character {
	
	private int id;
	private String name;
	private int x;
	private int y;
	
	public Character() {
	}
	
	public Character(String name, int x, int y) {
		this.name = name;
		this.x = x;
		this.y = y;
	}

	public int getId() {
		return id;
	}
	
	private void setId(int id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getX() {
		return x;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public int getY() {
		return y;
	}
	
	public void setY(int y) {
		this.y = y;
	}
}
