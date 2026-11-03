/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.beans.beancontext.BeanContextServicesSupport;
import java.beans.beancontext.BeanContextSupport;
import java.lang.annotation.ElementType;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

/**
 * Test class for PropertyConverter.
 *
 * @author Emmanuel Bourg
 * @version $Id$
 */
public class TestPropertyConverter
{
    /** Constant for an enumeration class used by some tests. */
    private static final Class<ElementType> ENUM_CLASS = ElementType.class;

    @Test
    public void testSplit()
    {
        String s = "abc, xyz , 123";
        List<String> list = PropertyConverter.split(s, ',');

        assertEquals("size", 3, list.size());
        assertEquals("1st token for '" + s + "'", "abc", list.get(0));
        assertEquals("2nd token for '" + s + "'", "xyz", list.get(1));
        assertEquals("3rd token for '" + s + "'", "123", list.get(2));
    }

    @Test
    public void testSplitNoTrim()
    {
        String s = "abc, xyz , 123";
        List<String> list = PropertyConverter.split(s, ',', false);

        assertEquals("size", 3, list.size());
        assertEquals("1st token for '" + s + "'", "abc", list.get(0));
        assertEquals("2nd token for '" + s + "'", " xyz ", list.get(1));
        assertEquals("3rd token for '" + s + "'", " 123", list.get(2));
    }

    @Test
    public void testSplitWithEscapedSeparator()
    {
        String s = "abc\\,xyz, 123";
        List<String> list = PropertyConverter.split(s, ',');

        assertEquals("size", 2, list.size());
        assertEquals("1st token for '" + s + "'", "abc,xyz", list.get(0));
        assertEquals("2nd token for '" + s + "'", "123", list.get(1));
    }

    @Test
    public void testSplitEmptyValues()
    {
        String s = ",,";
        List<String> list = PropertyConverter.split(s, ',');

        assertEquals("size", 3, list.size());
        assertEquals("1st token for '" + s + "'", "", list.get(0));
        assertEquals("2nd token for '" + s + "'", "", list.get(1));
        assertEquals("3rd token for '" + s + "'", "", list.get(2));
    }

    @Test
    public void testSplitWithEndingSlash()
    {
        String s = "abc, xyz\\";
        List<String> list = PropertyConverter.split(s, ',');

        assertEquals("size", 2, list.size());
        assertEquals("1st token for '" + s + "'", "abc", list.get(0));
        assertEquals("2nd token for '" + s + "'", "xyz\\", list.get(1));
    }

