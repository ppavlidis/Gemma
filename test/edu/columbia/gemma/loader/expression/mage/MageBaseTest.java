package edu.columbia.gemma.loader.expression.mage;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import edu.columbia.gemma.BaseDAOTestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MageBaseTest extends BaseDAOTestCase {

    /**
     * XSL-transform the mage document. This is only needed for testing. In production, this is done as part of the
     * parsing.
     * 
     * @param mlp
     * @param resourceName
     * @throws IOException
     */
    protected void zipXslSetup( MageMLParser mlp, String resourceName ) throws IOException {
        ZipInputStream istMageExamples = new ZipInputStream( MageMLParserTest.class.getResourceAsStream( resourceName ) );
        istMageExamples.getNextEntry();
        InputStream istXsl = MageMLParser.class.getResourceAsStream( "resource/MAGE-simplify.xsl" );
        assert istMageExamples != null;
        assert istXsl != null;
        mlp.createSimplifiedXml( istMageExamples, istXsl );
        istMageExamples.close();
    }

    /**
     * XSL-transform the mage document. This is only needed for testing. In production, this is done as part of the
     * parsing.
     * 
     * @param mlp
     * @param resourceName
     * @throws IOException
     */
    protected void xslSetup( MageMLParser mlp, String resourceName ) throws IOException {
        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( resourceName );
        InputStream istXsl = MageMLParser.class.getResourceAsStream( "resource/MAGE-simplify.xsl" );
        assert istMageExamples != null;
        assert istXsl != null;
        mlp.createSimplifiedXml( istMageExamples, istXsl );
        istMageExamples.close();
    }

}
