/*

 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.model.genome.gene;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;

import edu.emory.mathcs.backport.java.util.Collections;

import ubic.gemma.model.Reference;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.persistence.GemmaSessionBackedValueObject;


/**
 * Represents a Gene group gene set.
 * 
 * @author kelsey
 * @version $Id$
 */
public class GeneSetValueObject implements GemmaSessionBackedValueObject, Serializable {

    private static final long serialVersionUID = 6212231006289412683L;

    /**
     * @param genesets
     * @param includeOnesWithoutGenes if true, even gene sets that lack genes will be included.
     * @return
     */
    public static Collection<GeneSetValueObject> convert2ValueObjects( Collection<GeneSet> genesets,
            boolean includeOnesWithoutGenes ) {
        List<GeneSetValueObject> results = new ArrayList<GeneSetValueObject>();

        for ( GeneSet gs : genesets ) {
            if ( !includeOnesWithoutGenes && gs.getMembers().isEmpty() ) {
                continue;
            }

            if ( gs.getId() == null ) {
                /*
                 * GO terms, for example. We need a unique ID that also is different from IDs of things in the database.
                 * This isn't an entirely satisfactory implementation, it should be made bulletproof.
                 */
                gs.setId( Long.parseLong( RandomStringUtils.randomNumeric( 16 ) ) + 100000L );
            }

            results.add( new GeneSetValueObject( gs ) );
        }

        Collections.sort( results, new Comparator<GeneSetValueObject>() {
            @Override
            public int compare( GeneSetValueObject o1, GeneSetValueObject o2 ) {
                return -o1.getSize().compareTo( o2.getSize() );
            }
        } );
        return results;
    }

    private boolean currentUserHasWritePermission = false;
    private String description;
    private Collection<Long> geneIds = new HashSet<Long>();
    private Long id;
    private String name;
    private boolean publik;
    private boolean shared;
    private Integer size;
    private String taxonName;
    private long taxonId;
    private Reference reference;
    
    /**
     * default constructor to satisfy java bean contract
     */
    public GeneSetValueObject() {
        super();
    }

    /**
     * Constructor to build value object from GeneSet
     * 
     * @param gs
     */
    public GeneSetValueObject( GeneSet gs ) {
        Collection<Long> gids = new HashSet<Long>();
        for ( GeneSetMember gm : gs.getMembers() ) {
            gids.add( gm.getGene().getId() );
        }
        this.setName( gs.getName() );

        this.setId( gs.getId() );

        this.setDescription( gs.getDescription() );
        this.geneIds.addAll( gids );
        this.setSize( geneIds.size() );        

        if ( geneIds.size() > 0 ) {
            // FIXME: The assumption is that all genes in a gene set belong to the same taxon. I think this is enforced at UI level only.
            // This assumption is used in other parts of the code as well.
            this.setTaxonName ( gs.getMembers().iterator().next().getGene().getTaxon().getCommonName() );
            this.setTaxonId ( gs.getMembers().iterator().next().getGene().getTaxon().getId() );
        }   
        
        this.reference = new Reference( gs.getId(), Reference.DATABASE_BACKED_GROUP );
    }
   
    public String getTaxonName() {
        return this.taxonName;
    }    

    public void setTaxonName( String taxonName ) {
        this.taxonName = taxonName;        
    }

    public long getTaxonId() {
        return this.taxonId;
    }    

    public void setTaxonId( long taxonId ) {
        this.taxonId = taxonId;        
    }
    
    /**
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return
     */
    public Collection<Long> getGeneIds() {
        return geneIds;
    }

    /**
     * @return
     */
    public Long getId() {
        return id;
    }


    /**
     * @return
     */
    public Reference getReference() {
        return reference;
    }


    public boolean isSessionBound() {
        return getReference().isSessionBound();
    }
        

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * returns the number of members in the group
     * 
     * @return
     */
    public Integer getSize() {
        return this.size;
    }

    /**
     * @return the currentUserHasWritePermission
     */
    public boolean isCurrentUserHasWritePermission() {
        return currentUserHasWritePermission;
    }

    public boolean isPublik() {
        return this.publik;
    }

    public boolean isShared() {
        return this.shared;
    }

    /**
     * @param currentUserHasWritePermission the currentUserHasWritePermission to set
     */
    public void setCurrentUserHasWritePermission( boolean currentUserHasWritePermission ) {
        this.currentUserHasWritePermission = currentUserHasWritePermission;
    }

    /**
     * @param description
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * @param geneMembers
     */
    public void setGeneIds( Collection<Long> geneMembers ) {
        this.geneIds = geneMembers;
    }

    /**
     * @param id
     */
    public void setId( Long id ) {
        this.id = id;
    }
    
    /**
     * @param reference
     */
    public void setReference( Reference reference ) {
        this.reference = reference;
    }

    /**
     * @param name
     */
    public void setName( String name ) {
        this.name = name;
    }

    public void setPublik( boolean isPublic ) {
        this.publik = isPublic;
    }

    public void setShared( boolean isShared ) {
        this.shared = isShared;
    }

    /**
     * @param size
     */
    public void setSize( Integer size ) {
        this.size = size;
    }

}
