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

package org.hipparchus.filtering.kalman.extended;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hipparchus.filtering.kalman.KalmanEstimator;
import org.hipparchus.filtering.kalman.Measurement;
import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.hipparchus.linear.CholeskyDecomposer;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;

public class ExtendedKalmanEstimatorTest {

    @Test
    public void testConstant() {

        // initial estimate is perfect, and process noise is perfectly known
        final RealMatrix q = MatrixUtils.createRealDiagonalMatrix(new double[] {
            1.0e-5
        });
        final ProcessEstimate initial = new ProcessEstimate(0,
                                                            MatrixUtils.createRealVector(new double[] { 10.0 }),
                                                            q);

        // reference values from Apache Commons Math 3.6.1 unit test
        final List<Reference> referenceData = loadReferenceData(1, 1, "constant-value.txt");
        final Stream<Measurement> measurements =
                        referenceData.stream().
                        map(r -> new Measurement(r.time,
                                                 r.z,
                                                 MatrixUtils.createRealMatrix(new double[][] { { 1.0 } }),
                                                 MatrixUtils.createRealDiagonalMatrix(new double[] { 0.1 })));

        // set up Kalman estimator
        final ExtendedKalmanEstimator estimator =
                        new ExtendedKalmanEstimator(new CholeskyDecomposer(1.0e-15, 1.0e-15),
                                                    (previousTime, previousState, currentTime) ->
                        new NonLinearEvolution(currentTime,
                                               previousState,
                                               MatrixUtils.createRealIdentityMatrix(1),
                                               q),
                                               initial);

        // sequentially process all measurements and check against the reference estimated state and covariance
        estimator.estimate(measurements).forEach(estimate -> {
            for (Reference r : referenceData) {
                if (r.sameTime(estimate.getTime())) {
                    r.checkState(estimate.getState(), 1.0e-15);
                    r.checkcovariance(estimate.getCovariance(), 3.0e-19);
                    return;
                }
            }
        });

    }

    @Test
    public void testConstantAcceleration() {

        final double mNoise  = 10.0;
        final NonLinearProcess process = new ConstantAccelerationProcess(1.0, 0.2);

        // initial state is estimated to be at rest on origin
        final ProcessEstimate initial = new ProcessEstimate(0,
                                                            MatrixUtils.createRealVector(new double[] { 0.0, 0.0 }),
                                                            MatrixUtils.createRealMatrix(new double[][] {
                                                                { 1.0, 1.0 },
                                                                { 1.0, 1.0 }
                                                            }));

        // reference values from Apache Commons Math 3.6.1 unit test
        final List<Reference> referenceData = loadReferenceData(2, 1, "constant-acceleration.txt");
        final Stream<Measurement> measurements =
                        referenceData.stream().
                        map(r -> new Measurement(r.time,
                                                 r.z,
                                                 MatrixUtils.createRealMatrix(new double[][] { { 1.0, 0.0 } }),
                                                 MatrixUtils.createRealDiagonalMatrix(new double[] { mNoise * mNoise })));

        // set up Kalman estimator
        final ExtendedKalmanEstimator estimator =
        new ExtendedKalmanEstimator(new CholeskyDecomposer(1.0e-15, 1.0e-15), process, initial);

        // sequentially process all measurements and check against the reference estimate
        estimator.estimate(measurements).forEach(estimate -> {
            for (Reference r : referenceData) {
                if (r.sameTime(estimate.getTime())) {
                    r.checkState(estimate.getState(), 4.0e-15);
                    r.checkcovariance(estimate.getCovariance(), 4.0e-15);
                    return;
                }
            }
        });

    }

    private static class ConstantAccelerationProcess implements NonLinearProcess {
        private final double a;
        private final double aNoise2;
        
        public ConstantAccelerationProcess(final double a, final double aNoise) {
            this.a       = a;
            this.aNoise2 = aNoise * aNoise;
        }

        public NonLinearEvolution getEvolution(double previousTime, RealVector previousState, double currentTime) {
            final double     dt    = currentTime - previousTime;
            final double     dt2   = dt  * dt;
            final double     dt3   = dt2 * dt;
            final double     dt4   = dt2 * dt2;
            final RealVector state = MatrixUtils.createRealVector(new double[] {
                previousState.getEntry(0) + previousState.getEntry(1) * dt + 0.5 * a * dt * dt,
                previousState.getEntry(1) + a * dt
            });
            final RealMatrix stm = MatrixUtils.createRealMatrix(new double[][] {
                { 1.0,  dt },
                { 0.0, 1.0 }
            });
            final RealMatrix processNoiseMatrix = MatrixUtils.createRealMatrix(new double[][] {
                { 0.25 * dt4 * aNoise2, 0.5 * dt3 * aNoise2 },
                { 0.5  * dt3 * aNoise2, dt2 * aNoise2 }
            });
            return new NonLinearEvolution(currentTime, state, stm, processNoiseMatrix);
        }

    }

