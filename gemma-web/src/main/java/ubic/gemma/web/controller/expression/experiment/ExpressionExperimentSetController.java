/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.controller.expression.experiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.security.SecurityService;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.expression.experiment.DatabaseBackedExpressionExperimentSetValueObject;
import ubic.gemma.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.expression.experiment.SessionBoundExpressionExperimentSetValueObject;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.persistence.SessionListManager;

/**
 * For fetching and manipulating ExpressionExperimentSets
 * 
 * @author paul
 * @version $Id$
 */
public class ExpressionExperimentSetController extends BaseFormController {
    private ExpressionExperimentReportService expressionExperimentReportService;
    private ExpressionExperimentService expressionExperimentService;
    private PersisterHelper persisterHelper;
    private SecurityService securityService;

    private TaxonService taxonService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService = null;

    @Autowired
    private SessionListManager sessionListManager;

    /**
     * @param entities
     * @return
     */
    public Collection<ExpressionExperimentSetValueObject> create(
            Collection<ExpressionExperimentSetValueObject> entities ) {
        Collection<ExpressionExperimentSetValueObject> result = new HashSet<ExpressionExperimentSetValueObject>();
        for ( ExpressionExperimentSetValueObject ees : entities ) {
            result.add( this.create( ees ) );
        }
        return result;
    }

    /**
     * AJAX adds the Expression Experiment group to the session
     * 
     * @param eeSetVos value object constructed on the client.
     * @return collection of added session groups (with updated reference.id etc)
     */
    public Collection<SessionBoundExpressionExperimentSetValueObject> addSessionGroups(
            Collection<SessionBoundExpressionExperimentSetValueObject> eeSetVos ) {

        Collection<SessionBoundExpressionExperimentSetValueObject> results = new HashSet<SessionBoundExpressionExperimentSetValueObject>();

        for ( SessionBoundExpressionExperimentSetValueObject eesvo : eeSetVos ) {

            results.add( sessionListManager.addExperimentSet( eesvo, true ) );
        }

        return results;
    }

    /**
     * AJAX adds the Expression Experiment group to the session
     * 
     * @param eeSetVos value object constructed on the client.
     * @return collection of added session groups (with updated reference.id etc)
     */
    public Collection<SessionBoundExpressionExperimentSetValueObject> addNonModificationBasedSessionBoundGroups(
            Collection<SessionBoundExpressionExperimentSetValueObject> eeSetVos ) {

        Collection<SessionBoundExpressionExperimentSetValueObject> results = new HashSet<SessionBoundExpressionExperimentSetValueObject>();

        for ( SessionBoundExpressionExperimentSetValueObject eesvo : eeSetVos ) {

            results.add( sessionListManager.addExperimentSet( eesvo, false ) );
        }

        return results;
    }

    /**
     * AJAX adds the experiment group to the session
     * 
     * @param geneSetVo value object constructed on the client.
     * @return the new gene groups
     */
    public Collection<ExpressionExperimentSetValueObject> addUserAndSessionGroups(
            Collection<ExpressionExperimentSetValueObject> entities ) {

        Collection<ExpressionExperimentSetValueObject> result = new HashSet<ExpressionExperimentSetValueObject>();

        Collection<SessionBoundExpressionExperimentSetValueObject> sessionResult = new HashSet<SessionBoundExpressionExperimentSetValueObject>();

        for ( ExpressionExperimentSetValueObject eesvo : entities ) {

            if ( eesvo instanceof SessionBoundExpressionExperimentSetValueObject ) {
                sessionResult.add( ( SessionBoundExpressionExperimentSetValueObject ) eesvo );
            } else {
                result.add( eesvo );
            }

        }

        result = create( result );

        result.addAll( addSessionGroups( sessionResult ) );

        return result;

    }

