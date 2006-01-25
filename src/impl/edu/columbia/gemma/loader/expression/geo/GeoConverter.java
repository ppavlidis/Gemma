/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package edu.columbia.gemma.loader.expression.geo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.auditAndSecurity.Contact;
import edu.columbia.gemma.common.auditAndSecurity.Person;
import edu.columbia.gemma.common.description.Characteristic;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.DatabaseType;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.OntologyEntry;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.bioAssayData.DesignElementDataVector;
import edu.columbia.gemma.expression.biomaterial.BioMaterial;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.experiment.ExperimentalDesign;
import edu.columbia.gemma.expression.experiment.ExperimentalFactor;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentSubSet;
import edu.columbia.gemma.expression.experiment.FactorValue;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.biosequence.BioSequence;
import edu.columbia.gemma.genome.biosequence.PolymerType;
import edu.columbia.gemma.genome.biosequence.SequenceType;
import edu.columbia.gemma.loader.expression.geo.model.GeoChannel;
import edu.columbia.gemma.loader.expression.geo.model.GeoContact;
import edu.columbia.gemma.loader.expression.geo.model.GeoData;
import edu.columbia.gemma.loader.expression.geo.model.GeoDataset;
import edu.columbia.gemma.loader.expression.geo.model.GeoPlatform;
import edu.columbia.gemma.loader.expression.geo.model.GeoReplication;
import edu.columbia.gemma.loader.expression.geo.model.GeoSample;
import edu.columbia.gemma.loader.expression.geo.model.GeoSeries;
import edu.columbia.gemma.loader.expression.geo.model.GeoSubset;
import edu.columbia.gemma.loader.expression.geo.model.GeoVariable;
import edu.columbia.gemma.loader.expression.geo.model.GeoReplication.ReplicationType;
import edu.columbia.gemma.loader.expression.geo.model.GeoVariable.VariableType;
import edu.columbia.gemma.loader.expression.geo.util.GeoConstants;
import edu.columbia.gemma.loader.loaderutils.Converter;

