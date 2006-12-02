/**
 * 
 */
package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.CompressedNamedBitMatrix;
import ubic.basecode.dataStructure.matrix.CompressedSparseDoubleMatrix2DNamed;
import ubic.basecode.util.StringUtil;
import ubic.gemma.analysis.linkAnalysis.MetaLinkFinder;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * @author xwan
 *
 */
public class MetaLinkFinderCli extends AbstractSpringAwareCLI {

	/* (non-Javadoc)
	 * @see ubic.gemma.util.AbstractCLI#buildOptions()
	 */
    private ExpressionExperimentService eeService;
    private boolean operRead = false, operWrite = false;
    private String matrixFile = null, eeMapFile = null;
	@Override
	protected void buildOptions() {
		// TODO Auto-generated method stub
        Option write = OptionBuilder.withDescription("If Saving the link into File").withLongOpt("write").create('w');
        addOption(write);
        Option read = OptionBuilder.withDescription("If Saving the link into File").withLongOpt("read").create('r');
        addOption(read);
        Option matrixFile = OptionBuilder.hasArg().withArgName("Bit Matrixfile").isRequired().withDescription("The file for savming bit matrix")
        .withLongOpt("matrixfile").create('m');
        addOption(matrixFile);

        Option mapFile = OptionBuilder.hasArg().withArgName("Expression Experiment Map File").isRequired().withDescription("The File for Savming the Expression Experiment Mapping").withLongOpt(
        "mapfile").create('e');
        addOption(mapFile);

	}
	protected void processOptions() {
		super.processOptions();
        if (hasOption('w')) {
            this.operWrite = true;
        }

        if (hasOption('r')) {
            this.operRead = true;
        }

        if (hasOption('m')) {
            this.matrixFile = getOptionValue('m');
        }
        
        if (hasOption('e')) {
            this.eeMapFile = getOptionValue('e');
        }
	}
	
	
	private Collection <Gene> getGenes(GeneService geneService){
		HashSet<Gene> genes = new HashSet();
		Gene gene = geneService.load(461722);
		genes.add(gene);
		return genes;
	}
	
	private Gene getGene(GeneService geneService, String geneName, Taxon taxon){
		Gene gene = Gene.Factory.newInstance();
		gene.setOfficialSymbol(geneName.trim());
		gene.setTaxon(taxon);
		gene = geneService.find(gene);
		return gene;
	}
	private Taxon getTaxon(String name){
		Taxon taxon = Taxon.Factory.newInstance();
		taxon.setCommonName(name);
		TaxonService taxonService = (TaxonService)this.getBean("taxonService");
		taxon = taxonService.find(taxon);
		if(taxon == null){
			log.info("NO Taxon found!");
		}
		return taxon;
	}
    private void test(){
        CompressedNamedBitMatrix matrix = new CompressedNamedBitMatrix(21, 11, 125);
        for(int i = 0; i < 21; i++)
            matrix.addRowName( new Long(i) );
        for(int i = 0; i < 11; i++)
            matrix.addColumnName( new Long(i) );
        matrix.set( 0,0,0);
        matrix.set( 0,0,12);
        matrix.set( 0,0,24);
        matrix.set( 20,0,0);
        matrix.set( 20,0,12);
        matrix.set( 20,0,24);
        matrix.set( 0,10,0);
        matrix.set( 0,10,12);
        matrix.set( 0,10,24);
        matrix.set( 20,10,0);
        matrix.set( 20,10,12);
        matrix.set( 20,10,24);
        matrix.toFile( "test.File" );
    }
	/* (non-Javadoc)
	 * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
	 */
	@Override
	protected Exception doWork(String[] args) {
		// TODO Auto-generated method stub
		Exception err = processCommandLine("Link Analysis Data Loader", args);
		if (err != null) {
			return err;
		}
		try {
			Probe2ProbeCoexpressionService p2pService = (Probe2ProbeCoexpressionService) this.getBean("probe2ProbeCoexpressionService");

			DesignElementDataVectorService deService = (DesignElementDataVectorService) this.getBean("designElementDataVectorService");

			eeService = (ExpressionExperimentService) this.getBean("expressionExperimentService");			
			
			GeneService geneService = (GeneService) this.getBean("geneService");
			
			MetaLinkFinder linkFinder = new MetaLinkFinder(p2pService, deService, eeService, geneService);
			
			Taxon taxon = this.getTaxon("human");
			
//            test();
//            linkFinder.fromFile( "test.File", "test.map");
//            linkFinder.toFile( "test1.File", "test1.map");
//            if(true)return null;
			if(this.operWrite){
			    /*
			    Collection<ExpressionExperiment> ees = null;
			    if(taxon != null)
			        ees = eeService.getByTaxon(taxon);
			    else
			        ees = eeService.loadAll();
			    if(ees == null || ees.size() == 0){
			        log.info("No Expression Experiment is found");
			        return null;
			    }
			    else
			        log.info("Found " + ees.size() + " Expression Experiment");

		        linkFinder.find(this.getGenes(geneService), ees);
		        */
			    linkFinder.find(taxon);
                if(!linkFinder.toFile( this.matrixFile, this.eeMapFile )){
                    log.info( "Couldn't save the results into the files ");
                    return null;
                }
			    
            }
            else{
                if(!linkFinder.fromFile(  this.matrixFile, this.eeMapFile )){
                    log.info( "Couldn't load the data from the files ");
                    return null;
                }
            }
		    BufferedReader bfr = new BufferedReader(new InputStreamReader(System.in)); 
		    String geneName;
		    linkFinder.outputStat();
		    int count = 0;
		    try {
		         // Hit CTRL-Z on PC's to send EOF, CTRL-D on Unix 
		    	while ( true ) {
		           // Read a character from keyboard
		    		System.out.println("The Gene ID: (Press CTRL-Z or CTRL-D to Stop)");
		    		System.out.print(">");
		            geneName = bfr.readLine();
		            if(geneName == null) break;
		            System.out.print("The Stringency:");
		            String tmp = bfr.readLine();
		            if(tmp == null) break;
		            count = Integer.valueOf(tmp.trim()).intValue();
		            //Gene gene = geneService.load(Long.valueOf(geneName).longValue());
		            Gene gene = this.getGene(geneService, geneName, taxon);
		            if(gene != null){
		            	System.out.println("Got "+ geneName + " " + count);
		            	linkFinder.output(gene, count);
		    		}
		            else
		            	System.out.println("Gene doesn't exist");
		         }
		       } catch (IOException ioe) {
		           System.out.println( "IO error:" + ioe );
		       } 
		} catch (Exception e) {
			log.error(e);
			return e;
		}
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MetaLinkFinderCli linkFinderCli = new MetaLinkFinderCli();
		StopWatch watch = new StopWatch();
		watch.start();
		try {
			Exception ex = linkFinderCli.doWork(args);
			if (ex != null) {
				ex.printStackTrace();
			}
			watch.stop();
			log.info(watch.getTime());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
