package com.amg.mywow.server.entities;

public class Customer {
	
	private int id;
	private String name;
	private String password;
	
	public Customer() {
	}
	
	public Customer(String name, String password) {
		this.name = name;
		this.password = password;
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
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