/**
 * Convert GEO domain objects into Gemma objects. Usually we trigger this by passing in GeoDataset objects.
 * <p>
 * GEO has four basic kinds of objects: Platforms (ArrayDesigns), Samples (BioAssays), Series (Experiments) and DataSets
 * (which are curated Experiments). Note that a sample can belong to more than one series. A series can include more
 * than one dataset. See http://www.ncbi.nlm.nih.gov/projects/geo/info/soft2.html.
 * <p>
 * For our purposes, a usable expression data set is represented by a GEO "GDS" number (a curated dataset), which
 * corresponds to a series. HOWEVER, multiple datasets may go together to form a series (GSE). This can happen when the
 * "A" and "B" arrays were both run on the same samples.
 * <p>
 * We don't want the GSE and the GDS for a single experiment to be both converted to ExpressionExperiments. We want the
 * information contained to be combined into one ExpressionExperiment.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoConverter implements Converter {

    private static Log log = LogFactory.getLog( GeoConverter.class.getName() );

    private ExternalDatabase geoDatabase;

    private Collection<Object> results = new HashSet<Object>();

    private Map<String, ArrayDesign> seenPlatforms = new HashMap<String, ArrayDesign>();

    private Map<String, Map<String, CompositeSequence>> platformDesignElementMap = new HashMap<String, Map<String, CompositeSequence>>();

    public GeoConverter() {
        geoDatabase = ExternalDatabase.Factory.newInstance();
        geoDatabase.setName( "GEO" );
        geoDatabase.setType( DatabaseType.EXPRESSION );
    }

    /**
     * @param seriesMap
     */
    @SuppressWarnings("unchecked")
    public Collection<Object> convert( Collection geoObjects ) {
        for ( Object geoObject : geoObjects ) {
            Object convertedObject = convert( geoObject );
            if ( convertedObject != null ) {
                if ( convertedObject instanceof Collection ) {
                    results.addAll( ( Collection ) convertedObject );
                } else {
                    results.add( convertedObject );
                }
            }
        }

        log.info( "Converted object tally:\n" + this );

        // log.debug( "Detailed object tree:" );
        // log.debug( PrettyPrinter.print( results ) );

        return results;
    }

    /**
     * @param geoObject
     */
    public Object convert( Object geoObject ) {
        if ( geoObject == null ) {
            log.warn( "Null object" );
            return null;
        }
        if ( geoObject instanceof Collection ) {
            return convert( ( Collection ) geoObject );
        } else if ( geoObject instanceof GeoDataset ) {
            return convertDataset( ( GeoDataset ) geoObject );
        } else if ( geoObject instanceof GeoSeries ) { // typically we start here, with a series.
            return convertSeries( ( GeoSeries ) geoObject );
        } else if ( geoObject instanceof GeoSubset ) {
            throw new IllegalArgumentException( "Can't deal with " + geoObject.getClass().getName() + " ('" + geoObject
                    + "')" );
        } else if ( geoObject instanceof GeoSample ) {
            throw new IllegalArgumentException( "Can't deal with " + geoObject.getClass().getName() + " ('" + geoObject
                    + "')" );
        } else if ( geoObject instanceof GeoPlatform ) {
            return convertPlatform( ( GeoPlatform ) geoObject );
        } else {
            throw new IllegalArgumentException( "Can't deal with " + geoObject.getClass().getName() + " ('" + geoObject
                    + "')" );
        }

    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        Map<String, Integer> tally = new HashMap<String, Integer>();
        for ( Object element : results ) {
            String clazz = element.getClass().getName();
            if ( !tally.containsKey( clazz ) ) {
                tally.put( clazz, new Integer( 0 ) );
            }
            tally.put( clazz, new Integer( ( tally.get( clazz ) ).intValue() + 1 ) );
        }

        for ( String clazz : tally.keySet() ) {
            buf.append( tally.get( clazz ) + " " + clazz + "s\n" );
        }

        return buf.toString();
    }

    /**
     * @param dataset
     * @param expExp
     */
    private ExpressionExperiment convertDataset( GeoDataset geoDataset, ExpressionExperiment expExp ) {
        log.info( "Converting dataset:" + geoDataset );

        if ( StringUtils.isEmpty( expExp.getDescription() ) ) {
            expExp.setDescription( geoDataset.getDescription() ); // probably not empty.
        }

        expExp.setDescription( expExp.getDescription() + " Includes " + geoDataset.getGeoAccession() + ". " );
        if ( StringUtils.isNotEmpty( geoDataset.getUpdateDate() ) ) {
            expExp.setDescription( expExp.getDescription() + " Update date " + geoDataset.getUpdateDate() + ". " );
        }

        if ( StringUtils.isEmpty( expExp.getName() ) ) {
            expExp.setName( geoDataset.getTitle() );
        } else {
            expExp.setDescription( expExp.getDescription() + " Dataset description " + geoDataset.getGeoAccession()
                    + ": " + geoDataset.getTitle() + ". " );
        }

        ArrayDesign ad = seenPlatforms.get( geoDataset.getPlatform().getGeoAccession() );
        if ( ad == null )
            throw new IllegalStateException( "ArrayDesigns must be converted before datasets - didn't find "
                    + geoDataset.getPlatform() );

        Map<String, List<String>> data = geoDataset.getData();
        for ( String probe : data.keySet() ) {
            List<String> dataVector = data.get( probe );
            // convert this into a Blob.
            CompositeSequence designElement = platformDesignElementMap.get( ad.getName() ).get( probe );

            DesignElementDataVector vector = DesignElementDataVector.Factory.newInstance();
            vector.setDesignElement( designElement );
            vector.setExpressionExperiment( expExp );
            // vector.setQuantitationType();
            // vector.setBioAssayDimension();
            // vector.setData();
        }

        convertSubsetAssociations( expExp, geoDataset );
        return expExp;

    }

    /**
     * @param geoDataset
     */
    private ExpressionExperiment convertDataset( GeoDataset geoDataset ) {

        if ( geoDataset.getSeries().size() == 0 ) {
            throw new IllegalArgumentException( "GEO Dataset must have associated series" );
        }

        if ( geoDataset.getSeries().size() > 1 ) {
            throw new UnsupportedOperationException( "GEO Dataset can only be associated with one series" );
        }

        return this.convertSeries( geoDataset.getSeries().iterator().next() );

        // // make sure that we don't already have an expression experiment corresponding to this dataset.
        //
        // log.debug( "Converting dataset:" + geoDataset.getGeoAccession() );
        // ExpressionExperiment result = ExpressionExperiment.Factory.newInstance();
        // result.setDescription( geoDataset.getDescription() );
        // result.setName( geoDataset.getTitle() );
        // result.setAccession( convertDatabaseEntry( geoDataset ) );
        //
        // for ( ExpressionExperiment expressionExperiment : convertedExpressionExperiments ) {
        // if ( expressionExperiment.getAccession() == result.getAccession() ) {
        // log.info( "Already have expressionExperiment corresondponding to " + result.getAccession() );
        // result = expressionExperiment;
        // // convertSubsetAssociations( result, geoDataset );
        // convertSeriesAssociations( result, geoDataset );
        // return null;
        // }
        // }
        //
        // // convertSubsetAssociations( result, geoDataset );
        // convertSeriesAssociations( result, geoDataset );
        // return result;
    }

    /**
     * @param sample
     * @param channel
     * @return
     */
    @SuppressWarnings("unchecked")
    private BioMaterial convertChannel( GeoSample sample, GeoChannel channel ) {
        log.debug( "Sample: " + sample.getGeoAccession() + " - Converting channel " + channel.getSourceName() );
        BioMaterial bioMaterial = BioMaterial.Factory.newInstance();

        bioMaterial.setExternalAccession( convertDatabaseEntry( sample ) );
        bioMaterial.setName( sample.getGeoAccession() + "_channel_" + channel.getChannelNumber() );
        bioMaterial.setDescription( "Channel sample source="
                + channel.getOrganism()
                + " "
                + channel.getSourceName()
                + ( StringUtils.isBlank( channel.getExtractProtocol() ) ? "" : " Extraction Protocol: "
                        + channel.getExtractProtocol() )
                + ( StringUtils.isBlank( channel.getLabelProtocol() ) ? "" : " Labeling Protocol: "
                        + channel.getLabelProtocol() )
                + ( StringUtils.isBlank( channel.getTreatmentProtocol() ) ? "" : " Treatment Protocol: "
                        + channel.getTreatmentProtocol() ) );
        // FIXME: these protocols could be made into 'real' protocols, if anybody cares.

        for ( String characteristic : channel.getCharacteristics() ) {
            Characteristic gemmaChar = Characteristic.Factory.newInstance();
            gemmaChar.setCategory( characteristic );
            gemmaChar.setValue( characteristic ); // FIXME need to put in actual value.
            bioMaterial.getCharacteristics().add( gemmaChar );
        }
        return bioMaterial;
    }

    /**
     * @param contact
     * @return
     */
    private Person convertContact( GeoContact contact ) {
        Person result = Person.Factory.newInstance();
        result.setAddress( contact.getCity() );
        result.setPhone( contact.getPhone() );
        result.setName( contact.getName() );
        result.setEmail( contact.getEmail() );

        // FIXME - set other contact fields
        return result;
    }

    /**
     * Often-needed generation of a valid databaseentry object.
     * 
     * @param geoData
     * @return
     */
    private DatabaseEntry convertDatabaseEntry( GeoData geoData ) {
        DatabaseEntry result = DatabaseEntry.Factory.newInstance();
        result.setExternalDatabase( this.geoDatabase );
        result.setAccession( geoData.getGeoAccession() );
        return result;
    }

    /**
     * @param platform
     */
    @SuppressWarnings("unchecked")
    private ArrayDesign convertPlatform( GeoPlatform platform ) {

        if ( seenPlatforms.containsKey( platform.getGeoAccession() ) ) {
            return ( seenPlatforms.get( platform.getGeoAccession() ) );
        }

        log.debug( "Converting platform: " + platform.getGeoAccession() );
        ArrayDesign arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setName( platform.getTitle() );
        arrayDesign.setDescription( platform.getDescriptions() );

        platformDesignElementMap.put( arrayDesign.getName(), new HashMap<String, CompositeSequence>() );

        Taxon taxon = convertPlatformOrganism( platform );

        // convert the design element information.
        String identifier = determinePlatformIdentifier( platform );
        String externalReference = determinePlatformExternalReferenceIdentifier( platform );
        String descriptionColumn = determinePlatformDescriptionColumn( platform );
        ExternalDatabase externalDb = determinePlatformExternalDatabase( platform );

        List<String> identifiers = platform.getData().get( identifier );
        List<String> externalRefs = platform.getData().get( externalReference );
        List<String> descriptions = platform.getData().get( descriptionColumn );

        assert identifier != null;
        assert externalRefs != null;
        assert externalRefs.size() == identifiers.size() : "Unequal numbers of identifiers and external references! "
                + externalRefs.size() + " != " + identifiers.size();

        log.debug( "Converting " + identifiers.size() + " probe identifiers on GEO platform "
                + platform.getGeoAccession() );

        Iterator<String> refIter = externalRefs.iterator();
        Iterator<String> descIter = descriptions.iterator();
        Collection compositeSequences = new HashSet();
        for ( String id : identifiers ) {
            String externalRef = refIter.next();
            String description = descIter.next();
            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( id );
            cs.setDescription( description );
            cs.setArrayDesign( arrayDesign );

            BioSequence bs = BioSequence.Factory.newInstance();
            bs.setName( externalRef );
            bs.setTaxon( taxon );
            bs.setPolymerType( PolymerType.DNA ); // FIXME: need to determine PolymerType.
            bs.setType( SequenceType.DNA ); // FIXME need to determine SequenceType

            DatabaseEntry dbe = DatabaseEntry.Factory.newInstance();
            dbe.setAccession( externalRef );
            dbe.setExternalDatabase( externalDb );

            cs.setBiologicalCharacteristic( bs );

            compositeSequences.add( cs );

            platformDesignElementMap.get( arrayDesign.getName() ).put( id, cs );
        }
        arrayDesign.setDesignElements( compositeSequences );
        arrayDesign.setAdvertisedNumberOfDesignElements( compositeSequences.size() );

        Contact manufacturer = Contact.Factory.newInstance();
        if ( platform.getManufacturer() != null ) {
            manufacturer.setName( platform.getManufacturer() );
        } else {
            manufacturer.setName( "Unknown" );
        }
        arrayDesign.setDesignProvider( manufacturer );

        seenPlatforms.put( platform.getGeoAccession(), arrayDesign );

        return arrayDesign;
    }

    /**
     * @param platform
     * @return
     */
    private Taxon convertPlatformOrganism( GeoPlatform platform ) {
        Taxon taxon = Taxon.Factory.newInstance();
        Collection<String> organisms = platform.getOrganisms();

        if ( organisms.size() > 1 ) {
            log.warn( "!!!! Multiple organisms represented on platform " + platform
                    + " --- BioSequences will be associated with the first one found." );
        }

        if ( organisms.size() == 0 ) {
            log.warn( "No organisms for platform " + platform );
            return null;
        }

        String organism = organisms.iterator().next();
        log.debug( "Organism: " + organism );

        // FIXME yucky hard-coding of Rattus.
        if ( organism.startsWith( "Rattus" ) || organism.startsWith( "rattus" ) ) {
            organism = "rattus"; // we don't distinguish between species.
        }

        taxon.setScientificName( organism );
        return taxon;
    }

    /**
     * @param repType
     * @return
     */
    private OntologyEntry convertReplicatationType( ReplicationType repType ) {
        OntologyEntry result = OntologyEntry.Factory.newInstance();
        ExternalDatabase mged = ExternalDatabase.Factory.newInstance();

        if ( !repType.equals( VariableType.other ) ) {
            mged.setName( "MGED Ontology" );
            mged.setType( DatabaseType.ONTOLOGY );
            result.setExternalDatabase( mged );
        }

        if ( repType.equals( ReplicationType.biologicalReplicate ) ) {
            result.setValue( "biological_replicate" );
        } else if ( repType.equals( ReplicationType.technicalReplicateExtract ) ) {
            result.setValue( "technical_replicate" );
        } else if ( repType.equals( ReplicationType.technicalReplicateLabeledExtract ) ) {
            result.setValue( "technical_replicate" ); // MGED doesn't have a term to distinguish these.
        } else {
            throw new IllegalStateException();
        }

        return result;

    }

    /**
     * Convert a variable into a ExperimentalFactor
     * 
     * @param variable
     * @return
     */
    private ExperimentalFactor convertReplicationToFactor( GeoReplication replication ) {
        log.debug( "Converting replication " + replication.getType() );
        ExperimentalFactor result = ExperimentalFactor.Factory.newInstance();
        result.setName( replication.getType().toString() );
        result.setDescription( replication.getDescription() );
        OntologyEntry term = convertReplicatationType( replication.getType() );

        result.setCategory( term );
        return result;

    }

    /**
     * @param replication
     * @return
     */
    private FactorValue convertReplicationToFactorValue( GeoReplication replication ) {
        FactorValue factorValue = FactorValue.Factory.newInstance();
        factorValue.setValue( replication.getDescription() );
        return factorValue;
    }

    /**
     * @param variable
     * @param factor
     */
    @SuppressWarnings("unchecked")
    private void convertReplicationToFactorValue( GeoReplication replication, ExperimentalFactor factor ) {
        FactorValue factorValue = convertReplicationToFactorValue( replication );
        factor.getFactorValues().add( factorValue );
    }

    /**
     * A Sample corresponds to a BioAssay; the channels correspond to BioMaterials.
     * 
     * @param sample
     */
    @SuppressWarnings("unchecked")
    private BioAssay convertSample( GeoSample sample ) {
        if ( sample == null ) {
            log.warn( "Null sample" );
            return null;
        }

        if ( sample.getGeoAccession() == null || sample.getGeoAccession().length() == 0 ) {
            log.error( "No GEO accession for sample" );
            return null;
        }

        log.info( "Converting sample: " + sample.getGeoAccession() );

        BioAssay bioAssay = BioAssay.Factory.newInstance();
        String title = sample.getTitle();
        if ( StringUtils.isBlank( title ) ) {
            throw new IllegalArgumentException( "Title cannot be blank for sample " + sample );
            // log.warn( "Blank title for sample " + sample );
        }
        bioAssay.setName( sample.getTitle() );
        bioAssay.setDescription( sample.getDescription() );
        bioAssay.setAccession( convertDatabaseEntry( sample ) );

        // FIXME: use the ones from the ExperimentalFactor.
        for ( GeoReplication replication : sample.getReplicates() ) {
            bioAssay.getFactorValues().add( convertReplicationToFactorValue( replication ) );
        }

        // FIXME: use the ones from the ExperimentalFactor.
        for ( GeoVariable variable : sample.getVariables() ) {
            bioAssay.getFactorValues().add( convertVariableToFactorValue( variable ) );
        }

        for ( GeoChannel channel : sample.getChannels() ) {
            BioMaterial bioMaterial = convertChannel( sample, channel );
            bioAssay.getSamplesUsed().add( bioMaterial );
        }

        for ( GeoPlatform platform : sample.getPlatforms() ) {
            if ( seenPlatforms.containsKey( platform.getGeoAccession() ) ) continue;
            log.info( "Converting " + platform );
            ArrayDesign arrayDesign = convertPlatform( platform );
            results.add( arrayDesign );
            seenPlatforms.put( platform.getGeoAccession(), arrayDesign );
            bioAssay.getArrayDesignsUsed().add( arrayDesign );
        }

        return bioAssay;
    }

    /**
     * @param series
     * @return
     */
    private ExpressionExperiment convertSeries( GeoSeries series ) {
        return this.convertSeries( series, null );
    }

    /**
     * @param series
     * @param resultToAddTo
     * @return
     */
    @SuppressWarnings("unchecked")
    private ExpressionExperiment convertSeries( GeoSeries series, ExpressionExperiment resultToAddTo ) {
        if ( series == null ) return null;
        log.debug( "Converting series: " + series.getGeoAccession() );

        ExpressionExperiment expExp;

        if ( resultToAddTo == null ) {
            expExp = ExpressionExperiment.Factory.newInstance();
            expExp.setDescription( "" );
        } else {
            expExp = resultToAddTo;
        }

        expExp.setDescription( series.getSummaries() );
        expExp.setName( series.getTitle() );

        convertContacts( series, expExp );

        expExp.setAccession( convertDatabaseEntry( series ) );

        ExperimentalDesign design = ExperimentalDesign.Factory.newInstance();
        design.setDescription( "" );
        Collection<GeoVariable> variables = series.getVariables().values();
        for ( GeoVariable variable : variables ) {
            log.debug( "Adding variable " + variable );
            ExperimentalFactor ef = convertVariableToFactor( variable );
            convertVariableToFactorValue( variable, ef );
            design.getExperimentalFactors().add( ef );
        }

        // FIXME - these could go somewhere more interesting, eg ontology enntry
        if ( series.getKeyWords().size() > 0 ) {
            for ( String keyWord : series.getKeyWords() ) {
                design.setDescription( design.getDescription() + " Keyword: " + keyWord );
            }
        }

        if ( series.getOverallDesign() != null ) {
            design.setDescription( design.getDescription() + " Overall design: " + series.getOverallDesign() );
        }

        Collection<GeoReplication> replication = series.getReplicates().values();
        for ( GeoReplication replicate : replication ) {
            log.debug( "Adding replication " + replicate );
            ExperimentalFactor ef = convertReplicationToFactor( replicate );
            convertReplicationToFactorValue( replicate, ef );
            design.getExperimentalFactors().add( ef );
        }

        expExp.setExperimentalDesigns( new HashSet<ExperimentalDesign>() );
        expExp.getExperimentalDesigns().add( design );

        // GEO does not have the concept of a biomaterial.
        Collection<BioMaterial> bioMaterials = new HashSet<BioMaterial>();
        Collection<GeoSample> samples = series.getSamples();
        expExp.setBioAssays( new HashSet() );
        int i = 1;
        for ( Iterator iter = series.getSampleCorrespondence().iterator(); iter.hasNext(); ) {

            BioMaterial bioMaterial = BioMaterial.Factory.newInstance();
            bioMaterial.setName( series.getGeoAccession() + "_" + i );
            i++;

            // Find the sample and convert it.
            Set<String> correspondingSamples = ( Set<String> ) iter.next();
            for ( String cSample : correspondingSamples ) {
                boolean found = false;
                for ( GeoSample sample : samples ) {
                    if ( sample == null || sample.getGeoAccession() == null ) {
                        log.warn( "Null sample or no accession for " + sample );
                        continue;
                    }

                    String accession = sample.getGeoAccession();

                    if ( accession.equals( cSample ) ) {
                        BioAssay ba = convertSample( sample );
                        ba.getSamplesUsed().add( bioMaterial );
                        log.info( "Adding " + ba + " and associating with  " + bioMaterial );
                        expExp.getBioAssays().add( ba );
                        found = true;
                        break;
                    }

                }
                if ( !found ) log.error( "No sample found for " + cSample );
            }

            bioMaterials.add( bioMaterial );
        }

        // Dataset has additional information about the samples.
        Collection<GeoDataset> dataSets = series.getDatasets();
        for ( GeoDataset dataset : dataSets ) {
            convertDataset( dataset, expExp );
        }

        return expExp;
    }

    /**
     * Take contact and contributer information from a GeoSeries and put it in the ExpressionExperiment.
     * 
     * @param series
     * @param expExp
     */
    @SuppressWarnings("unchecked")
    private void convertContacts( GeoSeries series, ExpressionExperiment expExp ) {
        expExp.getInvestigators().add( convertContact( series.getContact() ) );
        // FIXME possibly add contributers to the investigators.
        if ( series.getContributers().size() > 0 ) {
            expExp.setDescription( expExp.getDescription() + " -- Contributers: " );
            for ( GeoContact contributer : series.getContributers() ) {
                expExp.setDescription( expExp.getDescription() + " " + contributer.getName() );
            }
        }
    }

    // /**
    // * @param result
    // * @param geoDataset
    // */
    // private void convertSeriesAssociations( ExpressionExperiment result, GeoDataset geoDataset ) {
    // for ( GeoSeries series : geoDataset.getSeries() ) {
    // log.debug( "Converting series associated with dataset: " + series.getGeoAccession() );
    // ExpressionExperiment newInfo = convertSeries( series, result );
    // BeanPropertyCompleter.complete( newInfo, result ); // FIXME not good enough.
    // }
    // }

    /**
     * @param expExp
     * @param geoSubSet
     */
    @SuppressWarnings("unchecked")
    private ExpressionExperimentSubSet convertSubset( ExpressionExperiment expExp, GeoSubset geoSubSet ) {

        ExpressionExperimentSubSet subSet = ExpressionExperimentSubSet.Factory.newInstance();

        subSet.setSourceExperiment( expExp );

        expExp.setBioAssays( new HashSet() );
        for ( GeoSample sample : geoSubSet.getSamples() ) {

            BioAssay bioAssay = convertSample( sample ); // converted object here is not used.

            boolean found = addMatchingBioAssayToSubSet( subSet, bioAssay, expExp );
            assert found : "No matching bioassay found for " + bioAssay.getAccession().getAccession() + " in subset. "
                    + " Make sure the ExpressionExperiment was initialized "
                    + "properly by converting the samples before converting the subsets.";
        }
        return subSet;
    }

    /**
     * @param subSet
     * @param accession
     * @param experimentBioAssays
     * @return
     */
    @SuppressWarnings("unchecked")
    private boolean addMatchingBioAssayToSubSet( ExpressionExperimentSubSet subSet, BioAssay bioAssay,
            ExpressionExperiment expExp ) {
        String accession = bioAssay.getAccession().getAccession();
        Collection<BioAssay> experimentBioAssays = expExp.getBioAssays();
        boolean found = false;
        for ( BioAssay assay : experimentBioAssays ) {
            String testAccession = assay.getAccession().getAccession();
            if ( testAccession.equals( accession ) ) {
                subSet.getBioAssays().add( assay );
                found = true;
                break;
            }

        }
        return found;
    }

    /**
     * @param result
     * @param geoDataset
     */
    @SuppressWarnings("unchecked")
    private void convertSubsetAssociations( ExpressionExperiment result, GeoDataset geoDataset ) {
        for ( GeoSubset subset : geoDataset.getSubsets() ) {
            log.debug( "Converting subset: " + subset.getType() );
            ExpressionExperimentSubSet ees = convertSubset( result, subset );
            result.getSubsets().add( ees );
        }
    }

    /**
     * Convert a variable into a ExperimentalFactor
     * 
     * @param variable
     * @return
     */
    private ExperimentalFactor convertVariableToFactor( GeoVariable variable ) {
        log.debug( "Converting variable " + variable.getType() );
        ExperimentalFactor result = ExperimentalFactor.Factory.newInstance();
        result.setName( variable.getType().toString() );
        result.setDescription( variable.getDescription() );
        OntologyEntry term = convertVariableType( variable.getType() );

        result.setCategory( term );
        return result;

    }

    /**
     * @param variable
     * @return
     */
    private FactorValue convertVariableToFactorValue( GeoVariable variable ) {
        FactorValue factorValue = FactorValue.Factory.newInstance();
        factorValue.setValue( variable.getDescription() );
        return factorValue;
    }

    /**
     * @param variable
     * @param factor
     */
    @SuppressWarnings("unchecked")
    private void convertVariableToFactorValue( GeoVariable variable, ExperimentalFactor factor ) {
        FactorValue factorValue = convertVariableToFactorValue( variable );
        factor.getFactorValues().add( factorValue );
    }

    /**
     * Convert a variable
     * 
     * @param variable
     * @return
     */
    private OntologyEntry convertVariableType( VariableType varType ) {
        OntologyEntry categoryTerm = OntologyEntry.Factory.newInstance();
        ExternalDatabase mged = ExternalDatabase.Factory.newInstance();

        if ( !varType.equals( VariableType.other ) ) {
            mged.setName( "MGED Ontology" );
            mged.setType( DatabaseType.ONTOLOGY );
            categoryTerm.setExternalDatabase( mged );
        }

        if ( varType.equals( VariableType.age ) ) {
            categoryTerm.setValue( "Age" );
        } else if ( varType.equals( VariableType.agent ) ) {
            categoryTerm.setValue( "----" ); // FIXME
        } else if ( varType.equals( VariableType.cellLine ) ) {
            categoryTerm.setValue( "CellLine" );
        } else if ( varType.equals( VariableType.cellType ) ) {
            categoryTerm.setValue( "CellType" );
        } else if ( varType.equals( VariableType.developmentStage ) ) {
            categoryTerm.setValue( "DevelopmentalStage" );
        } else if ( varType.equals( VariableType.diseaseState ) ) {
            categoryTerm.setValue( "DiseaseState" );
        } else if ( varType.equals( VariableType.dose ) ) {
            categoryTerm.setValue( "Dose" );
        } else if ( varType.equals( VariableType.gender ) ) {
            categoryTerm.setValue( "Sex" );
        } else if ( varType.equals( VariableType.genotypeOrVariation ) ) {
            categoryTerm.setValue( "IndividualGeneticCharacteristics" );
        } else if ( varType.equals( VariableType.growthProtocol ) ) {
            categoryTerm.setValue( "GrowthCondition" );
        } else if ( varType.equals( VariableType.individual ) ) {
            categoryTerm.setValue( "Individiual" );
        } else if ( varType.equals( VariableType.infection ) ) {
            categoryTerm.setValue( "Phenotype" );
        } else if ( varType.equals( VariableType.isolate ) ) {
            categoryTerm.setValue( "Age" );
        } else if ( varType.equals( VariableType.metabolism ) ) {
            categoryTerm.setValue( "Metabolism" );
        } else if ( varType.equals( VariableType.other ) ) {
            categoryTerm.setValue( "Other" );
        } else if ( varType.equals( VariableType.protocol ) ) {
            categoryTerm.setValue( "Protocol" );
        } else if ( varType.equals( VariableType.shock ) ) {
            categoryTerm.setValue( "EnvironmentalStress" );
        } else if ( varType.equals( VariableType.species ) ) {
            categoryTerm.setValue( "Organism" );
        } else if ( varType.equals( VariableType.specimen ) ) {
            categoryTerm.setValue( "BioSample" );
        } else if ( varType.equals( VariableType.strain ) ) {
            categoryTerm.setValue( "StrainOrLine" );
        } else if ( varType.equals( VariableType.stress ) ) {
            categoryTerm.setValue( "EnvironmentalStress" );
        } else if ( varType.equals( VariableType.temperature ) ) {
            categoryTerm.setValue( "Temperature" );
        } else if ( varType.equals( VariableType.time ) ) {
            categoryTerm.setValue( "Time" );
        } else if ( varType.equals( VariableType.tissue ) ) {
            categoryTerm.setValue( "OrganismPart" );
        } else {
            throw new IllegalStateException();
        }

        return categoryTerm;

    }

    /**
     * @param platform
     * @return
     */
    private String determinePlatformDescriptionColumn( GeoPlatform platform ) {
        Collection<String> columnNames = platform.getColumnNames();
        int index = 0;
        for ( String string : columnNames ) {
            if ( GeoConstants.likelyProbeDescription( string ) ) {
                log.info( string + " appears to indicate the  probe descriptions in column " + index + " for platform "
                        + platform );
                return string;
            }
            index++;
        }
        log.warn( "No platform element description column found" );
        return null;
    }

    /**
     * @param platform
     * @return
     */
    private ExternalDatabase determinePlatformExternalDatabase( GeoPlatform platform ) {
        ExternalDatabase result = ExternalDatabase.Factory.newInstance();
        result.setType( DatabaseType.SEQUENCE );

        String likelyExternalDatabaseIdentifier = determinePlatformExternalReferenceIdentifier( platform );
        String dbIdentifierDescription = getDbIdentifierDescription( platform );

        String url = null;
        if ( dbIdentifierDescription == null ) {
            throw new IllegalStateException( "Could not identify database identifier column in " + platform );
        } else if ( dbIdentifierDescription.indexOf( "LINK_PRE:" ) >= 0 ) {
            // example: #ORF = ORF reference LINK_PRE:"http://genome-www4.stanford.edu/cgi-bin/SGD/locus.pl?locus="
            url = dbIdentifierDescription.substring( dbIdentifierDescription.indexOf( "LINK_PRE:" ) );
            result.setWebUri( url );
        }

        if ( likelyExternalDatabaseIdentifier.equals( "GB_ACC" ) || likelyExternalDatabaseIdentifier.equals( "GB_LIST" ) ) {
            result.setName( "Genbank" );
            result.setType( DatabaseType.SEQUENCE );
        } else if ( likelyExternalDatabaseIdentifier.equals( "ORF" ) ) {
            String organism = platform.getOrganisms().iterator().next();
            result.setName( organism );// FIXME what else can we do?
        }

        return result;
    }

    /**
     * @param platform
     * @return
     */
    private String determinePlatformExternalReferenceIdentifier( GeoPlatform platform ) {
        Collection<String> columnNames = platform.getColumnNames();
        int index = 0;
        for ( String string : columnNames ) {
            if ( GeoConstants.likelyExternalReference( string ) ) {
                log.debug( string + " appears to indicate the external reference identifier in column " + index
                        + " for platform " + platform );
                return string;
            }
            index++;
        }
        return null;
    }

    /**
     * @param platform
     * @return
     */
    private String determinePlatformIdentifier( GeoPlatform platform ) {
        Collection<String> columnNames = platform.getColumnNames();
        int index = 0;
        for ( String string : columnNames ) {
            if ( GeoConstants.likelyId( string ) ) {
                log.info( string + " appears to indicate the array element identifier in column " + index
                        + " for platform " + platform );
                return string;
            }
            index++;
        }
        return null;
    }

    /**
     * @param platform
     * @return
     */
    private String getDbIdentifierDescription( GeoPlatform platform ) {
        Collection<String> columnNames = platform.getColumnNames();
        int index = 0;
        for ( String string : columnNames ) {
            if ( GeoConstants.likelyExternalReference( string ) ) {
                return platform.getColumnDescriptions().get( index );
            }
            index++;
        }
        return null;
    }

}
