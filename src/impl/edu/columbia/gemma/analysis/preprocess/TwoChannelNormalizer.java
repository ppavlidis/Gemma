/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.analysis.preprocess;

import baseCode.dataStructure.matrix.DoubleMatrixNamed;

/**
 * Interface representing a mechanism for normalizing two-color arrays.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public interface TwoChannelNormalizer {

    /**
     * @param channelOneSignal
     * @param channelTwoSignal
     * @param channelOneBackground
     * @param channelTwoBackground
     * @param weights Allows different data points to have different weights in the normalization algorithm.
     * @return
     */
    public DoubleMatrixNamed normalize( DoubleMatrixNamed channelOneSignal, DoubleMatrixNamed channelTwoSignal,
            DoubleMatrixNamed channelOneBackground, DoubleMatrixNamed channelTwoBackground, DoubleMatrixNamed weights );

    /**
     * Normalization without consideration of background or weights.
     * 
     * @param channelOneSignal
     * @param channelTwoSignal
     * @return
     */
    public DoubleMatrixNamed normalize( DoubleMatrixNamed channelOneSignal, DoubleMatrixNamed channelTwoSignal );

}
