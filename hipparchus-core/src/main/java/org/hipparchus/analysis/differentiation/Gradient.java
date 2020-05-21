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
package org.hipparchus.analysis.differentiation;

import java.io.Serializable;
import java.util.Arrays;

import org.hipparchus.RealFieldElement;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;

/** Class representing both the value and the differentials of a function.
 * <p>This class is a stripped-down version of {@link DerivativeStructure}
 * with {@link DerivativeStructure#getOrder() derivation order} limited to one.
 * It should have less overhead than {@link DerivativeStructure} in its domain.</p>
 * <p>This class is an implementation of Rall's numbers. Rall's numbers are an
 * extension to the real numbers used throughout mathematical expressions; they hold
 * the derivative together with the value of a function.</p>
 * <p>{@link Gradient} instances can be used directly thanks to
 * the arithmetic operators to the mathematical functions provided as
 * methods by this class (+, -, *, /, %, sin, cos ...).</p>
 * <p>Implementing complex expressions by hand using these classes is
 * a tedious and error-prone task but has the advantage of having no limitation
 * on the derivation order despite not requiring users to compute the derivatives by
 * themselves.</p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @see DerivativeStructure
 * @see UnivariateDerivative1
 * @see UnivariateDerivative2
 * @see FieldDerivativeStructure
 * @see FieldUnivariateDerivative1
 * @see FieldUnivariateDerivative2
 * @see FieldGradient
 * @since 1.7
 */
