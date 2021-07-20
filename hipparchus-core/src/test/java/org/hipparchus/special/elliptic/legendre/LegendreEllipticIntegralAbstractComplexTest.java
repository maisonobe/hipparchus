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
package org.hipparchus.special.elliptic.legendre;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Test;

public abstract class LegendreEllipticIntegralAbstractComplexTest<T extends CalculusFieldElement<T>> {

    protected abstract T buildComplex(double realPart);
    protected abstract T buildComplex(double realPart, double imaginaryPart);
    protected abstract T K(T k);
    protected abstract T Kprime(T k);
    protected abstract T F(T phi, T k);
    protected abstract T E(T k);
    protected abstract T E(T phi, T k);
    protected abstract T D(T k);
    protected abstract T D(T phi, T k);
    protected abstract T Pi(T alpha2, T k);
    protected abstract T Pi(T phi, T alpha2, T k);

    private void check(double expectedReal, double expectedImaginary, T result, double tol) {
        Assert.assertEquals(0, buildComplex(expectedReal, expectedImaginary).subtract(result).norm(), tol);
    }

    @Test
    public void testNoConvergence() {
        Assert.assertTrue(LegendreEllipticIntegral.bigK(buildComplex(Double.NaN)).isNaN());
    }

    @Test
    public void testComplementary() {
        for (double m = 0.01; m < 1; m += 0.01) {
            T k1 = LegendreEllipticIntegral.bigK(buildComplex(m));
            T k2 = LegendreEllipticIntegral.bigKPrime(buildComplex(1 - m));
            Assert.assertEquals(k1.getReal(), k2.getReal(), FastMath.ulp(k1).getReal());
        }
    }

    @Test
    public void testAbramowitzStegunExample3() {
        T k = LegendreEllipticIntegral.bigK(buildComplex(80.0 / 81.0));
        Assert.assertEquals(3.591545001, k.getReal(), 2.0e-9);
    }

    public void testAbramowitzStegunExample4() {
        check(1.019106060, 0.0, LegendreEllipticIntegral.bigE(buildComplex(80.0 / 81.0)), 2.0e-8);
    }

    @Test
    public void testAbramowitzStegunExample8() {
        final T m    = buildComplex(1.0 / 5.0);
        check(1.115921, 0.0, LegendreEllipticIntegral.bigF(buildComplex(FastMath.acos(FastMath.sqrt(2) / 3.0)), m), 1.0e-6);
        check(0.800380, 0.0, LegendreEllipticIntegral.bigF(buildComplex(FastMath.acos(FastMath.sqrt(2) / 2.0)), m), 1.0e-6);
    }

    @Test
    public void testAbramowitzStegunExample9() {
        final T m    = buildComplex(1.0 / 2.0);
        check(1.854075, 0.0, LegendreEllipticIntegral.bigF(buildComplex(MathUtils.SEMI_PI), m), 1.0e-6);
        check(0.535623, 0.0, LegendreEllipticIntegral.bigF(buildComplex(FastMath.PI / 6.0), m), 1.0e-6);
    }

    @Test
    public void testAbramowitzStegunExample10() {
        final T m    = buildComplex(4.0 / 5.0);
        check(0.543604, 0.0, LegendreEllipticIntegral.bigF(buildComplex(FastMath.PI / 6.0), m), 1.0e-6);
    }

    @Test
    public void testAbramowitzStegunExample14() {
        final T k    = buildComplex(3.0 / 5.0);
        check(0.80904, 0.0, LegendreEllipticIntegral.bigE(buildComplex(FastMath.asin(FastMath.sqrt(5.0) / 3.0)),          k.multiply(k)), 1.0e-5);
        check(0.41192, 0.0, LegendreEllipticIntegral.bigE(buildComplex(FastMath.asin(5.0 / (3.0 * FastMath.sqrt(17.0)))), k.multiply(k)), 1.0e-5);
    }

