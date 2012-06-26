package ubic.gemma.script.framework
import ubic.gemma.util.SpringContextUtil
import ubic.gemma.security.authentication.ManualAuthenticationService
import java.util.concurrent.atomic.AtomicBoolean

class SpringSupport {

    private ctx

    SpringSupport() {
        this(null, null)
    }

    SpringSupport(String userName, String password) {

        def b = new AtomicBoolean(false);
        System.err.print "Loading Spring context "
        def t = Thread.start {
            while(!b.get()) {
                sleep 1000
                System.err.print '.'
            }
            System.err.println 'Ready'
        }

        ctx = SpringContextUtil.getApplicationContext();
        b.set(true)
        t.join()

        ManualAuthenticationService manAuthentication = ( ManualAuthenticationService ) ctx.getBean( "manualAuthenticationService" );

        if (userName == null && password == null) {
            manAuthentication.authenticateAnonymously()
        } else {
            def success = manAuthentication.validateRequest( userName, password )
            if ( !success ) {
                throw( "Not authenticated. Make sure you entered a valid username (got '" + userName + "') and password" )
            } else {
                println( "Logged in as " + userName )
            }
        }
    }

    def getBean(String beanName) {
        return ctx.getBean(beanName)
    }

    def shutdown() {
        ctx.close();
    }
}