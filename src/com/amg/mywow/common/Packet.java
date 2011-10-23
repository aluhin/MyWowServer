package com.amg.mywow.common;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Packet implements Externalizable{

	static public final short ACTION_DISCONNECT = -1;
	static public final short ACTION_MOVEMENT = 0;
	static public final short ACTION_MOVEMENT_CONFIRMATION = 1;
	static public final short ACTION_ATTACK = 2;
	static public final short ACTION_ATTACK_CONFIRMATION = 3;
	static public final short ACTION_LOGIN = 4;
	static public final short ACTION_CHAR_LOCATION = 5;
	
	static public final byte RESPONSE_GENERIC_OK = 0;
	static public final byte RESPONSE_GENERIC_NOTOK = 1;
	
	static public final byte RESPONSE_LOGIN_OK = 0;
	static public final byte RESPONSE_LOGIN_INCORRECT = 1;
	static public final byte RESPONSE_LOGIN_ALREADY = 2;
	
	private int characterId;
	private short action;
	private Object data;
	
	public Packet() {
	}
	
	public Packet(short action, Object data) {
		this(action, data, -1);
	}
	
	public Packet(short action, Object data, int characterId) {
		this.characterId = characterId;
		this.action = action;
		this.data = data;
	}
	
	@Override
	public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
		characterId = oi.readInt();
		action = oi.readShort();
		data = oi.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput oo) throws IOException {
		oo.writeInt(characterId);
		oo.writeShort(action);
		oo.writeObject(data);
	}

	public int getCharacterId() {
		return characterId;
	}
	
	public void setCharacterId(int characterId) {
		this.characterId = characterId;
	}

	public short getAction() {
		return action;
	}

	public void setAction(short action) {
		this.action = action;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
	
	public String toString() {
		return String.format("CharId: %d Action: %d Data: %s", characterId, action, data);
	}
}
