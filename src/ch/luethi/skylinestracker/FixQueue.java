/*
 * SkyLines Tracker is a location tracking client for the SkyLines platform <www.skylines-project.org>.
 * Copyright (C) 2013  Andreas Lüthi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.luethi.skylinestracker;

import android.content.Context;
import ch.luethi.ringbuffer.*;


public class FixQueue implements FixQueueIF {

    private static final String RINGBUFFER_DATA = "ringbuffer.dat";
    private static final int INIT_CAPACITY = 2000;
    private static final byte BUF_LEN = 48;
    private RingBuffer rb;

    public FixQueue(Context ctx, boolean init) {
        super();
        if (init) {
            rb = new RingBuffer(ctx.getDir("data", Context.MODE_PRIVATE).getAbsolutePath() + "/" + RINGBUFFER_DATA, INIT_CAPACITY);
            rb.setRecLen(BUF_LEN);
        } else {
            rb = new RingBuffer(ctx.getDir("data", Context.MODE_PRIVATE).getAbsolutePath() + "/" + RINGBUFFER_DATA);
        }
    }


    @Override
    public void push(byte[] data) {
        rb.push(data);
    }

    @Override
    public byte[] pop() {
        return rb.pop();
    }

    @Override
    public byte[][] pop(int num) {
        return rb.peek(num);
    }


    @Override
    public long size() {
        return  rb.getCount();
    }

    @Override
    public boolean isEmpty() {
        return rb.getCount()==0;
    }

    @Override
    public FixQueueIF load() {
        return this;
    }
}
