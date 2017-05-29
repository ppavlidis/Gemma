package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.VoEnabledDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by tesarst on 07/03/17.
 * DAO covering methods common to all Curatable objects.
 *
 * @author tesarst
 */
public abstract class AbstractCuratableDao<C extends Curatable, VO extends AbstractCuratableValueObject<C>>
        extends VoEnabledDao<C, VO> implements CuratableDao<C, VO> {

    public AbstractCuratableDao( Class<C> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    /* ********************************
     * Abstract methods
     * ********************************/

    public abstract Map<Taxon, Long> getPerTaxonCount();

    /* ********************************
     * Public methods
     * ********************************/

    /**
     * Finds an entity by given name.
     *
     * @param name name of the entity to be found.
     * @return entity with given name, or null if such entity does not exist.
     */
    public Collection<C> findByName( String name ) {
        return this.findByParam( "name", name );
    }

    /**
     * Finds an entity by given short name.
     *
     * @param name short name of the entity to be found.
     * @return entity with given short name, or null if such entity does not exist.
     */
    public C findByShortName( String name ) {
        return this.findByParam( "shortName", name ).iterator().next();
    }

    /* ********************************
     * Protected methods
     * ********************************/

    protected void addEventsToMap( Map<Long, Collection<AuditEvent>> eventMap, Long id, AuditEvent event ) {
        if ( eventMap.containsKey( id ) ) {

            Collection<AuditEvent> events = eventMap.get( id );
            events.add( event );
        } else {
            Collection<AuditEvent> events = new ArrayList<>();
            events.add( event );
            eventMap.put( id, events );
        }
    }

    /**
     * Attaches all curation events to the given value object.
     * Note that the events can still be null, as the value-change events they represent might have not occurred since
     * the curatable object was created, and they are not initialised with a creation event.
     *
     * @param vo the value object that needs its curation events populated.
     */
    protected void addCurationEvents( AbstractCuratableValueObject vo ) {
        Long id = vo.getId();
        Curatable curatable = this.load( id );
        CurationDetails details = curatable.getCurationDetails();
        if ( details.getLastNoteUpdateEvent() != null ) {
            vo.setLastNoteUpdateEvent( new AuditEventValueObject( details.getLastNoteUpdateEvent() ) );
        }
        if ( details.getLastNeedsAttentionEvent() != null ) {
            vo.setLastNeedsAttentionEvent( new AuditEventValueObject( details.getLastNeedsAttentionEvent() ) );
        }
        if ( details.getLastTroubledEvent() != null ) {
            vo.setLastTroubledEvent( new AuditEventValueObject( details.getLastTroubledEvent() ) );
        }
    }


    /* ********************************
     * Private methods
     * ********************************/

    private Collection<C> findByParam( String paramName, String paramValue ) {
        Query query = this.getSessionFactory().getCurrentSession().createQuery(
                "from " + elementClass.getSimpleName() + " a where a." + paramName + "=:" + paramName + "" )
                .setParameter( paramName, paramValue );

        //noinspection unchecked
        return query.list();
    }

}
