package com.amg.mywow.server.testutils;

import org.hibernate.Session;

import com.amg.mywow.server.HibernateUtil;
import com.amg.mywow.server.entities.Customer;
import com.amg.mywow.server.entities.Character;

public class DatabasePolulator {
	
	private static final int NUMBER_OF_CUSTOMERS = 1000;
	private static final int NUMBER_OF_CHARACTERS = NUMBER_OF_CUSTOMERS;
	
	public static void main(String args[]) {
		
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		session.beginTransaction();
		session.createQuery("delete from Customer").executeUpdate();
		session.createQuery("delete from Character").executeUpdate();
		session.getTransaction().commit();
		
		session = HibernateUtil.getSessionFactory().getCurrentSession();
		session.beginTransaction();
		for (int i = 0; i < NUMBER_OF_CUSTOMERS; i++) {
			session.save(new Customer("username", "userpassword"));
		}
		for (int i = 0; i < NUMBER_OF_CHARACTERS; i++) {
			session.save(new Character("Character" + (i + 1), i + 1, i + 1));
		}
		session.getTransaction().commit();
	}
}
