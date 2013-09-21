/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.genome.sequenceAnalysis;

/**
 * An association between BioSequence and GeneProduct that is provided through an external annotation source, rather
 * than our own sequence analysis. Importantly, the 'overlap', 'score' and other parameters will not be filled in. Also
 * note that in these cases the associated BioSequence may not have actual sequence information filled in. This type of
 * association is used as a "last resort" annotation source for the following types of situations: No sequence
 * information is available; annotations are unavailable (e.g., non-model organisms); or sequences are too short to
 * align using our usual methods (e.g., miRNAs).
 */
public abstract class AnnotationAssociation extends ubic.gemma.model.association.BioSequence2GeneProduct {

    /**
     * Constructs new instances of {@link ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation}.
         */
        public static ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation newInstance() {
            return new ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -6885514937765078103L;
    private ubic.gemma.model.common.description.ExternalDatabase source;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public AnnotationAssociation() {
    }

    /**
     * <p>
     * The original source of the annotation, such as GEO or flyBase.
     * </p>
     */
    public ubic.gemma.model.common.description.ExternalDatabase getSource() {
        return this.source;
    }

    public void setSource( ubic.gemma.model.common.description.ExternalDatabase source ) {
        this.source = source;
    }

}