    @Test
    public void testSplitNull()
    {
        List<String> list = PropertyConverter.split(null, ',');
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    /**
     * Tests whether an escape character can be itself escaped.
     */
    @Test
    public void testSplitEscapeEscapeChar()
    {
        List<String> list = PropertyConverter.split("C:\\Temp\\\\,xyz", ',');
        assertEquals("Wrong list size", 2, list.size());
        assertEquals("Wrong element 1", "C:\\Temp\\", list.get(0));
        assertEquals("Wrong element 2", "xyz", list.get(1));
    }

    /**
     * Tests whether delimiters are correctly escaped.
     */
    @Test
    public void testEscapeDelimiters()
    {
        assertEquals("Wrong escaped delimiters",
                "C:\\\\Temp\\\\\\,D:\\\\Data\\\\", PropertyConverter
                        .escapeDelimiters("C:\\Temp\\,D:\\Data\\", ','));
    }

    /**
     * Tests whether only the list delimiter can be escaped.
     */
    @Test
    public void testEscapeListDelimiter()
    {
        assertEquals("Wrong escaped list delimiter", "C:\\Temp\\\\,D:\\Data\\",
                PropertyConverter.escapeListDelimiter("C:\\Temp\\,D:\\Data\\",
                        ','));
    }

    @Test
    public void testToIterator()
    {
        int[] array = new int[]{1, 2, 3};

        Iterator<?> it = PropertyConverter.toIterator(array, ',');

        assertEquals("1st element", new Integer(1), it.next());
        assertEquals("2nd element", new Integer(2), it.next());
        assertEquals("3rd element", new Integer(3), it.next());
    }

    /**
     * Tests that a collection containing itself does not cause an infinite
     * recursion (StackOverflowError).
     */
    @Test
    public void testToIteratorSelfReferencingCollection()
    {
        List<Object> list = new ArrayList<Object>();
        list.add("a");
        list.add(list);
        list.add("b,c");

        Iterator<?> it = PropertyConverter.toIterator(list, ',');

        assertEquals("1st element", "a", it.next());
        assertEquals("2nd element", "b", it.next());
        assertEquals("3rd element", "c", it.next());
        assertFalse("too many elements", it.hasNext());
    }

    /**
     * Tests that an indirect cycle in a tree of collections does not cause an
     * infinite recursion.
     */
    @Test
    public void testToIteratorIndirectCycle()
    {
        List<Object> list1 = new ArrayList<Object>();
        List<Object> list2 = new ArrayList<Object>();
        List<Object> list3 = new ArrayList<Object>();
        list1.add("a");
        list1.add(list2);
        list2.add("b");
        list2.add(list3);
        list3.add(list1);
        list3.add("c");
        list3.add(list2);

        Iterator<?> it = PropertyConverter.toIterator(list1, ',');

        assertEquals("1st element", "a", it.next());
        assertEquals("2nd element", "b", it.next());
        assertEquals("3rd element", "c", it.next());
        assertFalse("too many elements", it.hasNext());
    }

    /**
     * Tests that an array containing itself does not cause an infinite
     * recursion.
     */
    @Test
    public void testToIteratorSelfReferencingArray()
    {
        Object[] array = new Object[3];
        array[0] = "a";
        array[1] = array;
        array[2] = new Object[] { "b", array };

        Iterator<?> it = PropertyConverter.toIterator(array, ',');

        assertEquals("1st element", "a", it.next());
        assertEquals("2nd element", "b", it.next());
        assertFalse("too many elements", it.hasNext());
    }

    /**
     * Tests that a recursive array reference is skipped while the reachable
     * leaf values are kept.
     */
    @Test
    public void testToIteratorArrayCycle()
    {
        Object[] array = new Object[2];
        array[0] = "value1,value2";
        array[1] = array;

        assertEquals(Arrays.asList("value1", "value2"),
                toList(PropertyConverter.toIterator(array, ',')));
    }

    /**
     * Tests that a recursive list-array cycle is skipped while the reachable
     * leaf values are kept.
     */
    @Test
    public void testToIteratorMixedListAndArrayCycle()
    {
        List<Object> list = new ArrayList<Object>();
        Object[] array = { list };
        list.add("value1,value2");
        list.add(array);

        assertEquals(Arrays.asList("value1", "value2"),
                toList(PropertyConverter.toIterator(list, ',')));
    }

    /**
     * Tests that an iterator returning itself does not cause an infinite
     * recursion.
     */
    @Test
    public void testToIteratorSelfReferencingIterator()
    {
        Iterator<Object> iterator = new Iterator<Object>()
        {
            private int index;

            public boolean hasNext()
            {
                return index <= 100000;
            }

            public Object next()
            {
                return (index++ < 100000) ? (Object) this : "a";
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };

        Iterator<?> it = PropertyConverter.toIterator(iterator, ',');

        assertEquals("1st element", "a", it.next());
        assertFalse("too many elements", it.hasNext());
    }

    /**
     * Tests that a self-referencing BeanContextSupport (which is a collection)
     * does not cause an infinite recursion.
     */
    @Test
    public void testToIteratorBeanContextSupport()
    {
        assertTrue(toList(PropertyConverter.toIterator(new BeanContextSupport(), ',')).isEmpty());
        assertTrue(toList(PropertyConverter.toIterator(new BeanContextSupport(new BeanContextSupport()), ',')).isEmpty());

        BeanContextSupport bcs = new BeanContextSupport();
        bcs.add("bar");
        List<Object> list = toList(PropertyConverter.toIterator(bcs, ','));
        assertEquals("size", 1, list.size());
        assertEquals("1st element", "bar", list.get(0));

        bcs.add(bcs);
        list = toList(PropertyConverter.toIterator(bcs, ','));
        assertEquals("size", 1, list.size());
        assertEquals("1st element", "bar", list.get(0));
    }

    /**
     * Tests that a self-referencing BeanContextServicesSupport (which is a
     * collection) does not cause an infinite recursion.
     */
    @Test
    public void testToIteratorBeanContextServicesSupport()
    {
        assertTrue(toList(PropertyConverter.toIterator(new BeanContextServicesSupport(), ',')).isEmpty());
        assertTrue(toList(PropertyConverter.toIterator(new BeanContextServicesSupport(new BeanContextServicesSupport()), ',')).isEmpty());

        BeanContextSupport bcs = new BeanContextSupport();
        BeanContextServicesSupport bcss = new BeanContextServicesSupport();
        bcs.add("bar");
        bcss.add(bcs);
        List<Object> list = toList(PropertyConverter.toIterator(bcss, ','));
        assertEquals("size", 1, list.size());
        assertEquals("1st element", "bar", list.get(0));

        bcss.add("baz");
        list = toList(PropertyConverter.toIterator(bcss, ','));
        assertEquals("size", 2, list.size());
        assertTrue("bar missing", list.contains("bar"));
        assertTrue("baz missing", list.contains("baz"));

        bcss.add(bcss);
        list = toList(PropertyConverter.toIterator(bcss, ','));
        assertEquals("size", 2, list.size());
        assertTrue("bar missing", list.contains("bar"));
        assertTrue("baz missing", list.contains("baz"));
    }

    /**
     * Tests that a collection which is referenced several times in a tree
     * without forming a cycle is not skipped.
     */
    @Test
    public void testToIteratorRepeatedCollection()
    {
        List<Object> shared = new ArrayList<Object>();
        shared.add("x");
        List<Object> list = new ArrayList<Object>();
        list.add(shared);
        list.add(new Object[] { shared, "y" });
        list.add(shared);

        Iterator<?> it = PropertyConverter.toIterator(list, ',');

        assertEquals("1st element", "x", it.next());
        assertEquals("2nd element", "x", it.next());
        assertEquals("3rd element", "y", it.next());
        assertEquals("4th element", "x", it.next());
        assertFalse("too many elements", it.hasNext());
    }

    /**
     * Tests that duplicate scalar values (even identical instances) are
     * preserved; only composite values are subject to cycle detection.
     */
    @Test
    public void testToIteratorDuplicateScalars()
    {
        Integer one = Integer.valueOf(1);
        List<Object> list = new ArrayList<Object>();
        list.add(one);
        list.add(Integer.valueOf(2));
        list.add(one);
        list.add(Boolean.TRUE);
        list.add(Boolean.TRUE);
        list.add("a");
        list.add("a");

        assertEquals(Arrays.asList(one, Integer.valueOf(2), one, Boolean.TRUE,
                Boolean.TRUE, "a", "a"), toList(PropertyConverter.toIterator(
                list, ',')));
    }

    /**
     * Tests that a Path is treated as a single value. A Path is an Iterable
     * over Path objects which would otherwise cause an infinite recursion.
     */
    @Test
    public void testToIteratorPath()
    {
        Path path = Paths.get("path1");
        Iterator<?> it = PropertyConverter.toIterator(path, ',');
        assertSame("Wrong element", path, it.next());
        assertFalse("too many elements", it.hasNext());

        List<Object> list = new ArrayList<Object>();
        list.add(Paths.get("path1"));
        list.add(Paths.get("dir", "path2"));
        list.add(Paths.get("path1"));
        assertEquals(Arrays.asList(Paths.get("path1"),
                Paths.get("dir", "path2"), Paths.get("path1")),
                toList(PropertyConverter.toIterator(list, ',')));
    }

    /**
     * Collects the elements of the specified iterator in a list.
     */
    private static List<Object> toList(Iterator<?> it)
    {
        List<Object> list = new ArrayList<Object>();
        while (it.hasNext())
        {
            list.add(it.next());
        }
        return list;
    }

    /**
     * Tests the interpolation features.
     */
    @Test
    public void testInterpolateString()
    {
        PropertiesConfiguration config = new PropertiesConfiguration();
        config.addProperty("animal", "quick brown fox");
        config.addProperty("target", "lazy dog");
        assertEquals("Wrong interpolation",
                "The quick brown fox jumps over the lazy dog.",
                PropertyConverter.interpolate("The ${animal} jumps over the ${target}.", config));
    }

    /**
     * Tests interpolation of an object. Here nothing should be substituted.
     */
    @Test
    public void testInterpolateObject()
    {
        assertEquals("Object was not correctly interpolated", new Integer(42),
                PropertyConverter.interpolate(new Integer(42), new PropertiesConfiguration()));
    }

    /**
     * Tests complex interpolation where the variables' values contain in turn
     * other variables.
     */
    @Test
    public void testInterpolateRecursive()
    {
        PropertiesConfiguration config = new PropertiesConfiguration();
        config.addProperty("animal", "${animal_attr} fox");
        config.addProperty("target", "${target_attr} dog");
        config.addProperty("animal_attr", "quick brown");
        config.addProperty("target_attr", "lazy");
        assertEquals("Wrong complex interpolation",
                "The quick brown fox jumps over the lazy dog.",
                PropertyConverter.interpolate("The ${animal} jumps over the ${target}.", config));
    }

    /**
     * Tests an interpolation that leads to a cycle. This should throw an
     * exception.
     */
    @Test(expected = IllegalStateException.class)
    public void testCyclicInterpolation()
    {
        PropertiesConfiguration config = new PropertiesConfiguration();
        config.addProperty("animal", "${animal_attr} ${species}");
        config.addProperty("animal_attr", "quick brown");
        config.addProperty("species", "${animal}");
        PropertyConverter.interpolate("This is a ${animal}", config);
    }

    /**
     * Tests interpolation if a variable is unknown. Then the variable won't be
     * substituted.
     */
    @Test
    public void testInterpolationUnknownVariable()
    {
        PropertiesConfiguration config = new PropertiesConfiguration();
        config.addProperty("animal", "quick brown fox");
        assertEquals("Wrong interpolation",
                "The quick brown fox jumps over ${target}.",
                PropertyConverter.interpolate("The ${animal} jumps over ${target}.", config));
    }

    /**
     * Tests conversion to numbers when the passed in objects are already
     * numbers.
     */
    @Test
    public void testToNumberDirect()
    {
        Integer i = new Integer(42);
        assertSame("Wrong integer", i, PropertyConverter.toNumber(i, Integer.class));
        BigDecimal d = new BigDecimal("3.1415");
        assertSame("Wrong BigDecimal", d, PropertyConverter.toNumber(d, Integer.class));
    }

    /**
     * Tests conversion to numbers when the passed in objects have a compatible
     * string representation.
     */
    @Test
    public void testToNumberFromString()
    {
        assertEquals("Incorrect Integer value", new Integer(42), PropertyConverter.toNumber("42", Integer.class));
        assertEquals("Incorrect Short value", new Short((short) 10), PropertyConverter.toNumber(new StringBuffer("10"), Short.class));
    }

    /**
     * Tests conversion to numbers when the passed in objects are strings with
     * prefixes for special radices.
     */
    @Test
    public void testToNumberFromHexString()
    {
        Number n = PropertyConverter.toNumber("0x10", Integer.class);
        assertEquals("Incorrect Integer value", 16, n.intValue());
    }

    /**
     * Tests conversion to numbers when an invalid Hex value is passed in.
     * This should cause an exception.
     */
    @Test(expected = ConversionException.class)
    public void testToNumberFromInvalidHexString()
    {
        PropertyConverter.toNumber("0xNotAHexValue", Integer.class);
    }

    /**
     * Tests conversion to numbers when the passed in objects are strings with
     * prefixes for special radices.
     */
    @Test
    public void testToNumberFromBinaryString()
    {
        Number n = PropertyConverter.toNumber("0b1111", Integer.class);
        assertEquals("Incorrect Integer value", 15, n.intValue());
    }

    /**
     * Tests conversion to numbers when an invalid binary value is passed in.
     * This should cause an exception.
     */
    @Test(expected = ConversionException.class)
    public void testToNumberFromInvalidBinaryString()
    {
        PropertyConverter.toNumber("0bNotABinValue", Integer.class);
    }

    /**
     * Tests conversion to numbers when the passed in objects have no numeric
     * String representation. This should cause an exception.
     */
    @Test(expected = ConversionException.class)
    public void testToNumberFromInvalidString()
    {
        PropertyConverter.toNumber("Not a number", Byte.class);
    }

    /**
     * Tests conversion to numbers when the passed in target class is invalid.
     * This should cause an exception.
     */
    @Test(expected = ConversionException.class)
    public void testToNumberWithInvalidClass()
    {
        PropertyConverter.toNumber("42", Object.class);
    }

    @Test
    public void testToEnumFromEnum()
    {
        assertEquals(ElementType.METHOD, PropertyConverter.toEnum(ElementType.METHOD, ENUM_CLASS));
    }

    @Test
    public void testToEnumFromString()
    {
        assertEquals(ElementType.METHOD, PropertyConverter.toEnum("METHOD", ENUM_CLASS));
    }

    @Test(expected = ConversionException.class)
    public void testToEnumFromInvalidString()
    {
        PropertyConverter.toEnum("FOO", ENUM_CLASS);
    }

    @Test
    public void testToEnumFromNumber()
    {
        assertEquals(ElementType.METHOD, PropertyConverter.toEnum(
                Integer.valueOf(ElementType.METHOD.ordinal()),
                ENUM_CLASS));
    }

    @Test(expected = ConversionException.class)
    public void testToEnumFromInvalidNumber()
    {
        PropertyConverter.toEnum(Integer.valueOf(-1), ENUM_CLASS);
    }

    /**
     * Tests a trivial conversion: the value has already the desired type.
     */
    @Test
    public void testToNoConversionNeeded()
    {
        String value = "testValue";
        assertEquals("Wrong conversion result", value,
                PropertyConverter.to(String.class, value, null));
    }

    /**
     * Tests whether a conversion to character is possible.
     */
    @Test
    public void testToCharSuccess()
    {
        assertEquals("Wrong conversion result", Character.valueOf('t'),
                PropertyConverter.to(Character.class, "t", null));
    }

    /**
     * Tests whether other objects implementing a toString() method can be
     * converted to character.
     */
    @Test
    public void testToCharViaToString()
    {
        Object value = new Object()
        {
            @Override
            public String toString()
            {
                return "X";
            }
        };
        assertEquals("Wrong conversion result", Character.valueOf('X'),
                PropertyConverter.to(Character.TYPE, value, null));
    }

    /**
     * Tests a failed conversion to character.
     */
    @Test(expected = ConversionException.class)
    public void testToCharFailed()
    {
        PropertyConverter.to(Character.TYPE, "FF", null);
    }
}