    /**
     * @param id
     * @return
     */
    public Collection<Long> getExperimentIdsInSet( Long id ) {
        ExpressionExperimentSet eeSet = expressionExperimentSetService.load( id ); // secure
        Collection<BioAssaySet> datasets = eeSet.getExperiments(); // Not secure.
        Collection<Long> eeids = new HashSet<Long>();
        for ( BioAssaySet ee : datasets ) {
            eeids.add( ee.getId() );
        }
        return eeids;
    }

    /**
     * @param id
     * @return
     */
    public Collection<ExpressionExperimentValueObject> getExperimentsInSet( Long id ) {
        Collection<Long> eeids = getExperimentIdsInSet( id );
        Collection<ExpressionExperimentValueObject> result = expressionExperimentService.loadValueObjects( eeids );
        expressionExperimentReportService.fillReportInformation( result );
        return result;
    }

    /**
     * AJAX
     * 
     * returns all available sets that have a taxon value (so not really all)
     * sets can have *any* number of experiments
     * 
     * @return all available sets that have a taxon value
     */
    public Collection<ExpressionExperimentSetValueObject> loadAll() {
        Collection<ExpressionExperimentSet> sets = expressionExperimentSetService.loadAllExperimentSetsWithTaxon(); // filtered
        // by
        // security.
        List<ExpressionExperimentSetValueObject> results = new ArrayList<ExpressionExperimentSetValueObject>();

        // should be a small number of items.
        for ( ExpressionExperimentSet set : sets ) {
            ExpressionExperimentSetValueObject vo = makeEESetValueObject( set );
            results.add( vo );
        }

        Collections.sort( results );

        return results;
    }

    /**
     * AJAX
     * 
     * @return all available sets from db and also session backed sets
     */
    public Collection<ExpressionExperimentSetValueObject> loadAllUserAndSessionGroups() {

        Collection<ExpressionExperimentSetValueObject> results = loadAll();

        Collection<SessionBoundExpressionExperimentSetValueObject> sessionResults = sessionListManager
                .getAllExperimentSets();

        results.addAll( sessionResults );

        return results;
    }

    /**
     * AJAX
     * 
     * @return all available session backed sets
     */
    public Collection<SessionBoundExpressionExperimentSetValueObject> loadAllSessionGroups() {

        Collection<SessionBoundExpressionExperimentSetValueObject> sessionResults = sessionListManager
                .getAllExperimentSets();

        return sessionResults;
    }

    /**
     * AJAX
     * 
     * @return all available sets that have a taxon value from db and also session backed
     *         sets
     */
    public Collection<ExpressionExperimentSetValueObject> loadAllUserOwnedAndSessionGroups() {

        Collection<ExpressionExperimentSetValueObject> valueObjects = new ArrayList<ExpressionExperimentSetValueObject>();

        Collection<ExpressionExperimentSet> results = expressionExperimentSetService.loadMySets(); // expressionExperimentSetService
        // is null?
        valueObjects.addAll( DatabaseBackedExpressionExperimentSetValueObject.makeValueObjects( results ) );
        valueObjects.addAll( sessionListManager.getAllExperimentSets() );

        return valueObjects;
    }

    /**
     * AJAX returns a JSON string encoding whether the current user owns the group and whether the group is db-backed
     * 
     * @param ref reference for a gene set
     * @return
     */
    public String canCurrentUserEditGroup( ExpressionExperimentSetValueObject eesvo ) {
        boolean userCanEditGroup = false;
        boolean groupIsDBBacked = false;
        if ( eesvo instanceof DatabaseBackedExpressionExperimentSetValueObject ) {
            groupIsDBBacked = true;
            try {
                userCanEditGroup = securityService.isEditable( expressionExperimentService.load( eesvo.getId() ) );
            } catch ( org.springframework.security.access.AccessDeniedException ade ) {
                return "{groupIsDBBacked:" + groupIsDBBacked + ",userCanEditGroup:" + false + "}";
            }
        }
        return "{groupIsDBBacked:" + groupIsDBBacked + ",userCanEditGroup:" + userCanEditGroup + "}";
    }

