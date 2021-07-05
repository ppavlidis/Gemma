package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * Long argument type for dataset API, referencing the Dataset ID.
 *
 * @author tesarst
 */
public class DatasetIdArg extends DatasetArg<Long> {

    /**
     * @param l intentionally primitive type, so the value property can never be null.
     */
    public DatasetIdArg( long l ) {
        super( l );
    }

    @Override
    public ExpressionExperiment getEntity( ExpressionExperimentService service ) {
        return checkEntity( service.load( this.getValue() ) );
    }

    @Override
    public String getPropertyName() {
        return "id";
    }
}
