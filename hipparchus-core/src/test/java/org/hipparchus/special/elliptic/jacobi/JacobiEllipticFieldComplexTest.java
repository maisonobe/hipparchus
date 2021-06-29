/*
 * Licensed to the Hipparchus project under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The Hipparchus project licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hipparchus.special.elliptic.jacobi;

import java.io.IOException;

import org.hipparchus.complex.FieldComplex;
import org.hipparchus.dfp.Dfp;
import org.hipparchus.dfp.DfpField;
import org.junit.Assert;
import org.junit.Test;

public class JacobiEllipticFieldComplexTest {

    @Test
    public void testComplex() throws IOException {
        final DfpField field = new DfpField(30);
        FieldJacobiElliptic<FieldComplex<Dfp>> je = JacobiEllipticBuilder.build(new FieldComplex<>(field.newDfp("0.3"), field.newDfp("1.0")));
        final FieldComplex<Dfp> z = new FieldComplex<>(field.newDfp("5.2"), field.newDfp("-2.5"));
        final FieldCopolarC<FieldComplex<Dfp>> valuesC = je.valuesC(z);
        Assert.assertEquals(-0.24609405083573348938, valuesC.sc().getRealPart().getReal(),      1.0e-15);
        Assert.assertEquals( 0.74202229271111558523, valuesC.sc().getImaginaryPart().getReal(), 1.0e-15);
    }

}