    /**
     * @param entities
     * @return the entities which were removed.
     */
    public Collection<DatabaseBackedExpressionExperimentSetValueObject> remove(
            Collection<DatabaseBackedExpressionExperimentSetValueObject> entities ) {
        for ( DatabaseBackedExpressionExperimentSetValueObject ees : entities ) {
            this.remove( ees );
        }
        return entities;
    }

    /**
     * AJAX Given a valid experiment group will remove it from the session.
     * 
     * @param groups
     */
    public Collection<SessionBoundExpressionExperimentSetValueObject> removeSessionGroups(
            Collection<SessionBoundExpressionExperimentSetValueObject> vos ) {
        for ( SessionBoundExpressionExperimentSetValueObject experimentSetValueObject : vos ) {
            sessionListManager.removeExperimentSet( experimentSetValueObject );
        }

        return vos;
    }

    /**
     * AJAX Given valid experiment groups will remove them from the session or the database appropriately.
     * 
     * @param groups
     */
    public Collection<ExpressionExperimentSetValueObject> removeUserAndSessionGroups(
            Collection<ExpressionExperimentSetValueObject> vos ) {
        Collection<ExpressionExperimentSetValueObject> removedSets = new HashSet<ExpressionExperimentSetValueObject>();
        Collection<DatabaseBackedExpressionExperimentSetValueObject> databaseCollection = new HashSet<DatabaseBackedExpressionExperimentSetValueObject>();
        Collection<SessionBoundExpressionExperimentSetValueObject> sessionCollection = new HashSet<SessionBoundExpressionExperimentSetValueObject>();

        for ( ExpressionExperimentSetValueObject experimentSetValueObject : vos ) {
            if ( experimentSetValueObject instanceof SessionBoundExpressionExperimentSetValueObject ) {
                sessionCollection.add( ( SessionBoundExpressionExperimentSetValueObject ) experimentSetValueObject );
            } else if ( experimentSetValueObject instanceof DatabaseBackedExpressionExperimentSetValueObject ) {
                databaseCollection.add( ( DatabaseBackedExpressionExperimentSetValueObject ) experimentSetValueObject );
            }
        }

        sessionCollection = removeSessionGroups( sessionCollection );
        databaseCollection = remove( databaseCollection );

        removedSets.addAll( sessionCollection );
        removedSets.addAll( databaseCollection );

        return removedSets;
    }

