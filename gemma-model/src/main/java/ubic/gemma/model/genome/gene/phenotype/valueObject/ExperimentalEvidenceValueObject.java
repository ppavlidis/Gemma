/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ubic.gemma.model.association.phenotype.ExperimentalEvidence;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.common.description.VocabCharacteristicImpl;

public class ExperimentalEvidenceValueObject extends EvidenceValueObject {

    private Collection<CharacteristicValueObject> experimentCharacteristics = new TreeSet<CharacteristicValueObject>();
    private Set<CitationValueObject> relevantPublicationsCitationValueObjects = new HashSet<CitationValueObject>();
    private CitationValueObject primaryPublicationCitationValueObject = null;

    public ExperimentalEvidenceValueObject() {
        super();
    }

    /** Entity to Value Object */
    public ExperimentalEvidenceValueObject( ExperimentalEvidence experimentalEvidence ) {
        super( experimentalEvidence );

        this.primaryPublicationCitationValueObject = BibliographicReferenceValueObject
                .constructCitation( experimentalEvidence.getExperiment().getPrimaryPublication() );

        this.relevantPublicationsCitationValueObjects.addAll( BibliographicReferenceValueObject
                .constructCitations( experimentalEvidence.getExperiment().getOtherRelevantPublications() ) );

        Collection<Characteristic> collectionCharacteristics = experimentalEvidence.getExperiment()
                .getCharacteristics();

        if ( collectionCharacteristics != null ) {
            for ( Characteristic c : collectionCharacteristics ) {
                if ( c instanceof VocabCharacteristicImpl ) {
                    VocabCharacteristicImpl voCha = ( VocabCharacteristicImpl ) c;

                    String valueUri = null;

                    if ( voCha.getValueUri() != null && !voCha.getValueUri().equals( "" ) ) {
                        valueUri = voCha.getValueUri();
                    }

                    CharacteristicValueObject chaValueObject = new CharacteristicValueObject( voCha.getValue(),
                            voCha.getCategory(), valueUri, voCha.getCategoryUri() );

                    chaValueObject.setId( voCha.getId() );

                    this.experimentCharacteristics.add( chaValueObject );
                } else {
                    this.experimentCharacteristics.add( new CharacteristicValueObject( c.getValue(), c.getCategory() ) );
                }
            }
        }
    }

    public ExperimentalEvidenceValueObject( Integer geneNCBI, SortedSet<CharacteristicValueObject> phenotypes,
            String description, String evidenceCode, boolean isNegativeEvidence,
            EvidenceSourceValueObject evidenceSource, String primaryPublication, Set<String> relevantPublication,
            Set<CharacteristicValueObject> experimentCharacteristics ) {
        super( geneNCBI, phenotypes, description, evidenceCode, isNegativeEvidence, evidenceSource );

        if ( primaryPublication != null ) {
            this.primaryPublicationCitationValueObject = new CitationValueObject();
            this.primaryPublicationCitationValueObject.setPubmedAccession( primaryPublication );
        }

        for ( String relevantPubMedID : relevantPublication ) {
            CitationValueObject relevantPublicationValueObject = new CitationValueObject();
            relevantPublicationValueObject.setPubmedAccession( relevantPubMedID );
            this.relevantPublicationsCitationValueObjects.add( relevantPublicationValueObject );
        }

        this.experimentCharacteristics = experimentCharacteristics;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( !super.equals( obj ) ) return false;
        if ( getClass() != obj.getClass() ) return false;
        ExperimentalEvidenceValueObject other = ( ExperimentalEvidenceValueObject ) obj;
        if ( this.primaryPublicationCitationValueObject == null ) {
            if ( other.primaryPublicationCitationValueObject != null ) return false;
        } else if ( !this.primaryPublicationCitationValueObject.equals( other.primaryPublicationCitationValueObject ) )
            return false;
        if ( this.experimentCharacteristics.size() != other.experimentCharacteristics.size() ) {
            return false;
        }
        for ( CharacteristicValueObject characteristicValueObject : this.experimentCharacteristics ) {
            if ( !other.experimentCharacteristics.contains( characteristicValueObject ) ) {
                return false;
            }
        }
        if ( this.relevantPublicationsCitationValueObjects.size() != other.relevantPublicationsCitationValueObjects
                .size() ) {
            return false;
        }
        for ( CitationValueObject relevantPublicationsCitationValueObject : this.relevantPublicationsCitationValueObjects ) {
            if ( !other.relevantPublicationsCitationValueObjects.contains( relevantPublicationsCitationValueObject ) ) {
                return false;
            }
        }
        return true;
    }

    public Collection<CharacteristicValueObject> getExperimentCharacteristics() {
        return this.experimentCharacteristics;
    }

    public CitationValueObject getPrimaryPublicationCitationValueObject() {
        return this.primaryPublicationCitationValueObject;
    }

    public Set<CitationValueObject> getRelevantPublicationsCitationValueObjects() {
        return this.relevantPublicationsCitationValueObjects;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        for ( CharacteristicValueObject phenotype : this.experimentCharacteristics ) {
            result = result + phenotype.hashCode();
        }
        result = prime
                * result
                + ( ( this.primaryPublicationCitationValueObject == null ) ? 0
                        : this.primaryPublicationCitationValueObject.hashCode() );

        for ( CitationValueObject relevantPublicationsCitationValueObject : this.relevantPublicationsCitationValueObjects ) {
            result = result + relevantPublicationsCitationValueObject.hashCode();
        }

        return result;
    }

    public void setExperimentCharacteristics( Collection<CharacteristicValueObject> experimentCharacteristics ) {
        this.experimentCharacteristics = experimentCharacteristics;
    }

    public void setPrimaryPublicationCitationValueObject( CitationValueObject primaryPublicationCitationValueObject ) {
        this.primaryPublicationCitationValueObject = primaryPublicationCitationValueObject;
    }

    public void setRelevantPublicationsCitationValueObjects(
            Set<CitationValueObject> relevantPublicationsCitationValueObjects ) {
        this.relevantPublicationsCitationValueObjects = relevantPublicationsCitationValueObjects;
    }

}