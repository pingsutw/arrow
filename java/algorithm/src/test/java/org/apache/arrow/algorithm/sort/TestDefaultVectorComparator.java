/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.arrow.algorithm.sort;

import static org.apache.arrow.vector.complex.BaseRepeatedValueVector.OFFSET_WIDTH;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.UInt1Vector;
import org.apache.arrow.vector.UInt2Vector;
import org.apache.arrow.vector.UInt4Vector;
import org.apache.arrow.vector.UInt8Vector;
import org.apache.arrow.vector.complex.ListVector;
import org.apache.arrow.vector.types.Types;
import org.apache.arrow.vector.types.pojo.FieldType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for {@link DefaultVectorComparators}.
 */
public class TestDefaultVectorComparator {

  private BufferAllocator allocator;

  @Before
  public void prepare() {
    allocator = new RootAllocator(1024 * 1024);
  }

  @After
  public void shutdown() {
    allocator.close();
  }

  private ListVector createListVector(int count) {
    ListVector listVector = ListVector.empty("list vector", allocator);
    Types.MinorType type = Types.MinorType.INT;
    listVector.addOrGetVector(FieldType.nullable(type.getType()));
    listVector.allocateNew();

    IntVector dataVector = (IntVector) listVector.getDataVector();

    for (int i = 0; i < count; i++) {
      dataVector.set(i, i);
    }
    dataVector.setValueCount(count);

    listVector.setNotNull(0);

    listVector.getOffsetBuffer().setInt(0, 0);
    listVector.getOffsetBuffer().setInt(OFFSET_WIDTH, count);

    listVector.setLastSet(0);
    listVector.setValueCount(1);

    return listVector;
  }

  @Test
  public void testCompareLists() {
    try (ListVector listVector1 = createListVector(10);
         ListVector listVector2 = createListVector(11)) {
      VectorValueComparator<ListVector> comparator =
              DefaultVectorComparators.createDefaultComparator(listVector1);
      comparator.attachVectors(listVector1, listVector2);

      // prefix is smaller
      assertTrue(comparator.compare(0, 0) < 0);
    }

    try (ListVector listVector1 = createListVector(11);
         ListVector listVector2 = createListVector(11)) {
      ((IntVector) listVector2.getDataVector()).set(10, 110);

      VectorValueComparator<ListVector> comparator =
              DefaultVectorComparators.createDefaultComparator(listVector1);
      comparator.attachVectors(listVector1, listVector2);

      // breaking tie by the last element
      assertTrue(comparator.compare(0, 0) < 0);
    }

    try (ListVector listVector1 = createListVector(10);
         ListVector listVector2 = createListVector(10)) {

      VectorValueComparator<ListVector> comparator =
              DefaultVectorComparators.createDefaultComparator(listVector1);
      comparator.attachVectors(listVector1, listVector2);

      // list vector elements equal
      assertTrue(comparator.compare(0, 0) == 0);
    }
  }

  @Test
  public void testCopiedComparatorForLists() {
    for (int i = 1; i < 10; i++) {
      for (int j = 1; j < 10; j++) {
        try (ListVector listVector1 = createListVector(10);
             ListVector listVector2 = createListVector(11)) {
          VectorValueComparator<ListVector> comparator =
                  DefaultVectorComparators.createDefaultComparator(listVector1);
          comparator.attachVectors(listVector1, listVector2);

          VectorValueComparator<ListVector> copyComparator = comparator.createNew();
          copyComparator.attachVectors(listVector1, listVector2);

          assertEquals(comparator.compare(0, 0), copyComparator.compare(0, 0));
        }
      }
    }
  }

  @Test
  public void testCompareUInt1() {
    try (UInt1Vector vec = new UInt1Vector("", allocator)) {
      vec.allocateNew(10);
      vec.setValueCount(10);

      vec.setNull(0);
      vec.set(1, -2);
      vec.set(2, -1);
      vec.set(3, 0);
      vec.set(4, 1);
      vec.set(5, 2);
      vec.set(6, -2);
      vec.setNull(7);
      vec.set(8, Byte.MAX_VALUE);
      vec.set(9, Byte.MIN_VALUE);

      VectorValueComparator<UInt1Vector> comparator =
              DefaultVectorComparators.createDefaultComparator(vec);
      comparator.attachVector(vec);

      assertTrue(comparator.compare(0, 1) < 0);
      assertTrue(comparator.compare(1, 2) < 0);
      assertTrue(comparator.compare(1, 3) > 0);
      assertTrue(comparator.compare(2, 5) > 0);
      assertTrue(comparator.compare(4, 5) < 0);
      assertTrue(comparator.compare(1, 6) == 0);
      assertTrue(comparator.compare(0, 7) == 0);
      assertTrue(comparator.compare(8, 9) < 0);
      assertTrue(comparator.compare(4, 8) < 0);
      assertTrue(comparator.compare(5, 9) < 0);
      assertTrue(comparator.compare(2, 9) > 0);
    }
  }

