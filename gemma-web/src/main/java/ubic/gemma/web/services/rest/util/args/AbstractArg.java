package ubic.gemma.web.services.rest.util.args;

import org.jetbrains.annotations.NotNull;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.ws.rs.core.Response;

/**
 * Base class for non Object-specific functionality argument types, that can be malformed on input (E.g an argument
 * representing a number was a non-numeric string in the request).
 *
 * @author tesarst
 */
abstract class AbstractArg<T> {

    private final T value;

    /* if this is malformed */
    private final boolean malformed;
    private String errorMessage = "";
    private Exception exception;

    AbstractArg( T value ) {
        this.value = value;
        this.malformed = false;
    }

    /**
     * Constructor used to inform that the received argument was not well-formed.
     *
     * @param errorMessage the error message to be displayed to the client.
     * @param exception    the exception that the client should be informed about.
     */
    AbstractArg( String errorMessage, Exception exception ) {
        this.value = null;
        this.malformed = true;
        this.exception = exception;
        this.errorMessage = errorMessage;
    }

    /**
     * Obtain the value, or exception represented by this argument.
     *
     * Checks whether the instance of this object was created as a malformed argument, and if true, throws an
     * exception using the information provided in the constructor.
     *
     * Even though the internal value can be null, it is only the case when it is malformed and this method will produce
     * a {@link GemmaApiException}, thus guaranteeing non-nullity.
     */
    @NotNull
    public final T getValue() throws GemmaApiException {
        if ( this.malformed ) {
            WellComposedErrorBody body = new WellComposedErrorBody( Response.Status.BAD_REQUEST, errorMessage );
            WellComposedErrorBody.addExceptionFields( body, this.exception );
            throw new GemmaApiException( body );
        }
        return this.value;
    }

    @Override
    public String toString() {
        if ( this.malformed ) {
            return "This " + getClass().getName() + " is malformed because of the following error: " + errorMessage;
        } else {
            return this.value == null ? "" : String.valueOf( this.value );
        }
    }
}
