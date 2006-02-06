package edu.columbia.gemma.web.flow.bibref;

import java.util.HashMap;
import java.util.Map;

import org.springframework.webflow.Event;

import edu.columbia.gemma.BaseFlowTestCase;

/**
 * Tests the pubMedSearch-flow.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2006 University of British Columbia
 * 
 * @author keshav
 * @version $Id$
 */
public class SearchPubMedFlowTests extends BaseFlowTestCase {
    /**
     * 
     *
     */
    public SearchPubMedFlowTests() {
        setDependencyCheck( false );
    }

    /**
     * Test criteria.view with an invalid argument. Expect to return to the criteria.view.
     */
    // should add validator object. Fail on validation should take me back to the criteria.view.
    // public void testCriteriaView_Submit_Error() {
    // startFlow();
    // ViewDescriptor view = signalEvent( new SimpleEvent( this, "submit", null ) );
    // assertCurrentStateEquals( "criteria.view" );
    // }
    /**
     * Test criteria.view with a valid argument. Expect the results.view to be entered. Expect 1 bibliographic reference
     * to be returned from 1 pubMed Id.
     */
    // This test disabled as we aren't using flows.
    // public void testCriteriaView_Submit_Success() {
    // startFlow();
    // Map<String, String> properties = new HashMap<String, String>();
    // properties.put( "pubMedId", "15173114" );
    // properties.put( "_eventId", "pubMedSearch" );
    // signalEvent( new Event( this, "searchPubMed", properties ) );
    // assertCurrentStateEquals( "results.view" );
    // // asserts().assertCollectionAttributeSize( view, "bibliographicReferences", 1 );
    // }
    /**
     * Test the start of the flow
     */
    public void testStartFlow() {
        startFlow();
        assertCurrentStateEquals( "criteria.view" );
    }

    protected String flowId() {
        return "pubMed.Search";
    }

}