    @Test
    public void testAbramowitzStegunTable175() {
        final double sinAlpha1 = FastMath.sin(FastMath.toRadians(32));
        check(0.26263487, 0.0, LegendreEllipticIntegral.bigF(buildComplex(FastMath.toRadians(15)), buildComplex(sinAlpha1 * sinAlpha1)), 1.0e-8);
        final double sinAlpha2 = FastMath.sin(FastMath.toRadians(46));
        check(1.61923762, 0.0, LegendreEllipticIntegral.bigF(buildComplex(FastMath.toRadians(80)), buildComplex(sinAlpha2 * sinAlpha2)), 1.0e-8);
    }

    @Test
    public void testAbramowitzStegunTable176() {
        final double sinAlpha1 = FastMath.sin(FastMath.toRadians(64));
        check(0.42531712, 0.0, LegendreEllipticIntegral.bigE(buildComplex(FastMath.toRadians(25)), buildComplex(sinAlpha1 * sinAlpha1)), 1.0e-8);
        final double sinAlpha2 = FastMath.sin(FastMath.toRadians(76));
        check(0.96208074, 0.0, LegendreEllipticIntegral.bigE(buildComplex(FastMath.toRadians(70)), buildComplex(sinAlpha2 * sinAlpha2)), 1.0e-8);
    }

    @Test
    public void testAbramowitzStegunTable179() {
        final double sinAlpha1 = FastMath.sin(FastMath.toRadians(15));
        check(1.62298, 0.0, LegendreEllipticIntegral.bigPi(buildComplex(FastMath.toRadians(75)), buildComplex(0.4), buildComplex(sinAlpha1 * sinAlpha1)), 1.0e-5);
        final double sinAlpha2 = FastMath.sin(FastMath.toRadians(60));
        check(1.03076, 0.0, LegendreEllipticIntegral.bigPi(buildComplex(FastMath.toRadians(45)), buildComplex(0.8), buildComplex(sinAlpha2 * sinAlpha2)), 1.0e-5);
        final double sinAlpha3 = FastMath.sin(FastMath.toRadians(15));
        check(2.79990, 0.0, LegendreEllipticIntegral.bigPi(buildComplex(FastMath.toRadians(75)), buildComplex(0.9), buildComplex(sinAlpha3 * sinAlpha3)), 1.0e-5);
    }

    @Test
    public void testCompleteVsIncompleteF() {
        for (double m = 0.01; m < 1; m += 0.01) {
            double complete   = LegendreEllipticIntegral.bigK(buildComplex(m)).getReal();
            double incomplete = LegendreEllipticIntegral.bigF(buildComplex(MathUtils.SEMI_PI),
                                                              buildComplex(m)).getReal();
            Assert.assertEquals(complete, incomplete, FastMath.ulp(complete));
        }
    }

    @Test
    public void testCompleteVsIncompleteE() {
        for (double m = 0.01; m < 1; m += 0.01) {
            double complete   = LegendreEllipticIntegral.bigE(buildComplex(m)).getReal();
            double incomplete = LegendreEllipticIntegral.bigE(buildComplex(MathUtils.SEMI_PI),
                                                              buildComplex(m)).getReal();
            Assert.assertEquals(complete, incomplete, 4 * FastMath.ulp(complete));
        }
    }

    @Test
    public void testCompleteVsIncompleteD() {
        for (double m = 0.01; m < 1; m += 0.01) {
            double complete   = LegendreEllipticIntegral.bigD(buildComplex(m)).getReal();
            double incomplete = LegendreEllipticIntegral.bigD(buildComplex(MathUtils.SEMI_PI),
                                                              buildComplex(m)).getReal();
            Assert.assertEquals(complete, incomplete, FastMath.ulp(complete));
        }
    }

    @Test
    public void testCompleteVsIncompletePi() {
        for (double alpha2 = 0.01; alpha2 < 1; alpha2 += 0.01) {
            for (double m = 0.01; m < 1; m += 0.01) {
                double complete   = LegendreEllipticIntegral.bigPi(buildComplex(alpha2),
                                                                   buildComplex(m)).getReal();
                double incomplete = LegendreEllipticIntegral.bigPi(buildComplex(MathUtils.SEMI_PI),
                                                                   buildComplex(alpha2),
                                                                   buildComplex(m)).getReal();
                Assert.assertEquals(complete, incomplete, FastMath.ulp(complete));
            }
        }
    }

}