public class Gradient implements RealFieldElement<Gradient>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20200520L;

    /** Value of the function. */
    private final double value;

    /** Gradient of the function. */
    private final double[] gradient;

    /** Build an instance with values and derivative.
     * @param value value of the function
     * @param gradient gradient of the function
     */
    public Gradient(final double value, final double... gradient) {
        this.value    = value;
        this.gradient = gradient.clone();
    }

    /** Build an instance from a {@link DerivativeStructure}.
     * @param ds derivative structure
     * @exception MathIllegalArgumentException if {@code ds} order
     * is not 1
     */
    public Gradient(final DerivativeStructure ds) throws MathIllegalArgumentException {
        MathUtils.checkDimension(ds.getOrder(), 1);
        final double[] derivatives = ds.getAllDerivatives();
        this.value    = derivatives[0];
        this.gradient = new double[derivatives.length - 1];
        System.arraycopy(derivatives, 1, gradient, 0, gradient.length);
    }

    /** {@inheritDoc} */
    @Override
    public Gradient newInstance(final double value) {
        return new Gradient(value, new double[gradient.length]);
    }

    /** {@inheritDoc} */
    @Override
    public double getReal() {
        return getValue();
    }

    /** Get the value part of the function.
     * @return value part of the value of the function
     */
    public double getValue() {
        return value;
    }

    /** Get the gradient part of the function.
     * @return gradient part of the value of the function
     */
    public double[] getGradient() {
        return gradient.clone();
    }

    /** Get the number of free parameters.
     * @return number of free parameters
     */
    public int getFreeParameters() {
        return gradient.length;
    }

    /** Get the partial derivative with respect to one parameter.
     * @param n index of the parameter (counting from 0)
     * @return partial derivative with respect to the n<sup>th</sup> parameter
     * @exception MathIllegalArgumentException if n is either negative or larger
     * or equal to {@link #getFreeParameters()}
     */
    public double getPartialDerivative(final int n) throws MathIllegalArgumentException {
        if (n < 0 || n >= gradient.length) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, n, 0, gradient.length - 1);
        }
        return gradient[n];
    }

    /** Convert the instance to a {@link DerivativeStructure}.
     * @return derivative structure with same value and derivative as the instance
     */
    public DerivativeStructure toDerivativeStructure() {
        final double[] derivatives = new double[1 + gradient.length];
        derivatives[0] = value;
        System.arraycopy(gradient, 0, derivatives, 1, gradient.length);
        return getField().getConversionFactory().build(derivatives);
    }

    /** {@inheritDoc} */
    @Override
    public Gradient add(final double a) {
        return new Gradient(value + a, gradient);
    }

    /** {@inheritDoc} */
    @Override
    public Gradient add(final Gradient a) {
        final Gradient result = newInstance(value + a.value);
        for (int i = 0; i < gradient.length; ++i) {
            result.gradient[i] = gradient[i] + a.gradient[i];
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient subtract(final double a) {
        return new Gradient(value - a, gradient);
    }

    /** {@inheritDoc} */
    @Override
    public Gradient subtract(final Gradient a) {
        final Gradient result = newInstance(value - a.value);
        for (int i = 0; i < gradient.length; ++i) {
            result.gradient[i] = gradient[i] - a.gradient[i];
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient multiply(final int n) {
        final Gradient result = newInstance(value * n);
        for (int i = 0; i < gradient.length; ++i) {
            result.gradient[i] = gradient[i] * n;
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient multiply(final double a) {
        final Gradient result = newInstance(value * a);
        for (int i = 0; i < gradient.length; ++i) {
            result.gradient[i] = gradient[i] * a;
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient multiply(final Gradient a) {
        final Gradient result = newInstance(value * a.value);
        for (int i = 0; i < gradient.length; ++i) {
            result.gradient[i] = MathArrays.linearCombination(gradient[i], a.value, value, a.gradient[i]);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient divide(final double a) {
        final Gradient result = newInstance(value / a);
        for (int i = 0; i < gradient.length; ++i) {
            result.gradient[i] = gradient[i] / a;
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient divide(final Gradient a) {
        final double inv1 = 1.0 / a.value;
        final double inv2 = inv1 * inv1;
        final Gradient result = newInstance(value * inv1);
        for (int i = 0; i < gradient.length; ++i) {
            result.gradient[i] = MathArrays.linearCombination(gradient[i], a.value, -value, a.gradient[i]) * inv2;
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient remainder(final double a) {
        return new Gradient(FastMath.IEEEremainder(value, a), gradient);
    }

    /** {@inheritDoc} */
    @Override
    public Gradient remainder(final Gradient a) {

        // compute k such that lhs % rhs = lhs - k rhs
        final double rem = FastMath.IEEEremainder(value, a.value);
        final double k   = FastMath.rint((value - rem) / a.value);

        final Gradient result = newInstance(rem);
        for (int i = 0; i < gradient.length; ++i) {
            result.gradient[i] = gradient[i] - k * a.gradient[i];
        }
        return result;

    }

    /** {@inheritDoc} */
    @Override
    public Gradient negate() {
        final Gradient result = newInstance(-value);
        for (int i = 0; i < gradient.length; ++i) {
            result.gradient[i] = -gradient[i];
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient abs() {
        if (Double.doubleToLongBits(value) < 0) {
            // we use the bits representation to also handle -0.0
            return negate();
        } else {
            return this;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Gradient ceil() {
        return newInstance(FastMath.ceil(value));
    }

    /** {@inheritDoc} */
    @Override
    public Gradient floor() {
        return newInstance(FastMath.floor(value));
    }

    /** {@inheritDoc} */
    @Override
    public Gradient rint() {
        return newInstance(FastMath.rint(value));
    }

    /** {@inheritDoc} */
    @Override
    public long round() {
        return FastMath.round(value);
    }

    /** {@inheritDoc} */
    @Override
    public Gradient signum() {
        return newInstance(FastMath.signum(value));
    }

    /** {@inheritDoc} */
    @Override
    public Gradient copySign(final Gradient sign) {
        long m = Double.doubleToLongBits(value);
        long s = Double.doubleToLongBits(sign.value);
        if ((m >= 0 && s >= 0) || (m < 0 && s < 0)) { // Sign is currently OK
            return this;
        }
        return negate(); // flip sign
    }

    /** {@inheritDoc} */
    @Override
    public Gradient copySign(final double sign) {
        long m = Double.doubleToLongBits(value);
        long s = Double.doubleToLongBits(sign);
        if ((m >= 0 && s >= 0) || (m < 0 && s < 0)) { // Sign is currently OK
            return this;
        }
        return negate(); // flip sign
    }

    /** {@inheritDoc} */
    @Override
    public int getExponent() {
        return FastMath.getExponent(value);
    }

    /** {@inheritDoc} */
    @Override
    public Gradient scalb(final int n) {
        final Gradient result = newInstance(FastMath.scalb(value, n));
        for (int i = 0; i < gradient.length; ++i) {
            result.gradient[i] = FastMath.scalb(gradient[i], n);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient hypot(final Gradient y) {

        if (Double.isInfinite(value) || Double.isInfinite(y.value)) {
            return newInstance(Double.POSITIVE_INFINITY);
        } else if (Double.isNaN(value) || Double.isNaN(y.value)) {
            return newInstance(Double.NaN);
        } else {

            final int expX = getExponent();
            final int expY = y.getExponent();
            if (expX > expY + 27) {
                // y is neglectible with respect to x
                return abs();
            } else if (expY > expX + 27) {
                // x is neglectible with respect to y
                return y.abs();
            } else {

                // find an intermediate scale to avoid both overflow and underflow
                final int middleExp = (expX + expY) / 2;

                // scale parameters without losing precision
                final Gradient scaledX = scalb(-middleExp);
                final Gradient scaledY = y.scalb(-middleExp);

                // compute scaled hypotenuse
                final Gradient scaledH =
                        scaledX.multiply(scaledX).add(scaledY.multiply(scaledY)).sqrt();

                // remove scaling
                return scaledH.scalb(middleExp);

            }

        }
    }

    /** {@inheritDoc} */
    @Override
    public Gradient reciprocal() {
        final double inv1 = 1.0 / value;
        final double inv2 = inv1 * inv1;
        final Gradient result = newInstance(inv1);
        for (int i = 0; i < gradient.length; ++i) {
            result.gradient[i] = -gradient[i] * inv2;
        }
        return result;
    }

    /** Compute composition of the instance by a function.
     * @param g0 value of the function at the current point (i.e. at {@code g(getValue())})
     * @param g1 first derivative of the function at the current point (i.e. at {@code g'(getValue())})
     * @return g(this)
     */
    public Gradient compose(final double g0, final double g1) {
        final Gradient result = newInstance(g0);
        for (int i = 0; i < gradient.length; ++i) {
            result.gradient[i] = g1 * gradient[i];
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient sqrt() {
        final double s = FastMath.sqrt(value);
        return compose(s, 1 / (2 * s));
    }

    /** {@inheritDoc} */
    @Override
    public Gradient cbrt() {
        final double c = FastMath.cbrt(value);
        return compose(c, 1 / (3 * c * c));
    }

    /** {@inheritDoc} */
    @Override
    public Gradient rootN(final int n) {
        if (n == 2) {
            return sqrt();
        } else if (n == 3) {
            return cbrt();
        } else {
            final double r = FastMath.pow(value, 1.0 / n);
            return compose(r, 1 / (n * FastMath.pow(r, n - 1)));
        }
    }

    /** {@inheritDoc} */
    @Override
    public GradientField getField() {
        return GradientField.getField(getFreeParameters());
    }

    /** Compute a<sup>x</sup> where a is a double and x a {@link Gradient}
     * @param a number to exponentiate
     * @param x power to apply
     * @return a<sup>x</sup>
     */
    public static Gradient pow(final double a, final Gradient x) {
        if (a == 0) {
            return x.getField().getZero();
        } else {
            final double aX = FastMath.pow(a, x.value);
            final double aXlnA = aX * FastMath.log(a);
            final Gradient result = x.newInstance(aX);
            for (int i = 0; i < x.gradient.length; ++i) {
                result.gradient[i] =  aXlnA * x.gradient[i];
            }
            return result;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Gradient pow(final double p) {
        if (p == 0) {
            return getField().getOne();
        } else {
            final double valuePm1 = FastMath.pow(value, p - 1);
            return compose(valuePm1 * value, p * valuePm1);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Gradient pow(final int n) {
        if (n == 0) {
            return getField().getOne();
        } else {
            final double valueNm1 = FastMath.pow(value, n - 1);
            return compose(valueNm1 * value, n * valueNm1);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Gradient pow(final Gradient e) {
        return log().multiply(e).exp();
    }

    /** {@inheritDoc} */
    @Override
    public Gradient exp() {
        final double exp = FastMath.exp(value);
        return compose(exp, exp);
    }

    /** {@inheritDoc} */
    @Override
    public Gradient expm1() {
        final double exp   = FastMath.exp(value);
        final double expM1 = FastMath.expm1(value);
        return compose(expM1, exp);
    }

    /** {@inheritDoc} */
    @Override
    public Gradient log() {
        return compose(FastMath.log(value), 1 / value);
    }

    /** {@inheritDoc} */
    @Override
    public Gradient log1p() {
        return compose(FastMath.log1p(value), 1 / (1 + value));
    }

    /** {@inheritDoc} */
    @Override
    public Gradient log10() {
        return compose(FastMath.log10(value), 1 / (value * FastMath.log(10.0)));
    }

    /** {@inheritDoc} */
    @Override
    public Gradient cos() {
        final SinCos sinCos = FastMath.sinCos(value);
        return compose(sinCos.cos(), -sinCos.sin());
    }

    /** {@inheritDoc} */
    @Override
    public Gradient sin() {
        final SinCos sinCos = FastMath.sinCos(value);
        return compose(sinCos.sin(), sinCos.cos());
    }

    /** {@inheritDoc} */
    @Override
    public FieldSinCos<Gradient> sinCos() {
        final SinCos sinCos = FastMath.sinCos(value);
        final Gradient sin = newInstance(sinCos.sin());
        final Gradient cos = newInstance(sinCos.cos());
        for (int i = 0; i < gradient.length; ++i) {
            sin.gradient[i] =  +gradient[i] * sinCos.cos();
            cos.gradient[i] =  -gradient[i] * sinCos.sin();
        }
        return new FieldSinCos<>(sin, cos);
    }

    /** {@inheritDoc} */
    @Override
    public Gradient tan() {
        final double tan = FastMath.tan(value);
        return compose(tan, 1 + tan * tan);
    }

    /** {@inheritDoc} */
    @Override
    public Gradient acos() {
        return compose(FastMath.acos(value), -1 / FastMath.sqrt(1 - value * value));
    }

    /** {@inheritDoc} */
    @Override
    public Gradient asin() {
        return compose(FastMath.asin(value), 1 / FastMath.sqrt(1 - value * value));
    }

    /** {@inheritDoc} */
    @Override
    public Gradient atan() {
        return compose(FastMath.atan(value), 1 / (1 + value * value));
    }

    /** {@inheritDoc} */
    @Override
    public Gradient atan2(final Gradient x) {
        final double inv = 1.0 / (value * value + x.value * x.value);
        final Gradient result = newInstance(FastMath.atan2(value, x.value));
        for (int i = 0; i < gradient.length; ++i) {
            result.gradient[i] = MathArrays.linearCombination(x.value, gradient[i], -x.gradient[i], value) * inv;
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient cosh() {
        return compose(FastMath.cosh(value), FastMath.sinh(value));
    }

    /** {@inheritDoc} */
    @Override
    public Gradient sinh() {
        return compose(FastMath.sinh(value), FastMath.cosh(value));
    }

    /** {@inheritDoc} */
    @Override
    public Gradient tanh() {
        final double tanh = FastMath.tanh(value);
        return compose(tanh, 1 - tanh * tanh);
    }

    /** {@inheritDoc} */
    @Override
    public Gradient acosh() {
        return compose(FastMath.acosh(value), 1 / FastMath.sqrt(value * value - 1));
    }

    /** {@inheritDoc} */
    @Override
    public Gradient asinh() {
        return compose(FastMath.asinh(value), 1 / FastMath.sqrt(value * value + 1));
    }

    /** {@inheritDoc} */
    @Override
    public Gradient atanh() {
        return compose(FastMath.atanh(value), 1 / (1 - value * value));
    }

    /** {@inheritDoc} */
    @Override
    public Gradient toDegrees() {
        final Gradient result = newInstance(FastMath.toDegrees(value));
        for (int i = 0; i < gradient.length; ++i) {
            result.gradient[i] = FastMath.toDegrees(gradient[i]);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient toRadians() {
        final Gradient result = newInstance(FastMath.toRadians(value));
        for (int i = 0; i < gradient.length; ++i) {
            result.gradient[i] = FastMath.toRadians(gradient[i]);
        }
        return result;
    }

    /** Evaluate Taylor expansion a derivative structure.
     * @param delta parameters offsets (&Delta;x, &Delta;y, ...)
     * @return value of the Taylor expansion at x + &Delta;x, y + &Delta;y, ...
     */
    public double taylor(final double... delta) {
        double result = value;
        for (int i = 0; i < gradient.length; ++i) {
            result += delta[i] * gradient[i];
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient linearCombination(final Gradient[] a, final Gradient[] b) {

        // extract values and first derivatives
        final int      n  = a.length;
        final double[] a0 = new double[n];
        final double[] b0 = new double[n];
        final double[] a1 = new double[2 * n];
        final double[] b1 = new double[2 * n];
        for (int i = 0; i < n; ++i) {
            final Gradient ai = a[i];
            final Gradient bi = b[i];
            a0[i]         = ai.value;
            b0[i]         = bi.value;
            a1[2 * i]     = ai.value;
            b1[2 * i + 1] = bi.value;
        }

        final Gradient result = newInstance(MathArrays.linearCombination(a0, b0));
        for (int k = 0; k < gradient.length; ++k) {
            for (int i = 0; i < n; ++i) {
                a1[2 * i + 1] = a[i].gradient[k];
                b1[2 * i]     = b[i].gradient[k];
            }
            result.gradient[k] = MathArrays.linearCombination(a1, b1);
        }
        return result;

    }

    /** {@inheritDoc} */
    @Override
    public Gradient linearCombination(final double[] a, final Gradient[] b) {

        // extract values and first derivatives
        final int      n  = b.length;
        final double[] b0 = new double[n];
        final double[] b1 = new double[n];
        for (int i = 0; i < n; ++i) {
            b0[i] = b[i].value;
        }

        final Gradient result = newInstance(MathArrays.linearCombination(a, b0));
        for (int k = 0; k < gradient.length; ++k) {
            for (int i = 0; i < n; ++i) {
                b1[i] = b[i].gradient[k];
            }
            result.gradient[k] = MathArrays.linearCombination(a, b1);
        }
        return result;

    }

    /** {@inheritDoc} */
    @Override
    public Gradient linearCombination(final Gradient a1, final Gradient b1,
                                      final Gradient a2, final Gradient b2) {
        final Gradient result = newInstance(MathArrays.linearCombination(a1.value, b1.value,
                                                                         a2.value, b2.value));
        for (int i = 0; i < b1.gradient.length; ++i) {
            result.gradient[i] = MathArrays.linearCombination(a1.value,       b1.gradient[i],
                                                              a1.gradient[i], b1.value,
                                                              a2.value,       b2.gradient[i],
                                                              a2.gradient[i], b2.value);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient linearCombination(final double a1, final Gradient b1,
                                                   final double a2, final Gradient b2) {
        final Gradient result = newInstance(MathArrays.linearCombination(a1, b1.value,
                                                                         a2, b2.value));
        for (int i = 0; i < b1.gradient.length; ++i) {
            result.gradient[i] = MathArrays.linearCombination(a1, b1.gradient[i],
                                                              a2, b2.gradient[i]);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient linearCombination(final Gradient a1, final Gradient b1,
                                      final Gradient a2, final Gradient b2,
                                      final Gradient a3, final Gradient b3) {
        final double[] a = new double[] {
            a1.value, 0, a2.value, 0, a3.value, 0
        };
        final double[] b = new double[] {
            0, b1.value, 0, b2.value, 0, b3.value
        };
        final Gradient result = newInstance(MathArrays.linearCombination(a1.value, b1.value,
                                                                         a2.value, b2.value,
                                                                         a3.value, b3.value));
        for (int i = 0; i < b1.gradient.length; ++i) {
            a[1] = a1.gradient[i];
            a[3] = a2.gradient[i];
            a[5] = a3.gradient[i];
            b[0] = b1.gradient[i];
            b[2] = b2.gradient[i];
            b[4] = b3.gradient[i];
            result.gradient[i] = MathArrays.linearCombination(a, b);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient linearCombination(final double a1, final Gradient b1,
                                      final double a2, final Gradient b2,
                                      final double a3, final Gradient b3) {
        final Gradient result = newInstance(MathArrays.linearCombination(a1, b1.value,
                                                                         a2, b2.value,
                                                                         a3, b3.value));
        for (int i = 0; i < b1.gradient.length; ++i) {
            result.gradient[i] = MathArrays.linearCombination(a1, b1.gradient[i],
                                                              a2, b2.gradient[i],
                                                              a3, b3.gradient[i]);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient linearCombination(final Gradient a1, final Gradient b1,
                                      final Gradient a2, final Gradient b2,
                                      final Gradient a3, final Gradient b3,
                                      final Gradient a4, final Gradient b4) {
        final double[] a = new double[] {
            a1.value, 0, a2.value, 0, a3.value, 0, a4.value, 0
        };
        final double[] b = new double[] {
            0, b1.value, 0, b2.value, 0, b3.value, 0, b4.value
        };
        final Gradient result = newInstance(MathArrays.linearCombination(a1.value, b1.value,
                                                                         a2.value, b2.value,
                                                                         a3.value, b3.value,
                                                                         a4.value, b4.value));
        for (int i = 0; i < b1.gradient.length; ++i) {
            a[1] = a1.gradient[i];
            a[3] = a2.gradient[i];
            a[5] = a3.gradient[i];
            a[7] = a4.gradient[i];
            b[0] = b1.gradient[i];
            b[2] = b2.gradient[i];
            b[4] = b3.gradient[i];
            b[6] = b4.gradient[i];
            result.gradient[i] = MathArrays.linearCombination(a, b);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Gradient linearCombination(final double a1, final Gradient b1,
                                      final double a2, final Gradient b2,
                                      final double a3, final Gradient b3,
                                      final double a4, final Gradient b4) {
        final Gradient result = newInstance(MathArrays.linearCombination(a1, b1.value,
                                                                         a2, b2.value,
                                                                         a3, b3.value,
                                                                         a4, b4.value));
        for (int i = 0; i < b1.gradient.length; ++i) {
            result.gradient[i] = MathArrays.linearCombination(a1, b1.gradient[i],
                                                              a2, b2.gradient[i],
                                                              a3, b3.gradient[i],
                                                              a4, b4.gradient[i]);
        }
        return result;
    }

    /** Test for the equality of two univariate derivatives.
     * <p>
     * univariate derivatives are considered equal if they have the same derivatives.
     * </p>
     * @param other Object to test for equality to this
     * @return true if two univariate derivatives are equal
     */
    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (other instanceof Gradient) {
            final Gradient rhs = (Gradient) other;
            return value == rhs.value && MathArrays.equals(gradient, rhs.gradient);
        }

        return false;

    }

    /** Get a hashCode for the univariate derivative.
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return 129 + 7 * Double.hashCode(value) - 15 * Arrays.hashCode(gradient);
    }

}
