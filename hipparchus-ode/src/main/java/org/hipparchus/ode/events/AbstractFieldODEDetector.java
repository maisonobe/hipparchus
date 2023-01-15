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

package org.hipparchus.ode.events;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.ode.FieldODEStateAndDerivative;

/** Base class for #@link {@link FieldODEEventDetector}.
 * @param <T> type of the detector
 * @param <E> type of the field elements
 * @since 3.0
 */
public abstract class AbstractFieldODEDetector<T extends AbstractFieldODEDetector<T, E>, E extends CalculusFieldElement<E>>
    implements FieldODEEventDetector<E> {

    /** Default maximum checking interval (s). */
    public static final double DEFAULT_MAXCHECK = 600;

    /** Default convergence threshold (s). */
    public static final double DEFAULT_THRESHOLD = 1.e-6;

    /** Default maximum number of iterations in the event time search. */
    public static final int DEFAULT_MAX_ITER = 100;

    /** Max check interval. */
    private final E maxCheck;

    /** Convergence threshold. */
    private final E threshold;

    /** Maximum number of iterations in the event time search. */
    private final int maxIter;

    /** Default handler for event overrides. */
    private final FieldODEEventHandler<E> handler;

    /** Propagation direction. */
    private boolean forward;

    /** Build a new instance.
     * @param maxCheck maximum checking interval, must be strictly positive (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     */
    protected AbstractFieldODEDetector(final E maxCheck, final E threshold, final int maxIter,
                                       final FieldODEEventHandler<E> handler) {
        checkStrictlyPositive(maxCheck);
        checkStrictlyPositive(threshold);
        this.maxCheck  = maxCheck;
        this.threshold = threshold;
        this.maxIter   = maxIter;
        this.handler   = handler;
        this.forward   = true;
    }

    /** Check value is strictly positive.
     * @param value value to check
     * @exception MathIllegalArgumentException if value is not strictly positive
     */
    private void checkStrictlyPositive(final E value) throws MathIllegalArgumentException {
        if (value.getReal() <= 0.0) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.NUMBER_TOO_SMALL, value, 0.0);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p> This implementation sets the direction of integration and initializes the event
     * handler. If a subclass overrides this method it should call {@code
     * super.init(s0, t)}.
     */
    @Override
    public void init(final FieldODEStateAndDerivative<E> s0, final E t) {
        forward = t.subtract(s0.getTime()).getReal() >= 0;
        getHandler().init(s0, t, this);
    }

    /** {@inheritDoc} */
    @Override
    public abstract E g(FieldODEStateAndDerivative<E> s);

    /** {@inheritDoc} */
    @Override
    public E getMaxCheckInterval() {
        return maxCheck;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxIterationCount() {
        return maxIter;
    }

    /** {@inheritDoc} */
    @Override
    public E getThreshold() {
        return threshold;
    }

    /**
     * Setup the maximum checking interval.
     * <p>
     * This will override a maximum checking interval if it has been configured previously.
     * </p>
     * @param newMaxCheck maximum checking interval (s)
     * @return a new detector with updated configuration (the instance is not changed)
     */
    public T withMaxCheck(final E newMaxCheck) {
        return create(newMaxCheck, getThreshold(), getMaxIterationCount(), getHandler());
    }

    /**
     * Setup the maximum number of iterations in the event time search.
     * <p>
     * This will override a number of iterations if it has been configured previously.
     * </p>
     * @param newMaxIter maximum number of iterations in the event time search
     * @return a new detector with updated configuration (the instance is not changed)
     */
    public T withMaxIter(final int newMaxIter) {
        return create(getMaxCheckInterval(), getThreshold(), newMaxIter,  getHandler());
    }

    /**
     * Setup the convergence threshold.
     * <p>
     * This will override a convergence threshold if it has been configured previously.
     * </p>
     * @param newThreshold convergence threshold (s)
     * @return a new detector with updated configuration (the instance is not changed)
     */
    public T withThreshold(final E newThreshold) {
        return create(getMaxCheckInterval(), newThreshold, getMaxIterationCount(),  getHandler());
    }

    /**
     * Setup the event handler to call at event occurrences.
     * <p>
     * This will override a handler if it has been configured previously.
     * </p>
     * @param newHandler event handler to call at event occurrences
     * @return a new detector with updated configuration (the instance is not changed)
     */
    public T withHandler(final FieldODEEventHandler<E> newHandler) {
        return create(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(), newHandler);
    }

    /** {@inheritDoc} */
    @Override
    public FieldODEEventHandler<E> getHandler() {
        return handler;
    }

    /** Build a new instance.
     * @param newMaxCheck maximum checking interval (s)
     * @param newThreshold convergence threshold (s)
     * @param newMaxIter maximum number of iterations in the event time search
     * @param newHandler event handler to call at event occurrences
     * @return a new instance of the appropriate sub-type
     */
    protected abstract T create(E newMaxCheck, E newThreshold,
                                int newMaxIter, FieldODEEventHandler<E> newHandler);

    /** Check if the current propagation is forward or backward.
     * @return true if the current propagation is forward
     */
    public boolean isForward() {
        return forward;
    }

}