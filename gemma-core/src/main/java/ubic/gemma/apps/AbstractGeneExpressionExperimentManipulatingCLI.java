/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Base class for CLIs that need an expression experiment as an input.
 * 
 * @author Paul
 * @version $Id: AbstractGeneExpressionExperimentManipulatingCLI.java,v 1.2
 *          2007/08/21 22:07:39 raymond Exp $
 */
public abstract class AbstractGeneExpressionExperimentManipulatingCLI extends
		AbstractSpringAwareCLI {

	protected ExpressionExperimentService eeService;
	protected GeneService geneService;
	private String experimentShortName = null;
	private String excludeEeFileName;
	protected String experimentListFile = null;

	@SuppressWarnings("static-access")
	protected void buildOptions() {
		Option expOption = OptionBuilder
				.hasArg()
				.withArgName("Expression experiment")
				.withDescription(
						"Expression experiment short name. Most tools recognize comma-delimited values given on the command line, "
								+ "and if this option is omitted, the tool will be applied to all expression experiments.")
				.withLongOpt("experiment").create('e');

		addOption(expOption);

		Option geneFileListOption = OptionBuilder
				.hasArg()
				.withArgName("List of gene expression experiments file")
				.withDescription(
						"File with list of short names of expression experiments (one per line; use instead of '-e')")
				.withLongOpt("eeListfile").create('f');
		addOption(geneFileListOption);

		Option excludeEeOption = OptionBuilder.hasArg().withArgName(
				"Experiments to exclude").withDescription(
				"File containing list of expression experiments to exclude")
				.withLongOpt("excludeEEFile").create('x');
		addOption(excludeEeOption);
	}

	/**
	 * @param short
	 *            name of the experiment to find.
	 * @return
	 */
	protected ExpressionExperiment locateExpressionExperiment(String name) {

		if (name == null) {
			errorObjects
					.add("Expression experiment short name must be provided");
			return null;
		}

		ExpressionExperiment experiment = eeService.findByShortName(name);

		if (experiment == null) {
			log.error("No experiment " + name + " found");
			bail(ErrorCode.INVALID_OPTION);
		}
		return experiment;
	}

	@Override
	protected void processOptions() {
		super.processOptions();
		if (this.hasOption('e')) {
			this.experimentShortName = this.getOptionValue('e');
		}
		if (hasOption('f')) {
			this.experimentListFile = getOptionValue('f');
		}
		if (hasOption('x')) {
			excludeEeFileName = getOptionValue('x');
		}
		eeService = (ExpressionExperimentService) this
				.getBean("expressionExperimentService");
		geneService = (GeneService) this.getBean("geneService");
	}

	public String getExperimentShortName() {
		return experimentShortName;
	}

	public Collection<ExpressionExperiment> getExpressionExperiments(Taxon taxon)
			throws IOException {
		Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
		if (experimentShortName != null) {
			ExpressionExperiment ee = eeService
					.findByShortName(experimentShortName);
			if (ee == null)
				log.error("No experiment " + experimentShortName + " found");
			else
				ees.add(ee);
		}
		if (experimentListFile != null) {
			ees.addAll(readExpressionExperimentListFile(experimentListFile));
		}
		if (ees.size() == 0) {
			ees.addAll(eeService.findByTaxon(taxon));
		}
		if (excludeEeFileName != null) {
			Collection<String> excludedEeNames = readExpressionExperimentListFileToStrings(excludeEeFileName);
			int count = 0;
			for (Iterator<ExpressionExperiment> it = ees.iterator(); it
					.hasNext();) {
				ExpressionExperiment ee = it.next();
				if (excludedEeNames.contains(ee.getShortName())) {
					it.remove();
					count++;
				}
			}
			log.info("Excluded " + count + " expression experiments");
		}
		return ees;
	}

	private Collection<String> readExpressionExperimentListFileToStrings(
			String fileName) throws IOException {
		Collection<String> eeNames = new HashSet<String>();
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		while (in.ready()) {
			String eeName = in.readLine().trim();
			if (eeName.startsWith("#")) {
				continue;
			}
			eeNames.add(eeName);
		}
		return eeNames;
	}

	public Collection<ExpressionExperiment> readExpressionExperimentListFile(
			String fileName) throws IOException {
		Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
		for (String eeName : readExpressionExperimentListFileToStrings(fileName)) {
			ExpressionExperiment ee = eeService.findByShortName(eeName);
			if (ee == null) {
				log.error("No experiment " + eeName + " found");
				continue;
			}
			ees.add(ee);
		}
		return ees;
	}

	/**
	 * Read in a list of genes
	 * 
	 * @param inFile -
	 *            file name to read
	 * @param taxon
	 * @param type
	 *            format that gene is in
	 * @return collection of genes
	 * @throws IOException
	 */
	protected Collection<Gene> readGeneListFile(String inFile, Taxon taxon)
			throws IOException {
		log.info("Reading " + inFile);

		Collection<Gene> genes = new ArrayList<Gene>();
		BufferedReader in = new BufferedReader(new FileReader(inFile));
		String line;
		while ((line = in.readLine()) != null) {
			if (line.startsWith("#"))
				continue;
			String s = line.trim();
			Gene gene = findGeneByOfficialSymbol(s, taxon);
			if (gene == null) {
				log.error("ERROR: Cannot find genes for " + s);
				continue;
			}
			genes.add(gene);
		}
		return genes;
	}

	protected Gene findGeneByOfficialSymbol(String symbol, Taxon taxon) {
		Collection<Gene> genes = geneService
				.findByOfficialSymbolInexact(symbol);
		for (Gene gene : genes) {
			if (taxon.equals(gene.getTaxon()))
				return gene;
		}
		return null;
	}

}
