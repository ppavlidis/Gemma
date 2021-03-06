package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

/**
 * Represents an API arguments that maps to a {@link FactorValue} by its ID or name.
 * @author poirigui
 */
public abstract class FactorValueArg<A> extends AbstractEntityArg<A, FactorValue, FactorValueService> {

    FactorValueArg( A value ) {
        super( value );
    }

    @Override
    public String getEntityName() {
        return "FactorValue";
    }

    public static FactorValueArg<?> valueOf( String value ) {
        try {
            return new FactorValueIdArg( Long.valueOf( value ) );
        } catch ( NumberFormatException e ) {
            return new FactorValueValueArg( value );
        }
    }
}
