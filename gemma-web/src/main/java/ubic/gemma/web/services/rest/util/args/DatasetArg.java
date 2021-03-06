package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.core.analysis.preprocess.OutlierDetails;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mutable argument type base class for dataset (ExpressionExperiment) API.
 *
 * @author tesarst
 */
public abstract class DatasetArg<T>
        extends AbstractEntityArg<T, ExpressionExperiment, ExpressionExperimentService> {

    DatasetArg( T value ) {
        super( value );
    }

    @Override
    public String getEntityName() {
        return "Dataset";
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request dataset argument
     * @return instance of appropriate implementation of DatasetArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static DatasetArg<?> valueOf( final String s ) {
        try {
            return new DatasetIdArg( Long.parseLong( s.trim() ) );
        } catch ( NumberFormatException e ) {
            return new DatasetStringArg( s );
        }
    }

    /**
     * Retrieves the Platforms of the Dataset that this argument represents.
     *
     * @param service   service that will be used to retrieve the persistent EE object.
     * @param adService service to use to retrieve the ADs.
     * @return a collection of Platforms that the dataset represented by this argument is in.
     */
    public List<ArrayDesignValueObject> getPlatforms( ExpressionExperimentService service,
            ArrayDesignService adService ) {
        ExpressionExperiment ee = this.getEntity( service );
        return adService.loadValueObjectsForEE( ee.getId() );
    }

    /**
     * @param service                 service that will be used to retrieve the persistent EE object.
     * @param baService               service that will be used to convert the samples (BioAssays) to VOs.
     * @param outlierDetectionService service that will be used to detect which samples are outliers and fill their
     *                                corresponding predictedOutlier attribute.
     * @return a collection of BioAssays that represent the experiments samples.
     */
    public List<BioAssayValueObject> getSamples( ExpressionExperimentService service,
            BioAssayService baService, OutlierDetectionService outlierDetectionService ) {
        ExpressionExperiment ee = service.thawBioAssays( this.getEntity( service ) );
        Set<Long> predictedOutlierBioAssayIds = outlierDetectionService.identifyOutliersByMedianCorrelation( ee ).stream()
                .map( OutlierDetails::getBioAssay )
                .map( BioAssay::getId )
                .collect( Collectors.toSet() );
        List<BioAssayValueObject> bioAssayValueObjects = baService.loadValueObjects( ee.getBioAssays(), true );
        for ( BioAssayValueObject vo : bioAssayValueObjects ) {
            vo.setPredictedOutlier( predictedOutlierBioAssayIds.contains( vo.getId() ) );
        }
        return bioAssayValueObjects;
    }

    /**
     * @param service service that will be used to retrieve the persistent EE object.
     * @return a collection of Annotations value objects that represent the experiments annotations.
     */
    public Set<AnnotationValueObject> getAnnotations( ExpressionExperimentService service ) {
        ExpressionExperiment ee = this.getEntity( service );
        return service.getAnnotations( ee.getId() );
    }
}
