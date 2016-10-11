/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package io.reactivex.internal.operators.observable;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.internal.fuseable.QueueDisposable;
import java.util.concurrent.atomic.AtomicLong;

public final class ObservableRangeLong extends Observable<Long> {
    private final long start;
    private final long count;

    public ObservableRangeLong(long start, long count) {
        this.start = start;
        this.count = count;
    }

    @Override
    protected void subscribeActual(Observer<? super Long> o) {
        RangeDisposable parent = new RangeDisposable(o, start, start + count);
        o.onSubscribe(parent);
        parent.run();
    }

    static final class RangeDisposable
    extends AtomicLong
    implements QueueDisposable<Long> {

        private static final long serialVersionUID = 396518478098735504L;

        final Observer<? super Long> actual;

        final long end;

        long index;

        boolean fused;

        RangeDisposable(Observer<? super Long> actual, long start, long end) {
            this.actual = actual;
            this.index = start;
            this.end = end;
        }

        void run() {
            if (fused) {
                return;
            }
            Observer<? super Long> actual = this.actual;
            long e = end;
            for (long i = index; i != e && get() == 0; i++) {
                actual.onNext(i);
            }
            if (get() == 0) {
                lazySet(1);
                actual.onComplete();
            }
        }

        @Override
        public boolean offer(Long value) {
            throw new UnsupportedOperationException("Should not be called!");
        }

        @Override
        public boolean offer(Long v1, Long v2) {
            throw new UnsupportedOperationException("Should not be called!");
        }

        @Override
        public Long poll() throws Exception {
            long i = index;
            if (i != end) {
                index = i + 1;
                return i;
            }
            lazySet(1);
            return null;
        }

        @Override
        public boolean isEmpty() {
            return index == end;
        }

        @Override
        public void clear() {
            index = end;
            lazySet(1);
        }

        @Override
        public void dispose() {
            set(1);
        }

        @Override
        public boolean isDisposed() {
            return get() != 0;
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & SYNC) != 0) {
                fused = true;
                return SYNC;
            }
            return NONE;
        }
    }
}