    @Test
    public void testCannonballZeroProcessNoise() {
        doTestCannonball(new double[][] {
                            { 0.00, 0.00, 0.00, 0.00 },
                            { 0.00, 0.00, 0.00, 0.00 },
                            { 0.00, 0.00, 0.00, 0.00 },
                            { 0.00, 0.00, 0.00, 0.00 },
                         }, "cannonball-zero-process-noise.txt",
                         5.0e-13, 6.0e-14);
    }

    @Test
    public void testCannonballNonZeroProcessNoise() {
        doTestCannonball(new double[][] {
                            { 0.01, 0.00, 0.00, 0.00 },
                            { 0.00, 0.10, 0.00, 0.00 },
                            { 0.00, 0.00, 0.01, 0.00 },
                            { 0.00, 0.00, 0.00, 0.10 },
                         }, "cannonball-non-zero-process-noise.txt",
                         4.0e-13, 2.0e-13);
    }

    private void doTestCannonball(final double[][] q, final String name,
                                  final double tolState, final double tolCovariance) {

        final double mNoise   = 30.0;
        final double vIni     = 100.0;
        final double alphaIni = FastMath.PI / 4;
        final NonLinearProcess process = new CannonballProcess(9.81, q);

        // initial state is estimated to be a shot from origin with known angle and velocity
        final ProcessEstimate initial = new ProcessEstimate(0.0,
                                                            MatrixUtils.createRealVector(new double[] {
                                                                0.0, vIni * FastMath.cos(alphaIni),
                                                                0.0, vIni * FastMath.sin(alphaIni)
                                                            }),
                                                            MatrixUtils.createRealDiagonalMatrix(new double[] {
                                                                mNoise * mNoise, 1.0e-3, mNoise * mNoise, 1.0e-3
                                                            }));

        // reference values from Apache Commons Math 3.6.1 unit test
        // we have changed the test slightly, setting up a non-zero process noise
        final List<Reference> referenceData = loadReferenceData(4, 2, name);
        final Stream<Measurement> measurements =
                        referenceData.stream().
                        map(r -> new Measurement(r.time,
                                                 r.z,
                                                 MatrixUtils.createRealMatrix(new double[][] {
                                                                                  { 1.0, 0.0, 0.0, 0.0 },
                                                                                  { 0.0, 0.0, 1.0, 0.0 }
                                                                              }),
                                                 MatrixUtils.createRealDiagonalMatrix(new double[] {
                                                     mNoise * mNoise, mNoise * mNoise
                                                 })));

        // set up Kalman estimator
        final ExtendedKalmanEstimator estimator =
        new ExtendedKalmanEstimator(new CholeskyDecomposer(1.0e-15, 1.0e-15), process, initial);

        // sequentially process all measurements and check against the reference estimate
        estimator.estimate(measurements).forEach(estimate -> {
            for (Reference r : referenceData) {
                if (r.sameTime(estimate.getTime())) {
                    r.checkState(estimate.getState(), tolState);
                    r.checkcovariance(estimate.getCovariance(), tolCovariance);
                    return;
                }
            }
        });

    }

    private static class CannonballProcess implements NonLinearProcess {
        private final double g;
        private final RealMatrix q;
        
        public CannonballProcess(final double g, final double[][] qData) {
            this.g = g;
            this.q = MatrixUtils.createRealMatrix(qData);
        }

        public NonLinearEvolution getEvolution(double previousTime, RealVector previousState, double currentTime) {
            final double dt = currentTime - previousTime;
            final RealVector state = MatrixUtils.createRealVector(new double[] {
                previousState.getEntry(0) + previousState.getEntry(1) * dt,
                previousState.getEntry(1),
                previousState.getEntry(2) + previousState.getEntry(3) * dt - 0.5 * g * dt * dt,
                previousState.getEntry(3) - g * dt
            });
            final RealMatrix stm = MatrixUtils.createRealMatrix(new double[][] {
                { 1.0,  dt, 0.0, 0.0 },
                { 0.0, 1.0, 0.0, 0.0 },
                { 0.0, 0.0, 1.0,  dt },
                { 0.0, 0.0, 0.0, 1.0 },
            });
            return new NonLinearEvolution(currentTime, state, stm, q);
        }

    }

    @Test
    public void testWelshBishopExactR() {
        doTestWelshBishop(0xd30a8f811e2f7c61l, -0.37727, 0.1,
                          0.0, 1.0, 1.0e-5, 0.1 * 0.1,
                          50, -0.389117, 1.0e-6);
    }

    @Test
    public void testWelshBishopBigR() {
        doTestWelshBishop(0xd30a8f811e2f7c61l, -0.37727, 0.1,
                          0.0, 1.0, 1.0e-5, 1.0 * 1.0,
                          50, -0.385613, 1.0e-6);
    }

