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
package ubic.gemma.core.datastructure.matrix;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.text.NumberFormat;
import java.util.*;

/**
 * A data structure that holds a reference to the data for a given expression experiment. The data can be queried by row
 * or column, returning data for a specific DesignElement or data for a specific BioAssay. This class is not database
 * aware so the vectors provided must already be 'thawed'.
 *
 * @author pavlidis
 * @author keshav
 * @version $Id$
 */
public class ExpressionDataDoubleMatrix extends BaseExpressionDataMatrix<Double> {

    private static final int MAX_ROWS_TO_STRING = 200;
    private static final long serialVersionUID = 1L;
    private static final Log log = LogFactory.getLog( ExpressionDataDoubleMatrix.class.getName() );
    private DoubleMatrix<CompositeSequence, BioMaterial> matrix;

    private Map<CompositeSequence, Double> ranks = new HashMap<>();

    /**
     * To comply with bean specifications. Not to be instantiated.
     */
    public ExpressionDataDoubleMatrix() {
    }

    public ExpressionDataDoubleMatrix( Collection<? extends DesignElementDataVector> vectors ) {
        init();

        for ( DesignElementDataVector dedv : vectors ) {
            if ( !dedv.getQuantitationType().getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
                throw new IllegalStateException(
                        "Cannot convert non-double quantitation types into double matrix:" + dedv
                                .getQuantitationType() );
            }
        }

        selectVectors( vectors );
        vectorsToMatrix( vectors );
    }

    public ExpressionDataDoubleMatrix( Collection<? extends DesignElementDataVector> dataVectors,
            Collection<QuantitationType> quantitationTypes ) {
        init();
        for ( QuantitationType qt : quantitationTypes ) {
            if ( !qt.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
                throw new IllegalStateException(
                        "Cannot convert non-double quantitation types into double matrix: " + qt );
            }
        }
        Collection<DesignElementDataVector> selectedVectors = selectVectors( dataVectors, quantitationTypes );
        vectorsToMatrix( selectedVectors );
    }

    public ExpressionDataDoubleMatrix( Collection<? extends DesignElementDataVector> dataVectors,
            QuantitationType quantitationType ) {
        init();
        if ( !quantitationType.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
            throw new IllegalStateException(
                    "Cannot convert non-double quantitation types into double matrix: " + quantitationType );
        }
        Collection<DesignElementDataVector> selectedVectors = selectVectors( dataVectors, quantitationType );
        vectorsToMatrix( selectedVectors );
    }

