package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.StringUtils;

import java.util.List;

public class CompositeSequenceArrayArg
        extends AbstractEntityArrayArg<CompositeSequence, CompositeSequenceService> {
    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one "
            + "element ID or name, or multiple, separated by (',') character. "
            + "All identifiers must be same type, i.e. do not combine IDs and names in one query.";
    private static final String ERROR_MSG = ArrayArg.ERROR_MSG + " Element identifiers";

    private ArrayDesign arrayDesign;

    private CompositeSequenceArrayArg( List<String> values ) {
        super( values );
    }

    @Override
    protected Class<? extends AbstractEntityArg> getEntityArgClass() {
        return CompositeSequenceArg.class;
    }

    private CompositeSequenceArrayArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param  s the request arrayCompositeSequence argument
     * @return an instance of ArrayCompositeSequenceArg representing an array of CompositeSequence identifiers from
     *           the input string,
     *           or a malformed ArrayCompositeSequenceArg that will throw an {@link GemmaApiException} when accessing
     *           its value, if the
     *           input String can not be converted into an array of CompositeSequence identifiers.
     */
    @SuppressWarnings("unused")
    public static CompositeSequenceArrayArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) ) {
            return new CompositeSequenceArrayArg( String.format( CompositeSequenceArrayArg.ERROR_MSG, s ),
                    new IllegalArgumentException( CompositeSequenceArrayArg.ERROR_MSG_DETAIL ) );
        }
        return new CompositeSequenceArrayArg( StringUtils.splitAndTrim( s ) );
    }

    public void setPlatform( ArrayDesign arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    public List<ObjectFilter[]> getPlatformFilter() {
        ObjectFilter filter = new ObjectFilter( "arrayDesign.id", Long.class, this.arrayDesign.getId().toString(),
                ObjectFilter.is, ObjectFilter.DAO_PROBE_ALIAS );
        return ObjectFilter.singleFilter( filter );
    }

    @Override
    protected String getObjectDaoAlias() {
        return ObjectFilter.DAO_PROBE_ALIAS;
    }
}
