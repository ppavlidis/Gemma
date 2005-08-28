package edu.columbia.gemma.externalDb;

import java.sql.SQLException;
import java.util.Collection;

import org.hibernate.HibernateException;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public interface GoldenPathHumanDao {

    public boolean connectToDatabase() throws HibernateException, SQLException;

    public Collection retrieveFromDatabase() throws HibernateException, SQLException;
}