  @Test
  public void testCompareUInt2() {
    try (UInt2Vector vec = new UInt2Vector("", allocator)) {
      vec.allocateNew(10);
      vec.setValueCount(10);

      vec.setNull(0);
      vec.set(1, -2);
      vec.set(2, -1);
      vec.set(3, 0);
      vec.set(4, 1);
      vec.set(5, 2);
      vec.set(6, -2);
      vec.setNull(7);
      vec.set(8, Short.MAX_VALUE);
      vec.set(9, Short.MIN_VALUE);

      VectorValueComparator<UInt2Vector> comparator =
              DefaultVectorComparators.createDefaultComparator(vec);
      comparator.attachVector(vec);

      assertTrue(comparator.compare(0, 1) < 0);
      assertTrue(comparator.compare(1, 2) < 0);
      assertTrue(comparator.compare(1, 3) > 0);
      assertTrue(comparator.compare(2, 5) > 0);
      assertTrue(comparator.compare(4, 5) < 0);
      assertTrue(comparator.compare(1, 6) == 0);
      assertTrue(comparator.compare(0, 7) == 0);
      assertTrue(comparator.compare(8, 9) < 0);
      assertTrue(comparator.compare(4, 8) < 0);
      assertTrue(comparator.compare(5, 9) < 0);
      assertTrue(comparator.compare(2, 9) > 0);
    }
  }

  @Test
  public void testCompareUInt4() {
    try (UInt4Vector vec = new UInt4Vector("", allocator)) {
      vec.allocateNew(10);
      vec.setValueCount(10);

      vec.setNull(0);
      vec.set(1, -2);
      vec.set(2, -1);
      vec.set(3, 0);
      vec.set(4, 1);
      vec.set(5, 2);
      vec.set(6, -2);
      vec.setNull(7);
      vec.set(8, Integer.MAX_VALUE);
      vec.set(9, Integer.MIN_VALUE);

      VectorValueComparator<UInt4Vector> comparator =
              DefaultVectorComparators.createDefaultComparator(vec);
      comparator.attachVector(vec);

      assertTrue(comparator.compare(0, 1) < 0);
      assertTrue(comparator.compare(1, 2) < 0);
      assertTrue(comparator.compare(1, 3) > 0);
      assertTrue(comparator.compare(2, 5) > 0);
      assertTrue(comparator.compare(4, 5) < 0);
      assertTrue(comparator.compare(1, 6) == 0);
      assertTrue(comparator.compare(0, 7) == 0);
      assertTrue(comparator.compare(8, 9) < 0);
      assertTrue(comparator.compare(4, 8) < 0);
      assertTrue(comparator.compare(5, 9) < 0);
      assertTrue(comparator.compare(2, 9) > 0);
    }
  }

  @Test
  public void testCompareUInt8() {
    try (UInt8Vector vec = new UInt8Vector("", allocator)) {
      vec.allocateNew(10);
      vec.setValueCount(10);

      vec.setNull(0);
      vec.set(1, -2);
      vec.set(2, -1);
      vec.set(3, 0);
      vec.set(4, 1);
      vec.set(5, 2);
      vec.set(6, -2);
      vec.setNull(7);
      vec.set(8, Long.MAX_VALUE);
      vec.set(9, Long.MIN_VALUE);

      VectorValueComparator<UInt8Vector> comparator =
              DefaultVectorComparators.createDefaultComparator(vec);
      comparator.attachVector(vec);

      assertTrue(comparator.compare(0, 1) < 0);
      assertTrue(comparator.compare(1, 2) < 0);
      assertTrue(comparator.compare(1, 3) > 0);
      assertTrue(comparator.compare(2, 5) > 0);
      assertTrue(comparator.compare(4, 5) < 0);
      assertTrue(comparator.compare(1, 6) == 0);
      assertTrue(comparator.compare(0, 7) == 0);
      assertTrue(comparator.compare(8, 9) < 0);
      assertTrue(comparator.compare(4, 8) < 0);
      assertTrue(comparator.compare(5, 9) < 0);
      assertTrue(comparator.compare(2, 9) > 0);
    }
  }
}