    public void setExpressionExperimentReportService(
            ExpressionExperimentReportService expressionExperimentReportService ) {
        this.expressionExperimentReportService = expressionExperimentReportService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setExpressionExperimentSetService( ExpressionExperimentSetService expressionExperimentSetService ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
    }

    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param securityService the securityService to set
     */
    public void setSecurityService( SecurityService securityService ) {
        this.securityService = securityService;
    }

    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    /**
     * @param entities
     * @return the entities which were updated (even if they weren't actually updated)
     */
    public Collection<DatabaseBackedExpressionExperimentSetValueObject> update(
            Collection<DatabaseBackedExpressionExperimentSetValueObject> entities ) {
        for ( DatabaseBackedExpressionExperimentSetValueObject ees : entities ) {
            update( ees );
        }
        return entities;
    }

    /**
     * AJAX Updates the given group (permission permitting) with the given list of memberIds Will not allow the same
     * experiment to be added to the set twice. Cannot update name or description, just members
     * 
     * @param groupId id of the gene set being updated
     * @param eeIds
     */
    public String updateMembers( Long groupId, Collection<Long> eeIds ) {

        String msg = null;

        ExpressionExperimentSet eeSet = expressionExperimentSetService.load( groupId );
        if ( eeSet == null ) {
            throw new IllegalArgumentException( "No experiment set with id=" + groupId + " could be loaded" );
        }
        Collection<ExpressionExperiment> updatedExperimentlist = new HashSet<ExpressionExperiment>();

        if ( !eeIds.isEmpty() ) {
            Collection<ExpressionExperiment> experiments = expressionExperimentService.loadMultiple( eeIds );

            if ( experiments.isEmpty() ) {
                throw new IllegalArgumentException( "None of the experiment ids were valid (out of " + eeIds.size()
                        + " provided)" );
            }
            if ( experiments.size() < eeIds.size() ) {
                throw new IllegalArgumentException( "Some of the experiment ids were invalid: only found "
                        + experiments.size() + " out of " + eeIds.size() + " provided)" );
            }

            assert experiments.size() == eeIds.size();
            boolean exists = false;
            for ( ExpressionExperiment experiment : experiments ) {

                for ( BioAssaySet bas : eeSet.getExperiments() ) {
                    if ( bas.getId().equals( experiment.getId() ) ) {
                        exists = true;
                        break;
                    }
                }

                if ( !exists ) {
                    eeSet.getExperiments().add( experiment );
                    updatedExperimentlist.add( experiment );
                } else {
                    updatedExperimentlist.add( experiment );
                }

                exists = false;
            }
        }

        eeSet.getExperiments().clear();
        eeSet.getExperiments().addAll( updatedExperimentlist );

        expressionExperimentSetService.update( eeSet );

        return msg;

    }

    /**
     * AJAX Updates the session group.
     * 
     * @param groups
     */
    public Collection<SessionBoundExpressionExperimentSetValueObject> updateSessionGroups(
            Collection<SessionBoundExpressionExperimentSetValueObject> vos ) {
        for ( SessionBoundExpressionExperimentSetValueObject expressionExperimentSetValueObject : vos ) {
            sessionListManager.updateExperimentSet( expressionExperimentSetValueObject );
        }
        return vos;
    }

    /**
     * AJAX Updates the session group and user database groups.
     * 
     * @param groups
     */
    public Collection<ExpressionExperimentSetValueObject> updateUserAndSessionGroups(
            Collection<ExpressionExperimentSetValueObject> vos ) {

        Collection<ExpressionExperimentSetValueObject> updatedSets = new HashSet<ExpressionExperimentSetValueObject>();
        Collection<DatabaseBackedExpressionExperimentSetValueObject> databaseCollection = new HashSet<DatabaseBackedExpressionExperimentSetValueObject>();
        Collection<SessionBoundExpressionExperimentSetValueObject> sessionCollection = new HashSet<SessionBoundExpressionExperimentSetValueObject>();

        for ( ExpressionExperimentSetValueObject experimentSetValueObject : vos ) {
            if ( experimentSetValueObject instanceof SessionBoundExpressionExperimentSetValueObject ) {
                sessionCollection.add( ( SessionBoundExpressionExperimentSetValueObject ) experimentSetValueObject );
            } else if ( experimentSetValueObject instanceof DatabaseBackedExpressionExperimentSetValueObject ) {
                databaseCollection.add( ( DatabaseBackedExpressionExperimentSetValueObject ) experimentSetValueObject );
            }
        }

        sessionCollection = updateSessionGroups( sessionCollection );
        databaseCollection = update( databaseCollection );

        updatedSets.addAll( sessionCollection );
        updatedSets.addAll( databaseCollection );

        return updatedSets;

    }

    /**
     * @param obj
     * @return
     */
    private ExpressionExperimentSetValueObject create( ExpressionExperimentSetValueObject obj ) {

        if ( obj.getId() != null && obj.getId() >= 0 ) {
            throw new IllegalArgumentException( "Should not provide an id for 'create': " + obj.getId() );
        }

        if ( StringUtils.isBlank( obj.getName() ) ) {
            throw new IllegalArgumentException( "You must provide a name" );
        }

        /*
         * Sanity check.
         */
        if ( expressionExperimentService.findByName( obj.getName() ) != null ) {
            throw new IllegalArgumentException( "Sorry, there is already a set with that name (" + obj.getName() + ")" );
        }

        ExpressionExperimentSet newSet = ExpressionExperimentSet.Factory.newInstance();
        newSet.setName( obj.getName() );
        newSet.setDescription( obj.getDescription() );

        Collection<? extends BioAssaySet> datasetsAnalyzed = expressionExperimentService.loadMultiple( obj
                .getExpressionExperimentIds() );

        newSet.getExperiments().addAll( datasetsAnalyzed );

        if ( obj.getTaxonId() != null )
            newSet.setTaxon( taxonService.load( obj.getTaxonId() ) );
        else {
            /*
             * Figure out the taxon from the experiments. FIXME: mustn't be heterogeneous.
             */

            Taxon taxon = expressionExperimentService.getTaxon( newSet.getExperiments().iterator().next().getId() );
            newSet.setTaxon( taxon );

        }

        if ( newSet.getTaxon() == null ) {
            throw new IllegalArgumentException( "No such taxon with id=" + obj.getTaxonId() );
        }

        ExpressionExperimentSet newEESet = ( ExpressionExperimentSet ) persisterHelper.persist( newSet );
        return this.makeEESetValueObject( newEESet );
    }

    /**
     * @param set
     * @return
     */
    private ExpressionExperimentSetValueObject makeEESetValueObject( ExpressionExperimentSet set ) {
        int size = set.getExperiments().size();
        assert size > 1; // should be due to the query.

        expressionExperimentSetService.thaw( set );

        ExpressionExperimentSetValueObject vo = new DatabaseBackedExpressionExperimentSetValueObject();
        vo.setName( set.getName() );
        vo.setId( set.getId() );
        Taxon taxon = set.getTaxon();
        if ( taxon == null ) {
            // happens in test databases that aren't properly populated.
            log.debug( "No taxon provided" );
        } else {
            vo.setTaxonId( taxon.getId() );
            vo.setTaxonName( taxon.getCommonName() ); // If I don't do this, won't be populated in the
            // downstream object. This is
            // basically a thaw.
        }

        vo.setCurrentUserHasWritePermission( securityService.isEditable( set ) );
        
        vo.setDescription( set.getDescription() == null ? "" : set.getDescription() );
        
        // if the set is used in an analysis, it should not be modifiable
        if ( expressionExperimentSetService.getAnalyses( set ).size() > 0 ) {
            vo.setModifiable( false );
        } else {
            vo.setModifiable( true );
        }

        /*
         * getExperimentsInSet to get the security-filtered ees.
         */
        vo.getExpressionExperimentIds().addAll(
                EntityUtils.getIds( expressionExperimentSetService.getExperimentsInSet( set.getId() ) ) );

        vo.setNumExperiments( size );
        return vo;
    }

    /**
     * Delete a EEset from the system.
     * 
     * @param obj
     * @return true if it was deleted.
     * @throw IllegalArgumentException it has analyses associated with it
     */
    private boolean remove( DatabaseBackedExpressionExperimentSetValueObject obj ) {
        Long id = obj.getId();
        if ( id == null || id < 0 ) {
            throw new IllegalArgumentException( "Cannot delete eeset with id=" + id );
        }
        ExpressionExperimentSet toDelete = expressionExperimentSetService.load( id );
        if ( toDelete == null ) {
            throw new IllegalArgumentException( "No such eeset id=" + id );
        }

        if ( expressionExperimentSetService.getAnalyses( toDelete ).size() > 0 ) {
            throw new IllegalArgumentException( "Sorry, can't delete this set, it is associated with active analyses." );
        }

        try {
            expressionExperimentSetService.delete( toDelete );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        return true;
    }

    //
    // /**
    // * Fill in information about analyses done on the experiments.
    // *
    // * @param result
    // */
    // private void populateAnalyses( Collection<Long> eeids, Collection<ExpressionExperimentValueObject> result ) {
    // Map<Long, DifferentialExpressionAnalysis> analysisMap = differentialExpressionAnalysisService
    // .findByInvestigationIds( eeids );
    // for ( ExpressionExperimentValueObject eevo : result ) {
    // if ( !analysisMap.containsKey( eevo.getId() ) ) {
    // continue;
    // }
    // eevo.getDifferentialExpressionAnalysisIds().add( analysisMap.get( eevo.getId() ).getId() );
    // }
    // }

    /**
     * @param obj
     */
    private void update( DatabaseBackedExpressionExperimentSetValueObject obj ) {

        if ( obj.getId() == null ) {
            throw new IllegalArgumentException( "Can only update an existing eeset (passed id=" + obj.getId() + ")" );
        }

        if ( StringUtils.isBlank( obj.getName() ) ) {
            throw new IllegalArgumentException( "You must provide a name" );
        }

        ExpressionExperimentSet toUpdate = expressionExperimentSetService.load( obj.getId() );

        if ( toUpdate == null ) {
            throw new IllegalArgumentException( "No such set with id = " + obj.getId() );
        }

        boolean needUpdate = updateExperimentsInSet( obj, toUpdate );

        /*
         * Allow updating of the name & description.
         */
        if ( !obj.getName().equals( toUpdate.getName() ) ) {
            toUpdate.setName( obj.getName() );
            needUpdate = true;
        }

        if ( !obj.getDescription().equals( toUpdate.getDescription() ) ) {
            toUpdate.setDescription( obj.getDescription() );
            needUpdate = true;
        }

        if ( needUpdate ) {
            expressionExperimentSetService.update( toUpdate );
            log.info( "Updated " + obj.getName() );
        } else {
            log.info( "No changes found for " + obj.getName() );
        }

    }

    /**
     * Check if the user has requested a change in membership; if so, check if the set can be safely modified.
     * 
     * @param obj
     * @param toUpdate
     * @return true if the set of experiments has changed, false otherwise.
     * @throws IllegalArgumentException if the set cannot be modified becasue it is is associated with an analysis
     *         object.
     */
    private boolean updateExperimentsInSet( ExpressionExperimentSetValueObject obj, ExpressionExperimentSet toUpdate ) {

        Collection<Long> idsInExistingSet = this.getExperimentIdsInSet( obj.getId() );
        boolean membersAreTheSame = idsInExistingSet.containsAll( obj.getExpressionExperimentIds() )
                && obj.getExpressionExperimentIds().containsAll( idsInExistingSet );
        /*
         * If there is an existing analysis, we have to disallow alteration of the set. Warn the user if they are
         * attempting to do this.
         */
        if ( !membersAreTheSame && expressionExperimentSetService.getAnalyses( toUpdate ).size() > 0 ) {
            throw new IllegalArgumentException(
                    "Sorry, you can't update members of this set, it is associated with active analyses." );
        }

        if ( membersAreTheSame ) {
            return false;
        }
        Collection<? extends BioAssaySet> datasetsAnalyzed = expressionExperimentService.loadMultiple( obj
                .getExpressionExperimentIds() );
        toUpdate.getExperiments().retainAll( datasetsAnalyzed );
        toUpdate.getExperiments().addAll( datasetsAnalyzed );
        /*
         * See bug 2038. Check that all the datasets have the matching taxons or have the same parent taxon. Currently
         * we go only one level up.
         */
        taxonService.thaw( toUpdate.getTaxon() );

        for ( BioAssaySet ee : toUpdate.getExperiments() ) {
            Taxon taxon = expressionExperimentService.getTaxon( ee.getId() );
            assert taxon != null;

            taxonService.thaw( taxon );

            if ( taxon.equals( toUpdate.getTaxon() ) ) continue;

            Taxon parentTaxon = taxon.getParentTaxon();
            if ( parentTaxon == null ) continue;

            taxonService.thaw( parentTaxon );
            if ( parentTaxon.equals( toUpdate.getTaxon() ) ) continue;

            throw new IllegalArgumentException( "You cannot add a " + taxon.getCommonName() + " dataset to a "
                    + toUpdate.getTaxon().getCommonName()
                    + " set. All datasets should have the same taxon or share parent taxon." );
        }
        return true;
    }

    /**
     * This is needed or you will have to specify a commandClass in the DispatcherServlet's context
     * 
     * @param request
     * @return Object
     * @throws Exception
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return request;
    }

}