    /**
     * Create a data matrix like sourceMatrix but use the values from dataMatrix.
     *
     * @param dataMatrix - The rows can be different than the original matrix, but the columns must be the same.
     */
    public ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix,
            DoubleMatrix<CompositeSequence, BioMaterial> dataMatrix ) {
        init();
        this.expressionExperiment = sourceMatrix.expressionExperiment;
        this.bioAssayDimensions = sourceMatrix.bioAssayDimensions;
        this.columnAssayMap = sourceMatrix.columnAssayMap;
        this.columnBioAssayMapByInteger = sourceMatrix.columnBioAssayMapByInteger;
        this.columnBioMaterialMap = sourceMatrix.columnBioMaterialMap;
        this.columnBioMaterialMapByInteger = sourceMatrix.columnBioMaterialMapByInteger;
        this.quantitationTypes = sourceMatrix.quantitationTypes;
        this.matrix = dataMatrix;

        for ( int i = 0; i < dataMatrix.rows(); i++ ) {
            this.addToRowMaps( i, dataMatrix.getRowName( i ) );
        }

    }

    /**
     * Create a matrix based on another one's selected rows.
     */
    public ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix, List<CompositeSequence> rowsToUse ) {
        init();
        this.expressionExperiment = sourceMatrix.expressionExperiment;
        this.bioAssayDimensions = sourceMatrix.bioAssayDimensions;
        this.columnAssayMap = sourceMatrix.columnAssayMap;
        this.columnBioAssayMapByInteger = sourceMatrix.columnBioAssayMapByInteger;
        this.columnBioMaterialMap = sourceMatrix.columnBioMaterialMap;
        this.columnBioMaterialMapByInteger = sourceMatrix.columnBioMaterialMapByInteger;
        this.quantitationTypes = sourceMatrix.getQuantitationTypes();
        this.matrix = new DenseDoubleMatrix<>( rowsToUse.size(), sourceMatrix.columns() );
        this.matrix.setColumnNames( sourceMatrix.getMatrix().getColNames() );

        log.debug( "Creating a filtered matrix " + rowsToUse.size() + " x " + sourceMatrix.columns() );

        int i = 0;
        for ( CompositeSequence element : rowsToUse ) {
            super.addToRowMaps( i, element );
            Double[] rowVals = sourceMatrix.getRow( element );
            assert rowVals != null : "Source matrix does not have row for " + element;

            this.matrix.addRowName( element );

            for ( int j = 0; j < rowVals.length; j++ ) {
                Double val = rowVals[j];
                set( i, j, val );
            }
            i++;
        }
    }

    /**
     * Create a matrix given a 'raw' matrix that uses the same samples as the experiment. Only simple situations are
     * supported (one platform, not subsetting the dataset).
     *
     * @param ee to be associated with this
     * @param qt to be associated with this
     * @param matrix with valid row and column elements, and the data
     */
    public ExpressionDataDoubleMatrix( ExpressionExperiment ee, QuantitationType qt,
            DoubleMatrix<CompositeSequence, BioMaterial> matrix ) {

        if ( ee == null ) {
            throw new IllegalArgumentException( "Experiment cannot be null" );
        }

        if ( matrix.rows() == 0 || matrix.columns() == 0 || matrix.getRowNames().isEmpty() || matrix.getColNames()
                .isEmpty() ) {
            throw new IllegalArgumentException( "Matrix is invalid" );
        }

        init();
        this.expressionExperiment = ee;
        this.matrix = matrix;
        this.quantitationTypes.add( qt );

        BioAssayDimension dim = BioAssayDimension.Factory.newInstance();

        List<BioAssay> bioassays = new ArrayList<>();
        for ( BioMaterial bm : matrix.getColNames() ) {
            Collection<BioAssay> bioAssaysUsedIn = bm.getBioAssaysUsedIn();
            if ( bioAssaysUsedIn.size() > 1 ) {
                throw new UnsupportedOperationException(
                        "Can't make new data from matrix that has multiple bioassays per biomaterial" );
            }

            BioAssay bioAssay = bioAssaysUsedIn.iterator().next();

            if ( !ee.getBioAssays().contains( bioAssay ) ) {
                throw new IllegalArgumentException( "Bioassays in the matrix must match those in the experiment" );
            }

            bioassays.add( bioAssay );

        }

        if ( bioassays.size() != ee.getBioAssays().size() ) {
            throw new IllegalArgumentException( "All bioassays in the experiment must be used in the matrix" );
        }

        dim.setBioAssays( bioassays );
        dim.setDescription( "Built from matrix supplied to Constructor for " + ee + " from matrix" );
        dim.setName( StringUtils.abbreviate( "For " + ee.getShortName() + " from matrix", 255 ) );

        assert !matrix.getRowNames().isEmpty();
        int i = 0;
        for ( CompositeSequence cs : matrix.getRowNames() ) {
            bioAssayDimensions.put( cs, dim );
            this.addToRowMaps( i, cs );
            i++;
        }

        assert !bioAssayDimensions.isEmpty();

        setUpColumnElements();

    }

    /**
     * Create a matrix based on another one's selected columns. The results will be somewhat butchered - only a single
     * BioAssayDimension and the ranks will be copied over (not recomputed based on the selected columns).
     */
    public ExpressionDataDoubleMatrix( List<BioMaterial> columnsToUse, ExpressionDataDoubleMatrix sourceMatrix ) {
        init();
        this.expressionExperiment = sourceMatrix.expressionExperiment;

        this.matrix = new DenseDoubleMatrix<>( sourceMatrix.rows(), columnsToUse.size() );
        this.matrix.setRowNames( sourceMatrix.getMatrix().getRowNames() );

        this.ranks = sourceMatrix.ranks; // not strictly correct if we are using subcolumns

        /*
         * Indices of the biomaterials in the original matrix.
         */
        List<Integer> originalBioMaterialIndices = new ArrayList<>();

        List<BioAssay> bioAssays = new ArrayList<>();
        for ( BioMaterial bm : columnsToUse ) {
            originalBioMaterialIndices.add( sourceMatrix.getColumnIndex( bm ) );
            bioAssays.add( bm.getBioAssaysUsedIn().iterator().next() );
        }

        this.matrix.setColumnNames( columnsToUse );

        /*
         * fix the upper level column name maps.
         */
        BioAssayDimension reorderedDim = BioAssayDimension.Factory.newInstance();
        reorderedDim.setBioAssays( bioAssays );

        this.bioAssayDimensions.clear();

        reorderedDim.setName( "Slice" );

        this.getQuantitationTypes().addAll( sourceMatrix.getQuantitationTypes() );

        int i = 0;
        for ( ExpressionDataMatrixRowElement element : sourceMatrix.getRowElements() ) {
            CompositeSequence designElement = element.getDesignElement();
            super.addToRowMaps( i, designElement );

            Double[] sourceRow = sourceMatrix.getRow( designElement );

            assert sourceRow != null : "Source matrix does not have row for " + designElement;

            for ( int j = 0; j < originalBioMaterialIndices.size(); j++ ) {
                Double val = sourceRow[originalBioMaterialIndices.get( j )];
                set( i, j, val );
            }
            i++;

            this.bioAssayDimensions.put( designElement, reorderedDim );

        }

        super.setUpColumnElements();

    }

    @Override
    public int columns() {
        return matrix.columns();
    }

    /**
     * Convert this to a collection of vectors.
     */
    public Collection<ProcessedExpressionDataVector> toProcessedDataVectors() {
        Collection<ProcessedExpressionDataVector> result = new HashSet<>();
        QuantitationType qt = this.getQuantitationTypes().iterator().next();

        ByteArrayConverter bac = new ByteArrayConverter();
        if ( this.getQuantitationTypes().size() > 1 ) {
            throw new UnsupportedOperationException( "Cannot convert matrix that has more than one quantitation type" );
        }

        for ( int i = 0; i < this.rows(); i++ ) {

            Double[] data = this.getRow( i );

            ProcessedExpressionDataVector v = ProcessedExpressionDataVector.Factory.newInstance();
            v.setBioAssayDimension( this.getBestBioAssayDimension() );
            v.setDesignElement( this.getRowNames().get( i ) );
            v.setQuantitationType( qt );
            v.setData( bac.doubleArrayToBytes( data ) );
            v.setExpressionExperiment( this.expressionExperiment );
            // we don't fill in the ranks because we only have the mean value here.

            result.add( v );
        }

        return result;
    }

    @Override
    public Double get( CompositeSequence designElement, BioAssay bioAssay ) {
        Integer i = this.rowElementMap.get( designElement );
        Integer j = this.columnAssayMap.get( bioAssay );
        if ( i == null || j == null ) {
            log.warn( "No matrix element for " + designElement + ", " + bioAssay );
            return null;
        }
        return this.matrix.get( i, j );
    }

    @Override
    public Double get( int row, int column ) {
        return matrix.get( row, column );
    }

    @Override
    public Double[][] get( List<CompositeSequence> designElements, List<BioAssay> bioAssays ) {
        throw new UnsupportedOperationException( "Sorry, not implemented yet" );
    }

    @Override
    public Double[] getColumn( BioAssay bioAssay ) {
        int index = this.columnAssayMap.get( bioAssay );

        return this.getColumn( index );
    }

    @Override
    public Double[] getColumn( Integer index ) {
        double[] rawResult = this.matrix.getColumn( index );
        assert rawResult != null;
        Double[] result = new Double[rawResult.length];
        for ( int i = 0; i < rawResult.length; i++ ) {
            result[i] = rawResult[i];
        }
        return result;
    }

    @Override
    public Double[][] getColumns( List<BioAssay> bioAssays ) {
        throw new UnsupportedOperationException( "Sorry, not implemented yet" );
    }

    public DoubleMatrix<CompositeSequence, BioMaterial> getMatrix() {
        return matrix;
    }

    /**
     * @return The expression level ranks (based on mean signal intensity in the vectors); this will be empty if the
     *         vectors used to construct the matrix were not ProcessedExpressionDataVectors.
     */
    public Map<CompositeSequence, Double> getRanks() {
        return this.ranks;
    }

    @Override
    public Double[][] getRawMatrix() {

        Double[][] dMatrix = new Double[matrix.rows()][matrix.columns()];
        for ( int i = 0; i < matrix.rows(); i++ ) {
            Double[] row = matrix.getRowObj( i );
            dMatrix[i] = row;
        }

        return dMatrix;
    }

    @Override
    public Double[] getRow( CompositeSequence designElement ) {
        Integer row = this.rowElementMap.get( designElement );
        if ( row == null )
            return null;
        return getRow( row );
    }

    public double[] getRawRow( Integer index ) {
        return matrix.getRow( index );
    }

    @Override
    public Double[] getRow( Integer index ) {
        double[] rawRow = matrix.getRow( index );
        return ArrayUtils.toObject( rawRow );
    }

    public List<CompositeSequence> getRowNames() {
        return this.getMatrix().getRowNames();
    }

    @Override
    public Double[][] getRows( List<CompositeSequence> designElements ) {
        if ( designElements == null ) {
            return null;
        }

        Double[][] result = new Double[designElements.size()][];
        int i = 0;
        for ( CompositeSequence element : designElements ) {
            Double[] rowResult = getRow( element );
            result[i] = rowResult;
            i++;
        }
        return result;
    }

    @Override
    public int rows() {
        return matrix.rows();
    }

    public void set( CompositeSequence designElement, BioAssay bioAssay, Double value ) {
        int row = this.getRowIndex( designElement );
        int column = this.getColumnIndex( bioAssay );
        matrix.set( row, column, value );
    }

    @Override
    public void set( int row, int column, Double value ) {
        if ( value == null ) {
            matrix.set( row, column, Double.NaN );
        } else {
            matrix.set( row, column, value );
        }
    }

    /**
     * Sets the row of matrix to the input data.
     *
     * @param rowIndex The row index of the data in the matrix to be replaced.
     * @param data The input data.
     */
    public void setRow( int rowIndex, Double[] data ) {
        if ( rowIndex > this.matrix.rows() ) {
            throw new RuntimeException(
                    "Specified row index " + rowIndex + " is larger than the matrix of size " + this.matrix.rows()
                            + "." );
        }

        for ( int j = 0; j < data.length; j++ ) {
            this.matrix.set( rowIndex, j, data[j] );
        }
    }

    @Override
    public String toString() {
        int columns = this.columns();
        int rows = this.rows();

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits( 4 );

        StringBuilder buf = new StringBuilder();
        if ( rows <= MAX_ROWS_TO_STRING ) {
            buf.append( rows ).append( " x " ).append( columns ).append( " matrix of double values\n" );
        } else {
            buf.append( rows ).append( " x " ).append( columns ).append( " matrix of double values, showing up to " )
                    .append( MAX_ROWS_TO_STRING ).append( " rows\n" );
        }
        int stop = 0;
        buf.append( "Probe" );
        for ( int i = 0; i < columns; i++ ) {
            buf.append( "\t" ).append( this.getBioMaterialForColumn( i ).getName() ).append( ":" );
            for ( BioAssay ba : this.getBioAssaysForColumn( i ) ) {
                buf.append( ba.getName() ).append( "," );
            }
        }
        buf.append( "\n" );

        for ( int j = 0; j < rows; j++ ) {

            buf.append( this.rowDesignElementMapByInteger.get( j ).getName() );
            for ( int i = 0; i < columns; i++ ) {
                Double val = this.get( j, i );
                if ( Double.isNaN( val ) ) {
                    buf.append( "\t" ).append( val );
                } else {
                    buf.append( "\t" ).append( nf.format( this.get( j, i ) ) );
                }
            }

            buf.append( "\n" );

            if ( stop++ > MAX_ROWS_TO_STRING ) {
                buf.append( "\n(Stopping after " + MAX_ROWS_TO_STRING + " rows) ...\n" );
                break;
            }
        }
        return buf.toString();
    }

    /**
     * Convert {@link DesignElementDataVector}s into Double matrix.
     */
    @Override
    protected void vectorsToMatrix( Collection<? extends DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) {
            throw new IllegalArgumentException( "No vectors!" );
        }

        for ( DesignElementDataVector vector : vectors ) {
            if ( vector instanceof ProcessedExpressionDataVector ) {
                this.ranks
                        .put( vector.getDesignElement(), ( ( ProcessedExpressionDataVector ) vector ).getRankByMean() );
            }
        }

        int maxSize = setUpColumnElements();
        this.matrix = createMatrix( vectors, maxSize );

    }

    /**
     * Fill in the data
     *
     * @return DoubleMatrixNamed
     */
    private DoubleMatrix<CompositeSequence, BioMaterial> createMatrix(
            Collection<? extends DesignElementDataVector> vectors, int maxSize ) {

        int numRows = this.rowDesignElementMapByInteger.keySet().size();

        DoubleMatrix<CompositeSequence, BioMaterial> mat = new DenseDoubleMatrix<>( numRows, maxSize );

        for ( int j = 0; j < mat.columns(); j++ ) {
            mat.addColumnName( this.getBioMaterialForColumn( j ) );
        }

        // initialize the matrix to -Infinity; this marks values that are not yet initialized.
        for ( int i = 0; i < mat.rows(); i++ ) {
            for ( int j = 0; j < mat.columns(); j++ ) {
                mat.set( i, j, Double.NEGATIVE_INFINITY );
            }
        }

        ByteArrayConverter bac = new ByteArrayConverter();

        Map<Integer, CompositeSequence> rowNames = new TreeMap<>();
        for ( DesignElementDataVector vector : vectors ) {
            BioAssayDimension dimension = vector.getBioAssayDimension();
            byte[] bytes = vector.getData();

            CompositeSequence designElement = vector.getDesignElement();
            assert designElement != null : "No design element for " + vector;

            Integer rowIndex = this.rowElementMap.get( designElement );
            assert rowIndex != null;

            rowNames.put( rowIndex, designElement );

            double[] vals = bac.byteArrayToDoubles( bytes );

            Collection<BioAssay> bioAssays = dimension.getBioAssays();
            if ( bioAssays.size() != vals.length )
                throw new IllegalStateException(
                        "Mismatch: " + vals.length + " values in vector ( " + bytes.length + " bytes) for "
                                + designElement + " got " + bioAssays.size() + " bioassays in the bioAssayDimension" );

            Iterator<BioAssay> it = bioAssays.iterator();

            setMatBioAssayValues( mat, rowIndex, ArrayUtils.toObject( vals ), bioAssays, it );
        }

        /*
         * Note: these row names aren't that important unless we use the bare matrix.
         */
        for ( int i = 0; i < mat.rows(); i++ ) {
            mat.addRowName( rowNames.get( i ) );
        }
        assert mat.getRowNames().size() == mat.rows();

        // fill in remaining missing values.
        for ( int i = 0; i < mat.rows(); i++ ) {
            for ( int j = 0; j < mat.columns(); j++ ) {
                if ( mat.get( i, j ) == Double.NEGATIVE_INFINITY ) {
                    // log.debug( "Missing value at " + i + " " + j );
                    mat.set( i, j, Double.NaN );
                }
            }
        }
        log.debug( "Created a " + mat.rows() + " x " + mat.columns() + " matrix" );
        return mat;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix#hasMissingValues()
     */
    @Override
    public boolean hasMissingValues() {
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                if ( Double.isNaN( matrix.get( i, j ) ) ) return true;
            }
        }
        return false;
    }

}