    @Test
    public void testWelshBishopSmallR() {
        doTestWelshBishop(0xd30a8f811e2f7c61l, -0.37727, 0.1,
                          0.0, 1.0, 1.0e-5, 0.01 * 0.01,
                          50, -0.403015, 1.0e-6);
    }

    private void doTestWelshBishop(final long seed,
                                   final double trueConstant, final double trueStdv,
                                   final double initialEstimate, final double initialCovariance,
                                   final double q, final double r,
                                   final int nbMeasurements,
                                   final double expected, final double tolerance) {

        // this is the constant voltage example from paper
        // An Introduction to the Kalman Filter, Greg Welch and Gary Bishop
        // available from http://www.cs.unc.edu/~welch/media/pdf/kalman_intro.pdf
        final ProcessEstimate initial = new ProcessEstimate(0,
                                                            MatrixUtils.createRealVector(new double[] { initialEstimate }),
                                                            MatrixUtils.createRealDiagonalMatrix(new double[] { initialCovariance }));
        final RandomGenerator generator = new Well1024a(seed);
        final Stream<Measurement> measurements =
                        IntStream.
                        range(0, nbMeasurements).
                        mapToObj(i -> new Measurement(i,
                                                      MatrixUtils.createRealVector(new double[] {
                                                          trueConstant + generator.nextGaussian() * trueStdv,
                                                      }),
                                                      MatrixUtils.createRealMatrix(new double[][] { { 1.0 } }),
                                                      MatrixUtils.createRealDiagonalMatrix(new double[] { r })));

        // set up Kalman estimator
        final ExtendedKalmanEstimator estimator =
                        new ExtendedKalmanEstimator(new CholeskyDecomposer(1.0e-15, 1.0e-15),
                                                    (previousTime, previousState, currentTime) ->
                                                      new NonLinearEvolution(currentTime,
                                                                             previousState,
                                                                             MatrixUtils.createRealIdentityMatrix(1),
                                                                             MatrixUtils.createRealDiagonalMatrix(new double[] {
                                                                                 q
                                                                             })),
                                                    initial);

        // sequentially process all measurements and get only the last one
        final Stream<ProcessEstimate> estimates = estimator.estimate(measurements);
        final ProcessEstimate finalEstimate = estimates.reduce((first, second) -> second).get();

        Assert.assertEquals(expected, finalEstimate.getState().getEntry(0), tolerance);

    }

    private List<Reference> loadReferenceData(final int stateDimension, final int measurementDimension,
                                              final String name) {
        List<Reference> loaded = new ArrayList<>();
        try (InputStream is = KalmanEstimator.class.getResourceAsStream(name);
             InputStreamReader isr = new InputStreamReader(is, "UTF-8");
             BufferedReader br = new BufferedReader(isr)) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) {
                    loaded.add(new Reference(stateDimension, measurementDimension, line));
                }
            }
        } catch (IOException ioe) {
            Assert.fail(ioe.getLocalizedMessage());
        }
        return loaded;
    }

    private static class Reference {

        private final double     time;
        private final RealVector z;
        private final RealVector s;
        private final RealMatrix c;

        Reference(final int stateDimension, final int measurementDimension, final String line) {
            final String[] fields = line.split("\\s+");
            int index = 0;
            time = Double.parseDouble(fields[index++]);
            z = MatrixUtils.createRealVector(new double[measurementDimension]);
            for (int i = 0; i < measurementDimension; ++i) {
                z.setEntry(i, Double.parseDouble(fields[index++]));
            }
            s = MatrixUtils.createRealVector(new double[stateDimension]);
            for (int i = 0; i < stateDimension; ++i) {
                s.setEntry(i, Double.parseDouble(fields[index++]));
            }
            c = MatrixUtils.createRealMatrix(stateDimension, stateDimension);
            for (int i = 0; i < stateDimension; ++i) {
                for (int j = i; j < stateDimension; ++j) {
                    c.setEntry(i, j, Double.parseDouble(fields[index++]));
                    c.setEntry(j, i, c.getEntry(i, j));
                }
            }
        }

        public boolean sameTime(final double otherTime) {
            return FastMath.abs(time - otherTime) < 1.0e-6;
        }

        public void checkState(final RealVector otherState, final double tolerance) {
            Assert.assertEquals(s.getDimension(), otherState.getDimension());
            for (int i = 0; i < s.getDimension(); ++i) {
                Assert.assertEquals(time + ": ", s.getEntry(i), otherState.getEntry(i), tolerance);
            }
        }

        public void checkcovariance(final RealMatrix otherCovariance, final double tolerance) {
            Assert.assertEquals(c.getRowDimension(), otherCovariance.getRowDimension());
            Assert.assertEquals(c.getColumnDimension(), otherCovariance.getColumnDimension());
            for (int i = 0; i < c.getRowDimension(); ++i) {
                for (int j = i; j < c.getColumnDimension(); ++j) {
                    Assert.assertEquals(time + ": ", c.getEntry(i, j), otherCovariance.getEntry(i, j), tolerance);
                }
            }
        }

    }

}
