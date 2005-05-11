package edu.columbia.gemma;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import junit.framework.TestCase;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Base class for running DAO tests. Based on code from Appfuse.
 * 
 * @author mraible
 * @author pavlidis
 * @version $Id$
 */
public class BaseDAOTestCase extends TestCase {
    protected final Log log = LogFactory.getLog( getClass() );
    protected final static BeanFactory ctx;
    protected ResourceBundle rb;

    // This static block ensures that Spring's BeanFactory is only loaded
    // once for all tests
    static {
        ResourceBundle db = ResourceBundle.getBundle( "test" );
        String daoType = db.getString( "dao.type" );
        String servletContext = db.getString("servlet.name.0");

        // CAREFUL, these paths are dependent on the classpath for the test.
        String[] paths = { "applicationContext-dataSource.xml", "applicationContext-" + daoType + ".xml", servletContext + "-servlet.xml" };
        ctx = new ClassPathXmlApplicationContext( paths );
    }

    public BaseDAOTestCase() {
        // Since a ResourceBundle is not required for each class, just
        // do a simple check to see if one exists
        String className = this.getClass().getName();

        try {
            rb = ResourceBundle.getBundle( className ); // will look for <className>.properties
        } catch ( MissingResourceException mre ) {
            // log.warn("No resource bundle found for: " + className);
        }
    }

    /**
     * Utility method to populate a javabean-style object with values from a Properties file
     * 
     * @param obj
     * @return
     * @throws Exception
     */
    protected Object populate( Object obj ) throws Exception {
        // loop through all the beans methods and set its properties from
        // its .properties file
        Map map = new HashMap();

        for ( Enumeration keys = rb.getKeys(); keys.hasMoreElements(); ) {
            String key = ( String ) keys.nextElement();
            map.put( key, rb.getString( key ) );
        }

        BeanUtils.copyProperties( obj, map );

        return obj;
    }
}