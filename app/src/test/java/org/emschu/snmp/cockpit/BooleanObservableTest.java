/*
 * SNMP Cockpit Android App
 *
 * Copyright (C) 2018-2019
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.emschu.snmp.cockpit;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

import java.util.Observable;
import java.util.Observer;

import org.emschu.snmp.cockpit.util.BooleanObservable;

/**
 * test util class {@link BooleanObservable}
 */
public class BooleanObservableTest {

    private static boolean wasCalled = false;
    private static Observer testObserver = new Observer() {
        @Override
        public void update(Observable observable, Object o) {
            BooleanObservableTest.wasCalled = true;
        }
    };

    @Before
    public void setup() {
        wasCalled = false;
    }

    @Test
    public void testBooleanObservableMethods() {
        BooleanObservable bo = new BooleanObservable(true);
        Assert.assertTrue(bo.getValue());
        bo.setValue(false);
        Assert.assertFalse(bo.getValue());
        Assert.assertFalse(wasCalled);
        bo.addObserver(testObserver);
        Assert.assertFalse(wasCalled);
        bo.setValue(false);
        Assert.assertFalse(wasCalled);
        bo.notifyObservers();
        Assert.assertTrue(wasCalled);
        Assert.assertFalse(bo.getValue());
    }

    @Test
    public void testObservableInstantUpdateMethod() {
        BooleanObservable bo = new BooleanObservable(true);
        bo.addObserver(testObserver);
        Assert.assertTrue(bo.getValue());
        Assert.assertFalse(wasCalled);
        bo.setValueAndTriggerObservers(false);
        Assert.assertFalse(bo.hasChanged());
        Assert.assertTrue(wasCalled);
        Assert.assertFalse(bo.getValue());
    }

    @Test
    public void testObservableOnlyUpdateIfUpdate() {
        BooleanObservable bo = new BooleanObservable(true);
        bo.addObserver(testObserver);
        Assert.assertFalse(wasCalled);
        bo.setValue(true);
        Assert.assertFalse(bo.hasChanged());
        bo.setValue(false);
        Assert.assertTrue(bo.hasChanged());
    }
}
